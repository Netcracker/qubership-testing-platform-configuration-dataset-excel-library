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
import org.qubership.automation.configuration.dataset.excel.builder.config.BaseConfig;
import org.qubership.automation.configuration.dataset.excel.core.Consumer;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;

import com.google.common.collect.Lists;

public class DSFormer<Param, Params, Var, Vars> extends AbstractDSFormer<Param, Params, Var, Vars> {

    /**
     * Constant for Parameter Column Name.
     */
    private static final String PARAMETER_COL_NAME = "Parameter";

    /**
     * Constant for Entity Column Name.
     */
    private static final String ENTITY_COL_NAME = "Entity";

    /**
     * Cells Predicate for entity.
     */
    private final Predicate<Cell> entityPred;

    /**
     * Cells Predicate for parameter.
     */
    private final Predicate<Cell> paramsPred;

    /**
     * Parameter Entry Converter object.
     */
    private final ParamsEntryConverter<Param> paramsEntryConverter;

    /**
     * Vars Entry Converter object.
     */
    private final VarsEntryConverter<Param, Var> varsEntryConverter;

    /**
     * Current DSCell entity.
     */
    private DSCell curEntity;

    /**
     * Current DSCell parameter.
     */
    private DSCell curParam;

    /**
     * Converted parameter.
     */
    private Param convertedParam;

    /**
     * Constructor.
     *
     * @param sheet Sheet object
     * @param settings Base Config object
     * @param paramsEntryConverter ParamsEntryConverter object
     * @param varsEntryConverter VarsEntryConverter object
     * @param evaluationContext EvaluationContext object.
     */
    public DSFormer(@Nonnull final Sheet sheet,
                    @Nonnull final BaseConfig<Param, Params, Var, Vars> settings,
                    @Nonnull final ParamsEntryConverter<Param> paramsEntryConverter,
                    @Nonnull final VarsEntryConverter<Param, Var> varsEntryConverter,
                    @Nonnull final EvaluationContext evaluationContext) {
        super(sheet, settings, evaluationContext);
        this.paramsEntryConverter = paramsEntryConverter;
        this.varsEntryConverter = varsEntryConverter;
        entityPred = Utils.statefulHeaderPredicate(evaluationContext, ENTITY_COL_NAME);
        paramsPred = Utils.statefulHeaderPredicate(evaluationContext, PARAMETER_COL_NAME);
    }

    /**
     * Get mandatory columns list.
     *
     * @return List of Cell Predicates.
     */
    @Nullable
    protected List<Predicate<Cell>> getMandatoryColumns() {
        return Lists.newArrayList(entityPred, paramsPred);
    }

    /**
     * Get Handler.
     *
     * @param headerCell Cell of column header
     * @param predicate  Predicate of Cells
     * @return Cell Consumer object.
     */
    @Nullable
    @Override
    public Consumer<Cell> getHandler(@Nonnull final Cell headerCell, @Nonnull final Predicate<Cell> predicate) {
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

    /**
     * Go to the next row.
     */
    @Override
    protected void nextRow() {
        curParam = null;
        convertedParam = null;
    }

    /**
     * Push Cell input to ds Dataset.
     *
     * @param ds DSImpl object
     * @param input Cell to add.
     */
    protected void pushToDS(@Nonnull final DSImpl<Param, Var, Vars> ds, @Nonnull final Cell input) {
        if (convertedParam != null) {
            // for case when convertedParam has been filtered
            ds.accept(new VarsConvInfo<>(curEntity, curParam, convertedParam, input));
        }
    }

    /**
     * Push Cell input to DatasetList.
     *
     * @param input Cell to add.
     */
    protected void pushToDSList(@Nonnull final Cell input) {
        curParam = new DSCell(input, evaluationContext);
        convertedParam = paramsEntryConverter.doParamsEntry(curEntity, new DSCell(input, evaluationContext));
        if (convertedParam != null) {
            // for case when convertedParam has been filtered
            pushToDSList(convertedParam);
        }
    }
}
