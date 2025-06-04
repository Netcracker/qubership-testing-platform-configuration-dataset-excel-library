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

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.impl.morphcells.PropertyDescriptors;
import org.qubership.automation.configuration.dataset.excel.impl.morphcells.UniTypeCell;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Runnables;

public class EvaluationContext {

    /**
     * Workbook object.
     */
    private final Workbook wb;

    /**
     * Runnable to perform context cleanup.
     */
    private final Runnable cleanup;

    /**
     * PropertyDescriptors object.
     */
    private final PropertyDescriptors descriptors;

    /**
     * Strategy of formulas re-evaluation.
     */
    private final ReevaluateFormulas strategy;

    /**
     * Flag if the context is evaluated or not.
     */
    private volatile boolean initialized;

    /**
     * Formula Evaluator object.
     */
    private FormulaEvaluator eval;


    /**
     * Constructor.
     *
     * @param wb Workbook object
     * @param strategy Strategy of formulas re-evaluation.
     */
    public EvaluationContext(@Nonnull final Workbook wb, @Nonnull final ReevaluateFormulas strategy) {
        this.wb = wb;
        this.strategy = strategy;
        this.cleanup = cleanupRunnable(strategy.evaluationContextCleanup, this);
        this.descriptors = PropertyDescriptors.get(wb);
    }

    /**
     * Make Runnable to perform context cleanup if doCleanup = true, otherwise do nothing.
     *
     * @param doCleanup boolean flag to perform cleanup actually (if true) or not
     * @param evaluator EvaluationContext object to clean up
     * @return Runnable to perform cleanup or do nothing, depending on doCleanup value.
     */
    public static Runnable cleanupRunnable(final boolean doCleanup,
                                           @Nonnull final EvaluationContext evaluator) {
        return doCleanup ? EvaluationContext.cleanupRunnable(evaluator) : Runnables.doNothing();
    }

    /**
     * Runnable to perform evaluation context cleanup.
     *
     * @param eval EvaluationContext object to clean up
     * @return Runnable object.
     */
    public static Runnable cleanupRunnable(@Nonnull final EvaluationContext eval) {
        return new Runnable() {
            /**
             * Run this runnable object.
             */
            @Override
            public void run() {
                eval.clearFormulasCache();
            }

            /**
             * Make String representation of the object.
             *
             * @return String representation of the object.
             */
            @Override
            public String toString() {
                return "clearFormulasCache() invoke";
            }
        };
    }

    /**
     * Get Cell value.
     *
     * @param cell Cell object
     * @return Object value of Cell; in case Cell Type is CellType#FORMULA - evaluate the formula firstly,
     * and return String representation of the result.
     */
    @Nonnull
    public Object getCellValue(@Nonnull final Cell cell) {
        synchronized (cell) {
            CellType cellType = cell.getCellType();
            if (cellType == CellType.FORMULA) {
                //for lazy invoke on supplier
                return new Object() {
                    @Override
                    public String toString() {
                        synchronized (EvaluationContext.this) {
                            cleanup.run();
                            return getCellValue(evaluateFormulaCell(cell), cell).toString();
                        }
                    }
                };
            } else {
                return getCellValue(cellType, cell);
            }
        }
    }

    /**
     * Get Formula Evaluator; create it if not initialized yet.
     *
     * @return FormulaEvaluator eval object.
     */
    public FormulaEvaluator evaluator() {
        // A 2-field variant of Double-checked Locking.
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    eval = wb.getCreationHelper().createFormulaEvaluator();
                    initialized = true;
                    return eval;
                }
            }
        }
        return eval;
    }

    /**
     * Clear formulas cache.
     */
    public void clearFormulasCache() {
        if (!initialized) {
            return;
        }
        synchronized (this) {
            eval.clearAllCachedResultValues();
        }
    }

    /**
     * Execute callable synchronized for wb Workbook.
     *
     * @param callable Callable object to execute
     * @return &lt;V&gt; object.
     */
    public <V> V doThreadSafeUnchecked(@Nonnull final Callable<V> callable) {
        synchronized (wb) {
            try {
                return callable.call();
            } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * Execute callable thread-safe way (synchronized for wb Workbook).
     *
     * @param callable Callable object to execute
     * @return &lt;V&gt; object
     * @throws Exception in case execution errors occurred.
     */
    public <V> V doThreadSafe(@Nonnull final Callable<V> callable) throws Exception {
        synchronized (wb) {
            return callable.call();
        }
    }

    /**
     * Execute runnable thread-safe way (synchronized for wb Workbook).
     *
     * @param runnable Runnable object to execute.
     */
    public void doThreadSafe(@Nonnull final Runnable runnable) {
        synchronized (wb) {
            runnable.run();
        }
    }

    private CellType evaluateFormulaCell(@Nonnull final Cell cell) {
        try {
            return evaluator().evaluateFormulaCell(cell);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Can not evaluate formula in cell [%s] on sheet [%s]",
                    cell.getAddress(), cell.getSheet().getSheetName()), e);
        }
    }

    /**
     * Get descriptors.
     *
     * @return PropertyDescriptors object.
     */
    @Nonnull
    public PropertyDescriptors getDescriptors() {
        return descriptors;
    }

    /**
     * Get formula re-evaluation strategy.
     *
     * @return ReevaluateFormulas object.
     */
    public ReevaluateFormulas getStrategy() {
        return strategy;
    }

    @Nonnull
    private Object getCellValue(final CellType cellType, @Nonnull final Cell cell) {
        switch (cellType) {
            case _NONE:
            case ERROR:
                throw new IllegalArgumentException(
                        String.format("Can not evaluate formula [%s] in cell [%s] on sheet [%s]",
                                getDescriptors().forCell(cellType, cell).getToString(cell),
                                cell.getAddress(),
                                cell.getSheet().getSheetName()));
            case BLANK:
                return StringUtils.EMPTY;
            default:
                //noinspection unchecked
                return new UniTypeCell<>(getDescriptors().forCell(cellType, cell), cell);
        }
    }
}
