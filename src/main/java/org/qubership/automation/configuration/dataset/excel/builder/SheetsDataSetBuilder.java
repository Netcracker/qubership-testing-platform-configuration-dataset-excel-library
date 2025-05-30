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

package org.qubership.automation.configuration.dataset.excel.builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.qubership.automation.configuration.dataset.excel.impl.DSCell;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;

public class SheetsDataSetBuilder {

    /**
     * Supplier for all columns.
     */
    private static final Supplier<Predicate<DSCell>> ALL_COLUMNS
            = () -> Utils.statelessHeaderPredicate(always -> true);

    /**
     * Supplier for no columns.
     */
    private static final Supplier<Predicate<DSCell>> NO_COLUMNS
            = () -> Utils.statelessHeaderPredicate(always -> false);

    /**
     * Parent object.
     */
    final DataSetBuilder parent;

    /**
     * DSCell Predicate Supplier object.
     */
    Supplier<Predicate<DSCell>> columns;

    /**
     * Constructor.
     *
     * @param parent DataSetBuilder object.
     */
    SheetsDataSetBuilder(@Nonnull final DataSetBuilder parent) {
        this.parent = parent;
    }

    /**
     * Make ParamsBuilder for stateful Predicate Supplier.
     *
     * @param stateful Predicate Supplier of DSCell object
     * @return ParamsBuilder for stateful Predicate Supplier.
     */
    public ParamsBuilder forDataSets(@Nonnull final Supplier<Predicate<DSCell>> stateful) {
        return next(stateful);
    }

    /**
     * Make ParamsBuilder for stateless Predicate.
     *
     * @param stateless Predicate of DSCell object
     * @return ParamsBuilder for stateless Predicate.
     */
    public ParamsBuilder forDataSets(@Nonnull final Predicate<DSCell> stateless) {
        return forDataSets(() -> stateless);
    }

    /**
     * Make ParamsBuilder for all specified column names.
     *
     * @param names Collection of String names
     * @return ParamsBuilder for all specified columns (~datasets).
     */
    public ParamsBuilder forDataSets(@Nonnull final Collection<String> names) {
        return forDataSets(Utils.statelessHeaderPredicate(names));
    }

    /**
     * Make ParamsBuilder for all specified column names.
     *
     * @param names vararg of String names
     * @return ParamsBuilder for all specified columns (~datasets).
     */
    public ParamsBuilder forDataSets(@Nonnull final String... names) {
        return forDataSets(Arrays.asList(names));
    }

    /**
     * Make ParamsBuilder for all columns.
     *
     * @return ParamsBuilder for all columns (~datasets).
     */
    public ParamsBuilder forAllDataSets() {
        return forDataSets(ALL_COLUMNS);
    }

    /**
     * Make ParamsBuilder in case there is no dataset; can be used only to read params.
     *
     * @return ParamsBuilder for empty DataSetLists with params.
     */
    public ParamsBuilder noDataSets() {
        return forDataSets(NO_COLUMNS);
    }

    @Nonnull
    private ParamsBuilder next(@Nonnull final Supplier<Predicate<DSCell>> columns) {
        this.columns = columns;
        return new ParamsBuilder(this);
    }
}
