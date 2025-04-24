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
 * Holds base settings
 */
public class BaseConfig<Param, Params, Var, Vars> {
    public final String sourceQualifier;
    public final Workbook wb;//for synchronize/locking purposes
    public final Predicate<DSCell> columnsPred;
    public final ReevaluateFormulas evalStrategy;
    public final Function<Iterator<Param>, Params> paramsConverter;
    public final Supplier<VarsEntryConverter<Param, Var>> varConverter;
    public final Supplier<Function<Iterator<Var>, Vars>> varsConverter;
    public final Predicate<Sheet> sheetsPred;

    public BaseConfig(@Nonnull String sourceQualifier,
                      @Nonnull final Workbook wb,
                      @Nonnull Predicate<Sheet> sheetsPred,
                      @Nonnull Predicate<DSCell> columnsPred,
                      @Nonnull Function<Iterator<Param>, Params> paramsConverter,
                      @Nonnull Supplier<VarsEntryConverter<Param, Var>> varConverter,
                      @Nonnull Supplier<Function<Iterator<Var>, Vars>> varsConverter,
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
