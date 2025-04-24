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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;

public class Utils {

    public static final ParamsEntryConverter<String> STRING_PAR_ENTRY_C = new ParamsEntryConverter<String>() {
        @Nullable
        @Override
        public String doParamsEntry(@Nullable DSCell entity, @Nonnull DSCell param) {
            return doRegularParamName(entity == null ? null : entity.getStringValue(), param.getStringValue()).orElse(null);
        }
    };
    private static final VarsEntryConverter<?, ? extends VarEntity<?>> DEFAULT_VAR_ENTRY_C = new VarsEntryConverter<Object, VarEntity<?>>() {
        @Nonnull
        @Override
        public VarEntity<Object> doVarsEntry(@Nullable DSCell entity, @Nonnull DSCell param, @Nonnull Object convertedParam, @Nonnull DSCell value) {
            return new VarEntity<>(entity, param, convertedParam, value);
        }
    };

    /**
     * designed to be stateless
     */
    @Nonnull
    public static Predicate<DSCell> statelessHeaderPredicate(@Nonnull final Iterable<String> accepted) {
        return input -> Iterables.contains(accepted, input.getStringValue());
    }

    /**
     * for cases when string value of cell doesn't matter
     *
     * @param stateless should be stateless
     */
    @Nonnull
    public static Predicate<DSCell> statelessHeaderPredicate(@Nonnull final Predicate<Cell> stateless) {
        return input -> stateless.test(input.getCell());
    }

    /**
     * should be used when evaluator is acquired
     * can not be reused with different workbook
     */
    @Nonnull
    public static Predicate<Cell> statefulHeaderPredicate(@Nonnull final Predicate<DSCell> wrapped, @Nonnull final EvaluationContext evaluator) {
        return cell -> wrapped.test(new DSCell(cell, evaluator));
    }

    /**
     * should be used when evaluator is acquired
     * can not be reused with different workbook
     */
    @Nonnull
    public static Predicate<Cell> statefulHeaderPredicate(@Nonnull final Iterable<String> accepted, @Nonnull final EvaluationContext evaluator) {
        return input -> Iterables.contains(accepted, evaluator.getCellValue(input).toString());
    }

    /**
     * should be used when evaluator is acquired
     * can not be reused with different workbook
     */
    @Nonnull
    public static Predicate<Cell> statefulHeaderPredicate(@Nonnull EvaluationContext evaluator, @Nonnull String... accepted) {
        return statefulHeaderPredicate(Arrays.asList(accepted), evaluator);
    }

    public static Optional<String> doRegularParamName(@Nullable String entityName, @Nullable String paramName) {
        if (Strings.isNullOrEmpty(paramName))
            return Optional.empty();
        if (!Strings.isNullOrEmpty(entityName)) {
            paramName = entityName + "." + paramName;
        }
        return Optional.of(paramName);
    }

    public static <Param> Function<Iterator<Param>, List<Param>> listParamsFunc() {
        return new Function<Iterator<Param>, List<Param>>() {
            @Nullable
            @Override
            public List<Param> apply(@Nonnull Iterator<Param> input) {
                return Lists.newArrayList(input);
            }
        };
    }

    public static <Param, Var> Function<Iterator<Pair<Param, Var>>, Map<Param, Var>> mapVarsFunc() {
        return new Function<Iterator<Pair<Param, Var>>, Map<Param, Var>>() {
            @Nullable
            @Override
            public Map<Param, Var> apply(@Nonnull Iterator<Pair<Param, Var>> input) {
                Map<Param, Var> result = Maps.newHashMap();
                while (input.hasNext()) {
                    Pair<Param, Var> item = input.next();
                    result.put(item.getKey(), item.getValue());
                }
                return result;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <Param> VarsEntryConverter<Param, VarEntity<Param>> defaultVarEntryConv() {
        return (VarsEntryConverter<Param, VarEntity<Param>>) DEFAULT_VAR_ENTRY_C;
    }

    /**
     * see {@link Suppliers#memoize(com.google.common.base.Supplier)}
     */
    @Nonnull
    public static <T> Supplier<T> memoize(@Nonnull Supplier<T> delegate) {
        return new MemoizingSupplier<>(Preconditions.checkNotNull(delegate));
    }

    /**
     * assumes, that connected parent iterator routes some items to the {@link Function#apply(Object)} method after
     * invoking its {@link Iterator#next()}. Acts like a collector with an ability to ask its parent for a value
     *
     * @param <T>
     */
    public static class CachingIterator<T> extends AbstractIterator<T> implements Consumer<T> {
        protected final Supplier<? extends Iterator<?>> connectedParent;
        private final Queue<T> cache = new LinkedList<>();

        public CachingIterator(@Nonnull Supplier<? extends Iterator<?>> connectedParent) {
            this.connectedParent = connectedParent;
        }

        @Override
        protected T computeNext() {
            synchronized (connectedParent) {
                Iterator<?> parent = connectedParent.get();
                while (cache.isEmpty() && parent.hasNext()) {
                    parent.next();
                }
            }
            if (cache.isEmpty()) {
                return endOfData();
            }
            return cache.remove();
        }

        @Override
        public void accept(T input) {
            if (input != null)
                cache.offer(input);
        }
    }

    /**
     * stolen from {@link Suppliers#memoize(com.google.common.base.Supplier)}
     */
    static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
        final Supplier<T> delegate;
        transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        transient T value;

        MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }
    }

    /**
     * see https://stackoverflow
     * .com/questions/19808342/how-to-initialize-a-circular-dependency-final-fields-referencing-each-other?lq=1
     */
    public static class MutableSupplier<T> implements Supplier<T> {

        private boolean valueWasSet;

        private T value;

        public MutableSupplier() {
        }

        public static <T> MutableSupplier<T> create() {
            return new MutableSupplier<>();
        }

        @Override
        public T get() {
            if (!valueWasSet) {
                throw new NullPointerException("Value has not been set yet");
            }
            return value;
        }

        public T set(final T value) {
            if (valueWasSet) {
                throw new IllegalStateException("Value has already been set and should not be reset");
            }
            this.value = value;
            this.valueWasSet = true;
            return value;
        }
    }
}
