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

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import com.google.common.collect.Lists;
import org.qubership.automation.configuration.dataset.excel.builder.config.BaseConfig;
import org.qubership.automation.configuration.dataset.excel.core.Consumer;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;

public class DSFormer<Param, Params, Var, Vars> extends AbstractDSFormer<Param, Params, Var, Vars> {

    private static final String PARAMETER_COL_NAME = "Parameter";
    private static final String ENTITY_COL_NAME = "Entity";

    private final Predicate<Cell> entityPred;
    private final Predicate<Cell> paramsPred;
    private final ParamsEntryConverter<Param> paramsEntryConverter;
    private final VarsEntryConverter<Param, Var> varsEntryConverter;

    private DSCell curEntity;
    private DSCell curParam;
    private Param convertedParam;

    public DSFormer(@Nonnull Sheet sheet,
                    @Nonnull BaseConfig<Param, Params, Var, Vars> settings,
                    @Nonnull ParamsEntryConverter<Param> paramsEntryConverter,
                    @Nonnull VarsEntryConverter<Param, Var> varsEntryConverter,
                    @Nonnull EvaluationContext evaluationContext) {
        super(sheet, settings, evaluationContext);
        this.paramsEntryConverter = paramsEntryConverter;
        this.varsEntryConverter = varsEntryConverter;
        entityPred = Utils.statefulHeaderPredicate(evaluationContext, ENTITY_COL_NAME);
        paramsPred = Utils.statefulHeaderPredicate(evaluationContext, PARAMETER_COL_NAME);
    }

    @Nullable
    protected List<Predicate<Cell>> getMandatoryColumns() {
        return Lists.newArrayList(entityPred, paramsPred);
    }

    @Nullable
    @Override
    public Consumer<Cell> getHandler(@Nonnull Cell headerCell, @Nonnull Predicate<Cell> predicate) {
        if (predicate.equals(entityPred)) {
            return input -> {
                DSCell entity = new DSCell(input, evaluationContext);
                if (!entity.getStringValue().isEmpty()) {
                    curEntity = entity;
                }
            };
        } else if (predicate.equals(paramsPred)) {
            //register dsList
            doDSList();
            return this::pushToDSList;
        } else {
            //register ds
            //Preconditions.checkNotNull(dsList,"No parameters column found");
            final DSImpl<Param, Var, Vars> ds = doDS(headerCell);
            return input -> pushToDS(ds, input);
        }
    }

    @Override
    protected void nextRow() {
        //IT need for NITP-4060, NITP-4080
        curParam = null;
        convertedParam = null;
    }

    protected void pushToDS(@Nonnull DSImpl<Param, Var, Vars> ds, @Nonnull Cell input) {
        if (convertedParam != null)//for case when convertedParam has been filtered
        {
            ds.accept(new VarsConvInfo<>(curEntity, curParam, convertedParam, input));
        }
    }

    protected void pushToDSList(@Nonnull Cell input) {
        curParam = new DSCell(input, evaluationContext);
        convertedParam = paramsEntryConverter.doParamsEntry(curEntity, new DSCell(input, evaluationContext));
        if (convertedParam != null)//for case when convertedParam has been filtered
        {
            pushToDSList(convertedParam);
        }
    }
}
