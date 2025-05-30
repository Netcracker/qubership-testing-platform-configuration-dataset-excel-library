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

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.qubership.automation.configuration.dataset.excel.builder.config.BaseConfig;
import org.qubership.automation.configuration.dataset.excel.builder.config.DTBaseConfig;
import org.qubership.automation.configuration.dataset.excel.core.Adapter;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.impl.DSCell;

/**
 * Reusable.
 * Stateless settings with an ability to pass settings through suppliers.
 */
public class FinishBuilder<Param, Params, Var, Vars> {

    /**
     * Workbook Supplier object.
     */
    final Supplier<Workbook> workbook;

    /**
     * Supplier of Sheet Predicates.
     */
    final Supplier<Predicate<Sheet>> sheets;

    /**
     * Supplier of DSCell Predicates.
     */
    final Supplier<Predicate<DSCell>> columns;

    /**
     * Supplier of Param Functions.
     */
    final Supplier<Function<Iterator<Param>, Params>> paramsConverter;

    /**
     * Supplier of Var Functions.
     */
    final Supplier<Function<Iterator<Var>, Vars>> valuesConverter;

    /**
     * Supplier of ParamsEntryConverter.
     */
    final Supplier<ParamsEntryConverter<Param>> paramEntryConverter;

    /**
     * Supplier of VarsEntryConverter.
     */
    final Supplier<VarsEntryConverter<Param, Var>> varEntryConverter;

    /**
     * Variant of Formulas Re-evaluation.
     */
    final ReevaluateFormulas evalStrat;

    /**
     * Constructor.
     *
     * @param workbook Workbook Supplier object
     * @param sheets Supplier of Sheet Predicates
     * @param columns Supplier of DSCell Predicates
     * @param paramsConverter Supplier of Param Functions
     * @param valuesConverter Supplier of Var Functions
     * @param paramEntryConverter Supplier of ParamsEntryConverter
     * @param varEntryConverter Supplier of VarsEntryConverter
     * @param evalStrat Variant of Formulas Re-evaluation.
     */
    FinishBuilder(@Nonnull final Supplier<Workbook> workbook,
                  @Nonnull final Supplier<Predicate<Sheet>> sheets,
                  @Nonnull final Supplier<Predicate<DSCell>> columns,
                  @Nonnull final Supplier<Function<Iterator<Param>, Params>> paramsConverter,
                  @Nonnull final Supplier<Function<Iterator<Var>, Vars>> valuesConverter,
                  @Nonnull final Supplier<ParamsEntryConverter<Param>> paramEntryConverter,
                  @Nonnull final Supplier<VarsEntryConverter<Param, Var>> varEntryConverter,
                  @Nonnull final ReevaluateFormulas evalStrat) {
        this.workbook = workbook;
        this.sheets = sheets;
        this.columns = columns;
        this.paramsConverter = paramsConverter;
        this.valuesConverter = valuesConverter;
        this.paramEntryConverter = paramEntryConverter;
        this.varEntryConverter = varEntryConverter;
        this.evalStrat = evalStrat;
    }

    /**
     * Create DSList Iterator.
     *
     * @return new DSList Iterator object.
     */
    public Iterator<DSList<Param, Params, Vars>> buildIterator() {
        return build().iterator();
    }

    /**
     * Create DSLists.
     *
     * @return new DSLists object.
     */
    public DSLists<Param, Params, Vars> build() {
        BaseConfig<Param, Params, Var, Vars> config = new BaseConfig<>(workbook.toString(),
                workbook.get(),
                sheets.get(),
                columns.get(),
                paramsConverter.get(),
                varEntryConverter,
                valuesConverter,
                evalStrat);
        return (new DTBaseConfig<>(config, paramEntryConverter.get(), varEntryConverter.get())).build();
    }

    /**
     * Fill DSLists.
     *
     * @param adapter Adapter of Params vs. Vars.
     */
    public void fill(@Nonnull final Adapter<Params, Vars> adapter) {
        Iterator<DSList<Param, Params, Vars>> listIter = buildIterator();
        while (listIter.hasNext()) {
            DSList<Param, Params, Vars> list = listIter.next();
            adapter.doDataSetList(list.getName(), list.getParameters());
            for (DS<Param, Vars> ds : list.getDataSets()) {
                adapter.doDataSet(ds.getName(), ds.getVariables());
            }
        }
    }
}
