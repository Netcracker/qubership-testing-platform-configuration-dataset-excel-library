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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class DataSetBuilder {

    /**
     * Workbook Supplier object.
     */
    final Supplier<Workbook> wb;

    /**
     * Supplier of Sheet Predicates object.
     */
    Supplier<Predicate<Sheet>> selectedSheets;

    /**
     * Constructor.
     *
     * @param wb Workbook Supplier to use.
     */
    DataSetBuilder(@Nonnull final Supplier<Workbook> wb) {
        this.wb = wb;
    }

    /**
     * Create DataSetBuilder for wb Workbook Supplier given.
     *
     * @param wb Workbook Supplier to use
     * @return a new DataSetBuilder object created for Workbook Supplier.
     */
    public static DataSetBuilder create(@Nonnull final Supplier<Workbook> wb) {
        return new DataSetBuilder(wb);
    }

    /**
     * Create DataSetBuilder for wb Workbook given.
     *
     * @param wb Workbook to process
     * @return a new DataSetBuilder object created for Workbook Supplier.
     */
    public static DataSetBuilder create(@Nonnull final Workbook wb) {
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
     * Get SheetsDataSetBuilder for specified names;
     * will accept each name only ones.
     *
     * @param names vararg String sheet names
     * @return SheetsDataSetBuilder object.
     */
    public SheetsDataSetBuilder forSheets(@Nonnull final String... names) {
        return forSheets(Arrays.asList(names));
    }

    /**
     * Get SheetsDataSetBuilder for specified names;
     * will accept each name only ones.
     *
     * @param names Collection of String sheet names
     * @return SheetsDataSetBuilder object.
     */
    private SheetsDataSetBuilder forSheets(@Nonnull final Collection<String> names) {
        return forSheets(new SheetByNameAccepter(names));
    }

    /**
     * Get SheetsDataSetBuilder for all sheets.
     *
     * @return SheetsDataSetBuilder object.
     */
    public SheetsDataSetBuilder forAllSheets() {
        return forSheets(always -> true);
    }

    /**
     * for stateless predicate.
     *
     * @param stateless Supplier of Sheet Predicates
     * @return SheetsDataSetBuilder object.
     */
    public SheetsDataSetBuilder forSheets(@Nonnull final Predicate<Sheet> stateless) {
        return forSheets(() -> stateless);
    }

    /**
     * for stateful predicate.
     *
     * @param stateful Supplier of Sheet Predicates
     * @return SheetsDataSetBuilder object as next(stateful).
     */
    public SheetsDataSetBuilder forSheets(@Nonnull final Supplier<Predicate<Sheet>> stateful) {
        return next(stateful);
    }

    private SheetsDataSetBuilder next(@Nonnull final Supplier<Predicate<Sheet>> selectedSheets) {
        this.selectedSheets = selectedSheets;
        return new SheetsDataSetBuilder(this);
    }

    private static class SheetByNameAccepter implements Supplier<Predicate<Sheet>> {

        /**
         * List of String names of Sheets.
         */
        private final ImmutableList<String> names;

        private SheetByNameAccepter(@Nonnull final Collection<String> names) {
            this.names = ImmutableList.copyOf(names);
        }

        @Override
        public Predicate<Sheet> get() {
            final Collection<String> namesCopy = Lists.newArrayList(names);
            return sheet -> namesCopy.remove(sheet.getSheetName());
        }
    }

}
