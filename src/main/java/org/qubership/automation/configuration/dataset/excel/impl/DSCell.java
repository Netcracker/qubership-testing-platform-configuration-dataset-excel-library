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

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;

public class DSCell {

    /**
     * EvaluationContext object.
     */
    private final EvaluationContext evaluator;

    /**
     * Cell object.
     */
    private final Cell cell;

    /**
     * Constructor.
     *
     * @param cell Cell object
     * @param evaluator EvaluationContext object.
     */
    public DSCell(@Nonnull final Cell cell, @Nonnull final EvaluationContext evaluator) {
        this.cell = cell;
        this.evaluator = evaluator;
    }

    /**
     * <pre>
     * returns an object which is able to recalculate its string value each time {@link Object#toString()} invoked
     * in case if cell is actually a formula and represents mutable data (values).
     * Invoke of {@link Object#toString()} may return {@link org.apache.commons.lang3.StringUtils#EMPTY},
     * see {@link EvaluationContext#getCellValue(org.apache.poi.ss.usermodel.Cell)}
     *
     *
     * can be used as {@link DS#getVariables()} return parameters
     * to be able to acquire fresh calculated string value each time accessed
     * use {@link ReevaluateFormulas#IN_CONVERTER} for that case
     *
     * </pre>
     */
    @Nonnull
    public Object getValue() {
        return evaluator.getCellValue(cell);
    }

    /**
     * just a shortcut of {@link #getValue}.toString()
     */
    @Nonnull
    public String getStringValue() {
        return getValue().toString();
    }

    /**
     * Get cell.
     *
     * @return Cell object.
     */
    @Nonnull
    public Cell getCell() {
        return cell;
    }

    /**
     * Get evaluator.
     *
     * @return EvaluationContext object.
     */
    @Nonnull
    public EvaluationContext getEvaluator() {
        return evaluator;
    }

}
