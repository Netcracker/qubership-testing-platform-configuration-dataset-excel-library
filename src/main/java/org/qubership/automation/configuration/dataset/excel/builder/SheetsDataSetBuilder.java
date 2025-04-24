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

import org.qubership.automation.configuration.dataset.excel.impl.DSCell;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SheetsDataSetBuilder {
    private static final Supplier<Predicate<DSCell>> ALL_COLUMNS = () -> Utils.statelessHeaderPredicate(always -> true);
    private static final Supplier<Predicate<DSCell>> NO_COLUMNS = () -> Utils.statelessHeaderPredicate(always -> false);
    final DataSetBuilder parent;
    Supplier<Predicate<DSCell>> columns;

    SheetsDataSetBuilder(@Nonnull DataSetBuilder parent) {
        this.parent = parent;
    }

    /**
     * for stateful predicates
     */
    public ParamsBuilder forDataSets(@Nonnull Supplier<Predicate<DSCell>> stateful) {
        return next(stateful);
    }

    /**
     * for stateless predicates
     */
    public ParamsBuilder forDataSets(@Nonnull Predicate<DSCell> stateless) {
        return forDataSets(() -> stateless);
    }

    public ParamsBuilder forDataSets(@Nonnull final Collection<String> names) {
        return forDataSets(Utils.statelessHeaderPredicate(names));
    }

    public ParamsBuilder forDataSets(@Nonnull String... names) {
        return forDataSets(Arrays.asList(names));
    }

    public ParamsBuilder forAllDataSets() {
        return forDataSets(ALL_COLUMNS);
    }

    /**
     * can be used to read params only
     *
     * @return empty DataSetLists with params
     */
    public ParamsBuilder noDataSets() {
        return forDataSets(NO_COLUMNS);
    }

    @Nonnull
    private ParamsBuilder next(@Nonnull Supplier<Predicate<DSCell>> columns) {
        this.columns = columns;
        return new ParamsBuilder(this);
    }
}
