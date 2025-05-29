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

import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.impl.DSCell;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;
import org.qubership.automation.configuration.dataset.excel.impl.VarEntity;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class VariablesBuilder<Param, Params> {
    private final ParamsBuilder parent;
    private final Supplier<ParamsEntryConverter<Param>> paramEntryConv;
    private final Supplier<Function<Iterator<Param>, Params>> paramsConverter;

    VariablesBuilder(@Nonnull ParamsBuilder parent,
                     @Nonnull Supplier<ParamsEntryConverter<Param>> paramEntryConv,
                     @Nonnull Supplier<Function<Iterator<Param>, Params>> paramsConverter) {
        this.parent = parent;
        this.paramEntryConv = paramEntryConv;
        this.paramsConverter = paramsConverter;
    }

    /**
     * <pre>
     * Makes the {@link DS#getVariables()} to return a Map of Param, Object
     * Param is specified on the previous builder step
     * </pre>
     *
     * @return
     */
    public FinishBuilder<Param, Params, Pair<Param, Object>, Map<Param, Object>> paramToObjMap(boolean reevaluate) {
        return mapVars(new VarsEntryConverter<Param, Pair<Param, Object>>() {
            @Nullable
            @Override
            public Pair<Param, Object> doVarsEntry(@Nullable DSCell entity, @Nonnull DSCell param, @Nonnull Param convertedParam, @Nonnull DSCell value) {
                return Pair.of(convertedParam, value.getValue());
            }
        }, reevaluate ? ReevaluateFormulas.ALWAYS : ReevaluateFormulas.NEVER);
    }

    public FinishBuilder<Param, Params, Pair<Param, String>, Map<Param, String>> paramToStringMap(boolean reevaluate) {
        return mapVars(new VarsEntryConverter<Param, Pair<Param, String>>() {
            @Nullable
            @Override
            public Pair<Param, String> doVarsEntry(@Nullable DSCell entity, @Nonnull DSCell param, @Nonnull Param convertedParam, @Nonnull DSCell value) {
                return Pair.of(convertedParam, value.getStringValue());
            }
        }, reevaluate ? ReevaluateFormulas.IN_CONVERTER : ReevaluateFormulas.NEVER);
    }

    /**
     * <pre>
     *     Makes the {@link DS#getVariables()} to return a Map of Param, Var
     *     Param is specified on the previous builder step
     * </pre>
     *
     * @param varEntryConverter See {@link VarsEntryConverter}
     * @param <Var>             part of the {@link DS#getVariables()}
     * @return
     */
    public <Var> FinishBuilder<Param, Params, Pair<Param, Var>, Map<Param, Var>> mapVars(@Nonnull VarsEntryConverter<Param, Pair<Param, Var>> varEntryConverter,
                                                                                         @Nonnull ReevaluateFormulas strategy) {
        return customVars(varEntryConverter, Utils.<Param, Var>mapVarsFunc(), strategy);
    }

    /**
     * @param varEntryConverter See {@link VarsEntryConverter}
     * @param varsConverter     aggregate function for the all Var.
     *                          Makes the {@link DS#getVariables()} to return the Vars
     * @param <Var>             part of the {@link DS#getVariables()}
     * @param <Vars>            type of the {@link DS#getVariables()}
     * @return
     */
    public <Var, Vars> FinishBuilder<Param, Params, Var, Vars> customVars(@Nonnull VarsEntryConverter<Param, Var> varEntryConverter,
                                                                          @Nonnull Function<Iterator<Var>, Vars> varsConverter,
                                                                          @Nonnull ReevaluateFormulas strategy) {
        return customVars(() -> varEntryConverter, () -> varsConverter, strategy);
    }

    /**
     * same as {@link #customVars(VarsEntryConverter, Function, ReevaluateFormulas)}
     * with variables in form of {@link VarEntity}
     */
    public <Vars> FinishBuilder<Param, Params, VarEntity<Param>, Vars> customVars(@Nonnull Function<Iterator<VarEntity<Param>>, Vars> varsConverter,
                                                                                  @Nonnull ReevaluateFormulas strategy) {
        return customVars(() -> varsConverter, strategy);
    }

    /**
     * same as {@link #customVars(VarsEntryConverter, Function, ReevaluateFormulas)}
     * but with an ability to pass stateful converters,
     * with variables in form of {@link VarEntity}
     */
    public <Vars> FinishBuilder<Param, Params, VarEntity<Param>, Vars> customVars(@Nonnull Supplier<Function<Iterator<VarEntity<Param>>, Vars>> varsConverter,
                                                                                  @Nonnull ReevaluateFormulas strategy) {
        Supplier<VarsEntryConverter<Param, VarEntity<Param>>> varEntryConverter = () -> (Utils.defaultVarEntryConv());
        return customVars(varEntryConverter, varsConverter, strategy);
    }

    /**
     * same as {@link #customVars(VarsEntryConverter, Function, ReevaluateFormulas)}
     * but with an ability to pass stateful converters
     */
    public <Var, Vars> FinishBuilder<Param, Params, Var, Vars> customVars(@Nonnull Supplier<VarsEntryConverter<Param, Var>> varEntryConverter,
                                                                          @Nonnull Supplier<Function<Iterator<Var>, Vars>> varsConverter,
                                                                          @Nonnull ReevaluateFormulas strategy) {
        return new FinishBuilder<>(parent.parent.parent.wb,
                parent.parent.parent.selectedSheets,
                parent.parent.columns,
                paramsConverter,
                varsConverter,
                paramEntryConv,
                varEntryConverter,
                strategy);
    }
}
