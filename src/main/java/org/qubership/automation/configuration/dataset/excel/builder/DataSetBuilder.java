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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DataSetBuilder {

    final Supplier<Workbook> wb;
    Supplier<Predicate<Sheet>> selectedSheets;

    DataSetBuilder(@Nonnull final Supplier<Workbook> wb) {
        this.wb = wb;
    }

    public static DataSetBuilder create(@Nonnull Supplier<Workbook> wb) {
        return new DataSetBuilder(wb);
    }

    public static DataSetBuilder create(@Nonnull Workbook wb) {
        return create(new Supplier<Workbook>() {
            @Override
            public Workbook get() {
                return wb;
            }

            @Override
            public String toString() {
                return "unknown_source";
            }
        });
    }

    /**
     * will accept each name only one time
     */
    public SheetsDataSetBuilder forSheets(@Nonnull String... names) {
        return forSheets(Arrays.asList(names));
    }

    /**
     * will accept each name only one time
     */
    private SheetsDataSetBuilder forSheets(@Nonnull final Collection<String> names) {
        return forSheets(new SheetByNameAccepter(names));
    }

    public SheetsDataSetBuilder forAllSheets() {
        return forSheets(always -> true);
    }

    /**
     * for stateless predicate
     */
    public SheetsDataSetBuilder forSheets(@Nonnull final Predicate<Sheet> stateless) {
        return forSheets(() -> stateless);
    }

    /**
     * for stateful predicate
     */
    public SheetsDataSetBuilder forSheets(@Nonnull final Supplier<Predicate<Sheet>> stateful) {
        return next(stateful);
    }

    private SheetsDataSetBuilder next(@Nonnull Supplier<Predicate<Sheet>> selectedSheets) {
        this.selectedSheets = selectedSheets;
        return new SheetsDataSetBuilder(this);
    }

    private static class SheetByNameAccepter implements Supplier<Predicate<Sheet>> {

        private final ImmutableList<String> names;

        private SheetByNameAccepter(@Nonnull Collection<String> names) {
            this.names = ImmutableList.copyOf(names);
        }

        @Override
        public Predicate<Sheet> get() {
            final Collection<String> namesCopy = Lists.newArrayList(names);
            return sheet -> namesCopy.remove(sheet.getSheetName());
        }
    }


}
