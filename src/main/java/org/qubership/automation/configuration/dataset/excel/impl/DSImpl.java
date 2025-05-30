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

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryModificator;
import org.qubership.automation.configuration.dataset.excel.impl.morphcells.Changelist;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;

public class DSImpl<Param, Var, Vars> extends Utils.CachingIterator<VarsConvInfo<Param>> implements DS<Param, Vars> {

    /**
     * Dataset qualifier.
     */
    private final String qualifier;

    /**
     * EvaluationContext object.
     */
    private final EvaluationContext eval;

    /**
     * Supplier of Variables.
     */
    private final VarsSupplier<Param, Var, Vars> source;

    /**
     * Dataset name.
     */
    private String name;

    /**
     * Parent DatasetList.
     */
    private DSListImpl myList;

    /**
     * Runnable to check state.
     */
    private Runnable stateCheck = DSListImpl.ILLEGAL_STATE;

    /**
     * Constructor.
     *
     * @param sourceQualifier String qualifier
     * @param eval EvaluationContext object
     * @param name String dataset name
     * @param varConv Supplier of VarsEntryConverters
     * @param varsConv Supplier of Functions
     * @param cellsIter Cells Iterator Supplier
     */
    protected DSImpl(@Nonnull final String sourceQualifier,
                     @Nonnull final EvaluationContext eval,
                     @Nonnull final String name,
                     @Nonnull final Supplier<VarsEntryConverter<Param, Var>> varConv,
                     @Nonnull final Supplier<Function<Iterator<Var>, Vars>> varsConv,
                     @Nonnull final Supplier<? extends Iterator<?>> cellsIter) {
        super(cellsIter);
        setName(name);
        this.qualifier = sourceQualifier + "/" + name;
        this.eval = eval;
        this.source = doVarsSupplier(eval.getStrategy().onDsCleanup,
                eval.getStrategy().reuseConverters, varConv, varsConv, eval);
    }

    /**
     * Make String representation of the object.
     *
     * @return String representation of the object.
     */
    @Override
    public String toString() {
        return qualifier;
    }

    /**
     * Lazy initializer under parent DatasetList.
     *
     * @param myList - parent DatasetList object.
     */
    public void lazyInit(@Nonnull final DSListImpl myList) {
        this.myList = myList;
        stateCheck = Runnables.doNothing();
    }

    /**
     * Get name.
     *
     * @return String name.
     */
    @Nonnull
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Set name.
     *
     * @param name String name to set.
     */
    @Override
    public void setName(@Nonnull final String name) {
        this.name = name;
    }

    /**
     * Get variables.
     *
     * @return Vars object.
     */
    public Vars getVariables() {
        stateCheck.run();
        return source.get();
    }

    /**
     * Get variables.
     *
     * @param modificator did modifications of value cells of each variable entry;
     *                    these modifications are temporarily, just for returned vars
     * @return Vars object.
     */
    @Override
    public Vars getVariables(@Nonnull final VarsEntryModificator<Param> modificator) {
        stateCheck.run();
        Preconditions.checkArgument(eval.getStrategy() == ReevaluateFormulas.IN_CONVERTER,
                "Works only for ReevaluateFormulas.IN_CONVERTER");
        return source.get(modificator);
    }

    /**
     * Get eval field.
     *
     * @return EvaluationContext object.
     */
    @Nonnull
    public EvaluationContext getEval() {
        return this.eval;
    }

    /**
     * Get DSList.
     *
     * @return DSListImpl myList.
     */
    @Nonnull
    protected DSListImpl getDSList() {
        return myList;
    }

    private VarsSupplier<Param, Var, Vars> doVarsSupplier(
            final boolean onDsCleanup,
            final boolean reuseConverters,
            @Nonnull final Supplier<VarsEntryConverter<Param, Var>> varConv,
            @Nonnull final Supplier<Function<Iterator<Var>, Vars>> varsConv,
            @Nonnull final EvaluationContext evaluator) {
        Iterable<VarsConvInfo<Param>> datasource = new MemoizingIterable<>(this);
        Runnable preGet = EvaluationContext.cleanupRunnable(onDsCleanup, evaluator);
        if (reuseConverters) {
            return new VarsSupplier<>(varConv, varsConv, datasource, eval, preGet);
        } else {
            return new MemoizingVarsSupplier<>(varConv, varsConv, datasource, eval, preGet);
        }
    }

    /**
     * calculates Vars each time accessed
     */
    private static class VarsSupplier<Param, Var, Vars> implements Supplier<Vars> {

        /**
         * EvaluationContext object.
         */
        protected final EvaluationContext evaluator;

        /**
         * Supplier of VarsEntryConverters.
         */
        private final Supplier<VarsEntryConverter<Param, Var>> varConv;

        /**
         * Supplier of Functions.
         */
        private final Supplier<Function<Iterator<Var>, Vars>> varsConv;

        /**
         * Runnable pre-get task.
         */
        private final Runnable preGet;

