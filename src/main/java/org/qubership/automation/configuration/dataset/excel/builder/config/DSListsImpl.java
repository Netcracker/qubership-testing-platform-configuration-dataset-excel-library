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
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.impl.AbstractDSFormer;
import org.qubership.automation.configuration.dataset.excel.impl.DSFormer;
import org.qubership.automation.configuration.dataset.excel.impl.EvaluationContext;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

/**
 * This iterable makes possible to modify base config properties before use.
 */
public class DSListsImpl<Param, Params, Var, Vars> implements DSLists<Param, Params, Vars> {

    /**
     * DTBaseConfig object.
     */
    public final DTBaseConfig<Param, Params, Var, Vars> baseConfig;

    /**
     * Supplier of EvaluationContext.
     */
    private final Supplier<EvaluationContext> contextSup;

    /**
     * EvaluationContext object.
     */
    private volatile EvaluationContext evaluationContext;

    /**
     * Constructor.
     *
     * @param baseConfig DTBaseConfig object.
     */
    public DSListsImpl(@Nonnull final DTBaseConfig<Param, Params, Var, Vars> baseConfig) {
        this.baseConfig = baseConfig;
        final Workbook wb = baseConfig.config.wb;
        final ReevaluateFormulas strategy = baseConfig.config.evalStrategy;
        Supplier<EvaluationContext> contextSup = () -> new EvaluationContext(wb, strategy);
        if (!strategy.reevaluateHeaders) {
            contextSup = Utils.memoize(contextSup);
        }
        this.contextSup = contextSup;
    }

    private AbstractDSFormer<Param, Params, Var, Vars> doDSFormer(@Nonnull final Sheet sheet) {
        return new DSFormer<>(sheet, baseConfig.config,
                baseConfig.paramsEntryConverter, baseConfig.varsEntryConverter,
                evaluationContext);
    }

    /**
     * Get config.
     *
     * @return BaseConfig object.
     */
    @Override
    public BaseConfig<Param, Params, ?, Vars> getConfig() {
        return baseConfig.config;
    }

    /**
     * Get evaluationContext.
     *
     * @return EvaluationContext object.
     */
    @Override
    public EvaluationContext getEvaluationContext() {
        return this.evaluationContext;
    }

    /**
     * Get DSList iterator.
     *
     * @return DSList iterator.
     */
    @Override
    public Iterator<DSList<Param, Params, Vars>> iterator() {
        this.evaluationContext = contextSup.get();
        final Iterator<Sheet> sheets = sheets();
        return new AbstractIterator<DSList<Param, Params, Vars>>() {
            @Override
            protected DSList<Param, Params, Vars> computeNext() {
                DSList<Param, Params, Vars> next = null;
                while (sheets.hasNext() && next == null) {
                    next = doDSFormer(sheets.next()).get();
                }
                return next == null ? endOfData() : next;
            }
        };
    }

    private Iterator<Sheet> sheets() {
        return Iterators.filter(baseConfig.config.wb.sheetIterator(),
                baseConfig.config.sheetsPred::test);
    }

    /**
     * Make String representation of the object.
     *
     * @return String representation of the object.
     */
    @Override
    public String toString() {
        return baseConfig.config.sourceQualifier;
    }
}
