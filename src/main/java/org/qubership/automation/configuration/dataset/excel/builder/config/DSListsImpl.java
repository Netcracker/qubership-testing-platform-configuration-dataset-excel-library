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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.impl.AbstractDSFormer;
import org.qubership.automation.configuration.dataset.excel.impl.DSFormer;
import org.qubership.automation.configuration.dataset.excel.impl.EvaluationContext;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * This iterable makes possible to modify base config properties before use
 */
public class DSListsImpl<Param, Params, Var, Vars> implements DSLists<Param, Params, Vars> {
    public final DTBaseConfig<Param, Params, Var, Vars> baseConfig;
    private final Supplier<EvaluationContext> contextSup;
    private volatile EvaluationContext evaluationContext;

    public DSListsImpl(@Nonnull DTBaseConfig<Param, Params, Var, Vars> baseConfig) {
        this.baseConfig = baseConfig;
        final Workbook wb = baseConfig.config.wb;
        final ReevaluateFormulas strategy = baseConfig.config.evalStrategy;
        Supplier<EvaluationContext> contextSup = () -> new EvaluationContext(wb, strategy);
        if (!strategy.reevaluateHeaders) {
            contextSup = Utils.memoize(contextSup);
        }
        this.contextSup = contextSup;
    }

    private AbstractDSFormer<Param, Params, Var, Vars> doDSFormer(@Nonnull Sheet sheet) {
        return new DSFormer<>(sheet, baseConfig.config,
                baseConfig.paramsEntryConverter, baseConfig.varsEntryConverter,
                evaluationContext);
    }

    @Override
    public BaseConfig<Param, Params, ?, Vars> getConfig() {
        return baseConfig.config;
    }

    @Override
    public EvaluationContext getEvaluationContext() {
        return this.evaluationContext;
    }

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
                if (next != null)
                    return next;
                return endOfData();
            }
        };
    }

    private Iterator<Sheet> sheets() {
        return Iterators.filter(baseConfig.config.wb.sheetIterator(),
                baseConfig.config.sheetsPred::test);
    }

    @Override
    public String toString() {
        return baseConfig.config.sourceQualifier;
    }
}
