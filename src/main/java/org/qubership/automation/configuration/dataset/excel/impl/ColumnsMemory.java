/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.configuration.dataset.excel.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.qubership.automation.configuration.dataset.excel.core.ColumnHandler;
import org.qubership.automation.configuration.dataset.excel.core.Consumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <pre>
 * Incapsulates two main strategies: {@link MemorizeStrategy}, {@link RegularStrategy}
 * Takes first iterator, filters it by provided predicates({@link #addPredicates(Iterator)}),
 * Requests for the callBack function for cells in each column using CB provider ({@link #setCBProvider(ColumnHandler)})
 * This request consist of {@link Pair} mapping between provided predicate and found cell matches it.
 *  !limitation: 1 cell - 1 callBack - 1 request to provider with firstly matched predicate
 *
 * All the next iterators are processed with next strategy, which:
 * filters columns by numbers, invokes callback  bound to it.
 * !notes:
 * cb provider can return null callback for selected predicate;
 * in this case predicate will be skipped and all other predicates will be checked;
 * if provider returns non null cb, all the next predicates will be skipped.
 *
 * summary: 1 column matched by the predicate = 1 nonnull callback from the provider;
 *  in other cases columns are skipped
 *
 * should be initialized with predicates and cb provider before using the provided iterator wrapper
 * </pre>
 */
public class ColumnsMemory implements Function<Iterator<Cell>, Iterator<Cell>> {

    private static final Function<Iterator<Cell>, Iterator<Cell>> DEFAULT_EMPTY_STRATEGY
            = new Function<Iterator<Cell>, Iterator<Cell>>() {
        @Nullable
        @Override
        public Iterator<Cell> apply(@Nullable Iterator<Cell> input) {
            return Collections.emptyIterator();
        }
    };

    protected final Deque<Predicate<Cell>> predicates;
    protected ColumnHandler cbProvider;
    protected Function<Iterator<Cell>, Iterator<Cell>> currentStrategy = new MemorizeStrategy(this);
    protected boolean isStarted = false;

    public ColumnsMemory() {
        predicates = Lists.newLinkedList();
    }

    public void addPredicates(@Nonnull Iterable<Predicate<Cell>> predicates) {
        addPredicates(predicates.iterator());
    }

    public void addPredicates(@Nonnull Iterator<Predicate<Cell>> predicates) {
        Preconditions.checkState(!isStarted, "It is already started, this operation does nothing");
        Iterators.addAll(this.predicates, predicates);
    }

    @Nonnull
    public Deque<Predicate<Cell>> getPredicates() {
        return predicates;
    }

    public void setCBProvider(@Nonnull ColumnHandler cbProvider) {
        Preconditions.checkState(!isStarted, "It is already started, this operation does nothing");
        this.cbProvider = cbProvider;
    }

    @Nullable
    @Override
    public Iterator<Cell> apply(@Nullable Iterator<Cell> input) {
        return currentStrategy.apply(input);
    }

    private static class CallBackBoundary implements Comparable<CallBackBoundary> {
        protected final Predicate<Cell> selector;
        protected final int cellIndex;
        protected final Consumer<Cell> cbHandler;

        protected CallBackBoundary(@Nonnull Predicate<Cell> selector, int cellIndex, @Nonnull Consumer<Cell> cbHandler) {
            this.selector = selector;
            this.cellIndex = cellIndex;
            this.cbHandler = cbHandler;
        }

        @Override
        public int compareTo(@Nonnull CallBackBoundary o) {
            return ComparisonChain.start()
                    .compare(this.cellIndex, o.cellIndex)
                    .result();
        }
    }

    /**
     * <pre>
     * Wraps passed iterator into a new one
     * The new one iterate thru accepted cells only (accepted by predicates from {@link ColumnsMemory})
     * On the each next iteration requests for the new callback function for the previously found item
     * (requests from provider stored in {@link ColumnsMemory}), bounds it to predicates
     * At the end of input iterator, initializes the next strategy: {@link RegularStrategy}
     * </pre>
     */
    private static class MemorizeStrategy implements Function<Iterator<Cell>, Iterator<Cell>> {
        private final ColumnsMemory memory;
        private List<CallBackBoundary> cbs;


        private MemorizeStrategy(@Nonnull ColumnsMemory memory) {
            this.memory = memory;
        }

        @Nonnull
        @Override
        public Iterator<Cell> apply(final Iterator<Cell> input) {
            return new AbstractIterator<Cell>() {

                @Override
                protected Cell computeNext() {
                    while (input.hasNext()) {
                        final Cell cell = input.next();
                        if (tryRegisterCallback(cell))
                            return cell;
                    }
                    memory.currentStrategy = getNextStrat();
                    return endOfData();
                }
            };
        }

        @Nonnull
        private ColumnHandler getCBProvider() {
            if (!memory.isStarted) {
                Preconditions.checkNotNull(memory.cbProvider, "You should set the callbacks provider to lazy init the [%s]", memory);
                memory.isStarted = true;
            }
            return memory.cbProvider;
        }

        private boolean tryRegisterCallback(@Nonnull Cell cell, @Nonnull Predicate<Cell> matchedPredicate) {
            Consumer<Cell> callBack = getCBProvider().getHandler(cell, matchedPredicate);
            if (callBack == null)
                return false;
            CallBackBoundary boundary = new CallBackBoundary(matchedPredicate, cell.getColumnIndex(), callBack);
            if (cbs == null)
                cbs = Lists.newArrayList();
            cbs.add(boundary);
            return true;
        }

        private boolean tryRegisterCallback(@Nonnull Cell cell) {
            for (Predicate<Cell> pred : memory.predicates) {
                if (pred.test(cell) && tryRegisterCallback(cell, pred))
                    return true;
            }
            return false;
        }

        @Nonnull
        private Function<Iterator<Cell>, Iterator<Cell>> getNextStrat() {
            if (cbs == null)
                return DEFAULT_EMPTY_STRATEGY;
            Collections.sort(cbs);
            return new RegularStrategy(cbs);
        }
    }

    /**
     * <pre>
     * Wraps input iterator into a new one, which:
     * Takes cell numbers and bound callbacks to them, obtained by predicates in the previous strategy {@link MemorizeStrategy}
     * Invokes provided callbacks on the cells of memorized indexes(column numbers)
     * </pre>
     */
    private static class RegularStrategy implements Function<Iterator<Cell>, Iterator<Cell>> {
        private final List<CallBackBoundary> initedSortedCbs;

        private RegularStrategy(@Nonnull List<CallBackBoundary> initedSortedCbs) {
            this.initedSortedCbs = initedSortedCbs;
        }

        @Nonnull
        @Override
        public Iterator<Cell> apply(final Iterator<Cell> input) {
            return new AbstractIterator<Cell>() {
                private final PeekingIterator<CallBackBoundary> boundPeek = Iterators.peekingIterator(initedSortedCbs.iterator());
                private final PeekingIterator<Cell> inputPeek = Iterators.peekingIterator(input);

                @Override
                protected Cell computeNext() {
                    while (boundPeek.hasNext() && inputPeek.hasNext()) {
                        int inputCellIndex = inputPeek.peek().getColumnIndex();
                        int boundaryCellIndex = boundPeek.peek().cellIndex;
                        if (inputCellIndex > boundaryCellIndex) {
                            //will move boundary
                            boundPeek.next();
                        } else if (inputCellIndex < boundaryCellIndex) {
                            //will move inputPeek
                            inputPeek.next();
                        } else {
                            //equals
                            CallBackBoundary boundary = boundPeek.next();
                            Cell cell = inputPeek.next();
                            boundary.cbHandler.accept(cell);
                            return cell;
                        }
                    }
                    return endOfData();
                }
            };
        }
    }
}