        /**
         * Iterable of VarsConvInfo of Param object.
         */
        private final Iterable<VarsConvInfo<Param>> datasource;

        /**
         * Constructor.
         *
         * @param varConv Supplier of VarsEntryConverters object
         * @param varsConv Supplier of Functions object
         * @param datasource Iterable of VarsConvInfo of Param object
         * @param evaluator EvaluationContext object
         * @param preGet Runnable handler
         */
        public VarsSupplier(@Nonnull final Supplier<VarsEntryConverter<Param, Var>> varConv,
                            @Nonnull final Supplier<Function<Iterator<Var>, Vars>> varsConv,
                            @Nonnull final Iterable<VarsConvInfo<Param>> datasource,
                            @Nonnull final EvaluationContext evaluator,
                            @Nonnull final Runnable preGet) {
            this.varConv = varConv;
            this.varsConv = varsConv;
            this.preGet = preGet;
            this.datasource = datasource;
            this.evaluator = evaluator;
        }

        /**
         * Get variables.
         *
         * @return Vars object.
         */
        @Override
        public Vars get() {
            preGet.run();
            final VarsEntryConverter<Param, Var> varConv = this.varConv.get();
            final Iterator<VarsConvInfo<Param>> datasource = this.datasource.iterator();

            // new vars iterator should wrap datasource iterator
            // and pass each element through varConv with nulls omitting.
            Iterator<Var> vars = new AbstractIterator<Var>() {
                @Override
                protected Var computeNext() {
                    Var computed = null;
                    while (computed == null && datasource.hasNext()) {
                        VarsConvInfo<Param> toConvert = datasource.next();
                        computed = varConv.doVarsEntry(toConvert.entity,
                                toConvert.param,
                                toConvert.convertedParam,
                                new DSCell(toConvert.var, evaluator));
                    }
                    return computed == null ? endOfData() : computed;
                }
            };
            Function<Iterator<Var>, Vars> varsConv = this.varsConv.get();
            synchronized (evaluator) {
                return varsConv.apply(vars);
            }
        }

        /**
         * Get variables.
         *
         * @param modificator VarsEntryModificator of Param object
         * @return Vars object.
         */
        public Vars get(@Nonnull final VarsEntryModificator<Param> modificator) {
            Changelist changes = new Changelist(null, evaluator.getDescriptors());
            for (VarsConvInfo<Param> info : datasource) {
                Cell toModify = info.var;
                changes.setCurrentCell(toModify);
                modificator.modify(info.entity, info.param, info.convertedParam, changes);
            }
            synchronized (evaluator) {
                changes.applyChanges();
                Vars result;
                try {
                    result = get();
                } finally {
                    changes.revertChanges();
                }
                return result;
            }
        }
    }

    /**
     * runs preGet each time accessed
     */
    private static class MemoizingVarsSupplier<Param, Var, Vars> extends VarsSupplier<Param, Var, Vars> {

        /**
         * Runnable pre-get task.
         */
        protected final Runnable preGet;

        /**
         * Vars cached object.
         */
        private Vars cached;

        /**
         * Flag if the Supplier is already initialized.
         */
        private volatile boolean initialized;

        /**
         * Constructor.
         *
         * @param varConv Supplier of VarsEntryConverter object
         * @param varsConv Supplier of Function object
         * @param datasource Iterable of VarsConvInfo of Param object
         * @param evaluator EvaluationContext object
         * @param preGet Runnable
         */
        public MemoizingVarsSupplier(@Nonnull final Supplier<VarsEntryConverter<Param, Var>> varConv,
                                     @Nonnull final Supplier<Function<Iterator<Var>, Vars>> varsConv,
                                     @Nonnull final Iterable<VarsConvInfo<Param>> datasource,
                                     @Nonnull final EvaluationContext evaluator,
                                     @Nonnull final Runnable preGet) {
            super(varConv, varsConv, datasource, evaluator, Runnables.doNothing());
            this.preGet = preGet;
        }

        /**
         * Get variables.
         *
         * @return Vars object.
         */
        @Override
        public Vars get() {
            preGet.run();
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        cached = super.get();
                        initialized = true;
                        return cached;
                    }
                }
            }
            return cached;
        }
    }

    private static class MemoizingIterable<T> implements Iterable<T> {

        /**
         * Iterator of memorized objects.
         */
        final Iterator<T> toMemorize;

        /**
         * Flag if the object is already initialized.
         */
        volatile boolean initialized;

        /**
         * Iterable value.
         */
        Iterable<T> value;

        /**
         * Constructor.
         *
         * @param toMemorize Iterator of memorized objects.
         */
        public MemoizingIterable(final Iterator<T> toMemorize) {
            this.toMemorize = toMemorize;
        }

        /**
         * Get iterator.
         *
         * @return Iterator of value elements.
         */
        @Override
        public Iterator<T> iterator() {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        value = Lists.newArrayList(toMemorize);
                        initialized = true;
                        return value.iterator();
                    }
                }
            }
            return value.iterator();
        }
    }
}
