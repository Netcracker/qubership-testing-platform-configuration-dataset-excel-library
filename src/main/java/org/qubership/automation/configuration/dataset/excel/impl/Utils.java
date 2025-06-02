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
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Utils {

    /**
     * Constant converter to make regular name of a parameter.
     */
    public static final ParamsEntryConverter<String> STRING_PARAMS_ENTRY_CONVERTER = (entity, param)
            -> doRegularParamName(entity == null ? null : entity.getStringValue(), param.getStringValue())
            .orElse(null);

    /**
     * Default converter of values.
     */
    private static final VarsEntryConverter<?, ? extends VarEntity<?>> DEFAULT_VAR_ENTRY_CONVERTER
            = new VarsEntryConverter<Object, VarEntity<?>>() {
        @Nonnull
        @Override
        public VarEntity<Object> doVarsEntry(@Nullable final DSCell entity,
                                             @Nonnull final DSCell param,
                                             @Nonnull final Object convertedParam,
                                             @Nonnull final DSCell value) {
            return new VarEntity<>(entity, param, convertedParam, value);
        }
    };

    /**
     * Header Predicate; designed to be stateless.
     *
     * @param accepted Iterable of Strings to check
     * @return Predicate of DSCell.
     */
    @Nonnull
    public static Predicate<DSCell> statelessHeaderPredicate(@Nonnull final Iterable<String> accepted) {
        return input -> Iterables.contains(accepted, input.getStringValue());
    }

    /**
     * Header Predicate; for cases when string value of cell doesn't matter.
     *
     * @param stateless should be stateless
     * @return Predicate of DSCell.
     */
    @Nonnull
    public static Predicate<DSCell> statelessHeaderPredicate(@Nonnull final Predicate<Cell> stateless) {
        return input -> stateless.test(input.getCell());
    }

    /**
     * Header Predicate; should be used when evaluator is acquired. Can't be reused with different workbooks.
     *
     * @param wrapped Predicate of DSCell
     * @param evaluator EvaluationContext object
     * @return Predicate of Cell.
     */
    @Nonnull
    public static Predicate<Cell> statefulHeaderPredicate(@Nonnull final Predicate<DSCell> wrapped,
                                                          @Nonnull final EvaluationContext evaluator) {
        return cell -> wrapped.test(new DSCell(cell, evaluator));
    }

    /**
     * Header Predicate; should be used when evaluator is acquired. Can't be reused with different workbooks.
     *
     * @param accepted Iterable of Strings
     * @param evaluator EvaluationContext object
     * @return Predicate of Cell.
     */
    @Nonnull
    public static Predicate<Cell> statefulHeaderPredicate(@Nonnull final Iterable<String> accepted,
                                                          @Nonnull final EvaluationContext evaluator) {
        return input -> Iterables.contains(accepted, evaluator.getCellValue(input).toString());
    }

    /**
     * Header Predicate; should be used when evaluator is acquired. Can't be reused with different workbooks.
     *
     * @param evaluator EvaluationContext object
     * @param accepted vararg of Strings to check
     * @return Predicate of Cell.
     */
    @Nonnull
    public static Predicate<Cell> statefulHeaderPredicate(@Nonnull final EvaluationContext evaluator,
                                                          @Nonnull final String... accepted) {
        return statefulHeaderPredicate(Arrays.asList(accepted), evaluator);
    }

    /**
     * Make regular parameter name (dotted notation).
     *
     * @param entityName String name of entity
     * @param paramName String name of parameter
     * @return Optional String depending on entity/parameter names.
     */
    public static Optional<String> doRegularParamName(@Nullable final String entityName,
                                                      @Nullable final String paramName) {
        if (Strings.isNullOrEmpty(paramName)) {
            return Optional.empty();
        }
        return Optional.of(
                !Strings.isNullOrEmpty(entityName)
                        ? entityName + "." + paramName
                        : paramName
        );
    }

    /**
     * Function to make list of Params.
     *
     * @return &lt;Param&gt; object.
     */
    public static <Param> Function<Iterator<Param>, List<Param>> listParamsFunc() {
        return new Function<Iterator<Param>, List<Param>>() {
            @Nullable
            @Override
            public List<Param> apply(@Nonnull Iterator<Param> input) {
                return Lists.newArrayList(input);
            }
        };
    }

    /**
     * Function to make map of Param - Variables.
     *
     * @return Map of Param - Variables filled.
     */
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

    /**
     * Get default Var Entry Converter.
     */
    @SuppressWarnings("unchecked")
    public static <Param> VarsEntryConverter<Param, VarEntity<Param>> defaultVarEntryConv() {
        return (VarsEntryConverter<Param, VarEntity<Param>>) DEFAULT_VAR_ENTRY_CONVERTER;
    }

    /**
     * Create new MemoizingSupplier for the delegate (see {@link Suppliers#memoize(com.google.common.base.Supplier)}).
     *
     * @param delegate Supplier of T-class
     * @return new MemoizingSupplier object.
     */
    @Nonnull
    public static <T> Supplier<T> memoize(@Nonnull final Supplier<T> delegate) {
        return new MemoizingSupplier<>(Preconditions.checkNotNull(delegate));
    }

    /**
     * Assumes, that connected parent iterator routes some items to the {@link Function#apply(Object)} method after
     * invoking its {@link Iterator#next()}. Acts like a collector with an ability to ask its parent for a value.
     */
    public static class CachingIterator<T> extends AbstractIterator<T> implements Consumer<T> {

        /**
         * Supplier of parent objects.
         */
        protected final Supplier<? extends Iterator<?>> connectedParent;

        /**
         * Cache; Linked list if used.
         */
        private final Queue<T> cache = new LinkedList<>();

        /**
         * Constructor.
         *
         * @param connectedParent Supplier of parent objects.
         */
        public CachingIterator(@Nonnull final Supplier<? extends Iterator<?>> connectedParent) {
            this.connectedParent = connectedParent;
        }

        /**
         * Get the next value from the cache.
         *
         * @return &lt;T&gt; class object.
         */
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

        /**
         * Insert input object into cache (via cache offer mechanism).
         *
         * @param input the input argument.
         */
        @Override
        public void accept(final T input) {
            if (input != null) {
                cache.offer(input);
            }
        }
    }

    /**
     * stolen from {@link Suppliers#memoize(com.google.common.base.Supplier)}
     */
    static class MemoizingSupplier<T> implements Supplier<T>, Serializable {

        /**
         * Supplier of T class.
         */
        final Supplier<T> delegate;

        /**
         * Flag if the object is initialized or not.
         */
        transient volatile boolean initialized;

        /**
         * T class value.
         */
        transient T value;

        /**
         * Constructor.
         *
         * @param delegate Supplier of T class object.
         */
        MemoizingSupplier(final Supplier<T> delegate) {
            this.delegate = delegate;
        }

        /**
         * Get value; initialize supplier if necessary.
         *
         * @return T class object.
         */
        @Override
        public T get() {
            // A 2-field variant of Double-checked Locking.
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
     * see https://stackoverflow.com/questions/19808342/how-to-initialize-a-circular-dependency-final-fields-referencing-each-other?lq=1
     */
    public static class MutableSupplier<T> implements Supplier<T> {

        /**
         * Flag if value was already set or not.
         */
        private boolean valueWasSet;

        /**
         * T class value.
         */
        private T value;

        /**
         * Constructor.
         */
        public MutableSupplier() {
        }

        /**
         * Create new MutableSupplier object.
         */
        public static <T> MutableSupplier<T> create() {
            return new MutableSupplier<>();
        }

        /**
         * Get value.
         *
         * @return &lt;T&gt; class object.
         */
        @Override
        public T get() {
            if (!valueWasSet) {
                throw new NullPointerException("Value has not been set yet");
            }
            return value;
        }

        /**
         * Set value.
         *
         * @param value &lt;T&gt; class object
         * @return &lt;T&gt; class object.
         */
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
