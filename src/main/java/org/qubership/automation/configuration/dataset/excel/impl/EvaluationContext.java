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

    private final Workbook wb;
    private final Runnable cleanup;
    private final PropertyDescriptors descriptors;
    private final ReevaluateFormulas strategy;
    private volatile boolean initialized;
    private FormulaEvaluator eval;


    public EvaluationContext(@Nonnull Workbook wb, @Nonnull ReevaluateFormulas strategy) {
        this.wb = wb;
        this.strategy = strategy;
        this.cleanup=cleanupRunnable(strategy.evaluationContextCleanup,this);
        this.descriptors = PropertyDescriptors.get(wb);
    }

    public static Runnable cleanupRunnable(boolean doCleanup,
                                                @Nonnull final EvaluationContext evaluator) {
        if (doCleanup) {
            return EvaluationContext.cleanupRunnable(evaluator);
        } else {
            return Runnables.doNothing();
        }
    }

    public static Runnable cleanupRunnable(@Nonnull final EvaluationContext eval) {
        return new Runnable() {
            @Override
            public void run() {
                eval.clearFormulasCache();
            }

            @Override
            public String toString() {
                return "clearFormulasCache() invoke";
            }
        };
    }

    @Nonnull
    public Object getCellValue(@Nonnull final Cell cell) {
        synchronized (cell) {
            CellType cellType = cell.getCellType();
            if (cellType == CellType.FORMULA) //for lazy invoke on supplier
            {
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

    public FormulaEvaluator evaluator() {
        // A 2-field variant of Double Checked Locking.
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

    public void clearFormulasCache() {
        if (!initialized)
            return;
        synchronized (this) {
            eval.clearAllCachedResultValues();
        }
    }

    public <V> V doThreadSafeUnchecked(@Nonnull Callable<V> callable) {
        synchronized (wb) {
            try {
                return callable.call();
            } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public <V> V doThreadSafe(@Nonnull Callable<V> callable) throws Exception {
        synchronized (wb) {
            return callable.call();
        }
    }

    public void doThreadSafe(@Nonnull Runnable runnable) {
        synchronized (wb) {
            runnable.run();
        }
    }

    private CellType evaluateFormulaCell(@Nonnull final Cell cell) {
        try {
            return evaluator().evaluateFormulaCell(cell);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Can not evaluate formula in cell [%s] on sheet [%s]", cell.getAddress(), cell.getSheet().getSheetName()), e);
        }
    }

    @Nonnull
    public PropertyDescriptors getDescriptors() {
        return descriptors;
    }

    public ReevaluateFormulas getStrategy() {
        return strategy;
    }

    @Nonnull
    private Object getCellValue(CellType cellType, @Nonnull final Cell cell) {
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
