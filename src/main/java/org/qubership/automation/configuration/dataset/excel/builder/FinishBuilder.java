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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reusable.
 * Stateless settings with an ability to pass settings thru suppliers.
 */
public class FinishBuilder<Param, Params, Var, Vars> {
    final Supplier<Workbook> workbook;
    final Supplier<Predicate<Sheet>> sheets;
    final Supplier<Predicate<DSCell>> columns;
    final Supplier<Function<Iterator<Param>, Params>> paramsConverter;
    final Supplier<Function<Iterator<Var>, Vars>> valuesConverter;
    final Supplier<ParamsEntryConverter<Param>> paramEntryConverter;
    final Supplier<VarsEntryConverter<Param, Var>> varEntryConverter;
    final ReevaluateFormulas evalStrat;

    FinishBuilder(@Nonnull Supplier<Workbook> workbook,
                  @Nonnull Supplier<Predicate<Sheet>> sheets,
                  @Nonnull Supplier<Predicate<DSCell>> columns,
                  @Nonnull Supplier<Function<Iterator<Param>, Params>> paramsConverter,
                  @Nonnull Supplier<Function<Iterator<Var>, Vars>> valuesConverter,
                  @Nonnull Supplier<ParamsEntryConverter<Param>> paramEntryConverter,
                  @Nonnull Supplier<VarsEntryConverter<Param, Var>> varEntryConverter,
                  @Nonnull ReevaluateFormulas evalStrat) {
        this.workbook = workbook;
        this.sheets = sheets;
        this.columns = columns;
        this.paramsConverter = paramsConverter;
        this.valuesConverter = valuesConverter;
        this.paramEntryConverter = paramEntryConverter;
        this.varEntryConverter = varEntryConverter;
        this.evalStrat = evalStrat;
    }

    public Iterator<DSList<Param, Params, Vars>> buildIterator() {
        return build().iterator();
    }

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

    public void fill(@Nonnull Adapter<Params, Vars> adapter) {
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
