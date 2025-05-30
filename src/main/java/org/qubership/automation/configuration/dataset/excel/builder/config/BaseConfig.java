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

package org.qubership.automation.configuration.dataset.excel.builder.config;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.impl.DSCell;

/**
 * Base settings holder.
 */
public class BaseConfig<Param, Params, Var, Vars> {

    /**
     * String qualifier of the source.
     */
    public final String sourceQualifier;

    /**
     * Workbook object.
     */
    public final Workbook wb; // for synchronize/locking purposes

    /**
     * Predicate of DSCells.
     */
    public final Predicate<DSCell> columnsPred;

    /**
     * Strategy of formulas re-evaluation.
     */
    public final ReevaluateFormulas evalStrategy;

    /**
     * Function to convert Params.
     */
    public final Function<Iterator<Param>, Params> paramsConverter;

    /**
     * Supplier of VarsEntryConverters.
     */
    public final Supplier<VarsEntryConverter<Param, Var>> varConverter;

    /**
     * Supplier of Functions.
     */
    public final Supplier<Function<Iterator<Var>, Vars>> varsConverter;

    /**
     * Predicate of Sheets.
     */
    public final Predicate<Sheet> sheetsPred;

    /**
     * Constructor.
     *
     * @param sourceQualifier String qualifier of the source
     * @param wb Workbook object
     * @param sheetsPred Predicate of Sheets
     * @param columnsPred Predicate of DSCells
     * @param paramsConverter Function to convert Params
     * @param varConverter Supplier of VarsEntryConverters
     * @param varsConverter Supplier of Functions
     * @param evalStrategy Strategy of formulas re-evaluation.
     */
    public BaseConfig(@Nonnull final String sourceQualifier,
                      @Nonnull final Workbook wb,
                      @Nonnull final Predicate<Sheet> sheetsPred,
                      @Nonnull final Predicate<DSCell> columnsPred,
                      @Nonnull final Function<Iterator<Param>, Params> paramsConverter,
                      @Nonnull final Supplier<VarsEntryConverter<Param, Var>> varConverter,
                      @Nonnull final Supplier<Function<Iterator<Var>, Vars>> varsConverter,
                      @Nonnull final ReevaluateFormulas evalStrategy) {
        this.sourceQualifier = sourceQualifier;
        this.wb = wb;
        this.evalStrategy = evalStrategy;
        this.columnsPred = columnsPred;
        this.paramsConverter = paramsConverter;
        this.varConverter = varConverter;
        this.varsConverter = varsConverter;
        this.sheetsPred = sheetsPred;
    }
}
