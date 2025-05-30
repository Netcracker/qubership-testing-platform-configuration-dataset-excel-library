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

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.impl.DSCell;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;
import org.qubership.automation.configuration.dataset.excel.impl.VarEntity;

public class VariablesBuilder<Param, Params> {

    /**
     * ParamsBuilder parent object.
     */
    private final ParamsBuilder parent;

    /**
     * Supplier of ParamsEntryConverters.
     */
    private final Supplier<ParamsEntryConverter<Param>> paramEntryConv;

    /**
     * Supplier of Functions.
     */
    private final Supplier<Function<Iterator<Param>, Params>> paramsConverter;

    /**
     * Constructor.
     *
     * @param parent ParamsBuilder parent object
     * @param paramEntryConv Supplier of ParamsEntryConverter
     * @param paramsConverter Supplier of Functions.
     */
    VariablesBuilder(@Nonnull final ParamsBuilder parent,
                     @Nonnull final Supplier<ParamsEntryConverter<Param>> paramEntryConv,
                     @Nonnull final Supplier<Function<Iterator<Param>, Params>> paramsConverter) {
        this.parent = parent;
        this.paramEntryConv = paramEntryConv;
        this.paramsConverter = paramsConverter;
    }

    /**
     * Makes the {@link DS#getVariables()} to return a Map of Param, Object
     * Param is specified on the previous builder step.
     *
     * @param reevaluate boolean; if true - ALWAYS mode, otherwise NEVER mode
     * @return FinishBuilder object.
     */
    public FinishBuilder<Param, Params, Pair<Param, Object>, Map<Param, Object>> paramToObjMap(
            final boolean reevaluate) {
        return mapVars((entity, param, convertedParam, value)
                -> Pair.of(convertedParam, value.getValue()),
                reevaluate ? ReevaluateFormulas.ALWAYS : ReevaluateFormulas.NEVER);
    }

    /**
     * Makes the {@link DS#getVariables()} to return a Map of Param, String
     * Param is specified on the previous builder step.
     *
     * @param reevaluate boolean; if true - IN_CONVERTER mode, otherwise NEVER mode
     * @return FinishBuilder object.
     */
    public FinishBuilder<Param, Params, Pair<Param, String>, Map<Param, String>> paramToStringMap(
            final boolean reevaluate) {
        return mapVars((entity, param, convertedParam, value) ->
                Pair.of(convertedParam, value.getStringValue()),
                reevaluate ? ReevaluateFormulas.IN_CONVERTER : ReevaluateFormulas.NEVER);
    }

    /**
     * Makes the {@link DS#getVariables()} to return a Map of Param, Var
     * Param is specified on the previous builder step.
     *
     * @param varEntryConverter See {@link VarsEntryConverter}
     * @param strategy Strategy of formulas re-evaluation
     * @return new FinishBuilder object.
     */
    public <Var> FinishBuilder<Param, Params, Pair<Param, Var>, Map<Param, Var>> mapVars(
            @Nonnull final VarsEntryConverter<Param, Pair<Param, Var>> varEntryConverter,
            @Nonnull final ReevaluateFormulas strategy) {
        return customVars(varEntryConverter, Utils.mapVarsFunc(), strategy);
    }

    /**
     * Makes the {@link DS#getVariables()} to return a Map of Param, Var
     * Param is specified on the previous builder step.
     *
     * @param varEntryConverter See {@link VarsEntryConverter}
     * @param varsConverter     aggregate function for the all Var.
     *                          Makes the {@link DS#getVariables()} to return the Vars
     * @param strategy Strategy of formulas re-evaluation
     * @return new FinishBuilder object.
     */
    public <Var, Vars> FinishBuilder<Param, Params, Var, Vars> customVars(
            @Nonnull final VarsEntryConverter<Param, Var> varEntryConverter,
            @Nonnull final Function<Iterator<Var>, Vars> varsConverter,
            @Nonnull final ReevaluateFormulas strategy) {
        return customVars(() -> varEntryConverter, () -> varsConverter, strategy);
    }

    /**
     * The same as {@link #customVars(VarsEntryConverter, Function, ReevaluateFormulas)}
     * with variables in form of {@link VarEntity}.
     *
     * @param varsConverter Functions
     * @param strategy Strategy of formulas re-evaluation
     * @return new FinishBuilder object.
     */
    public <Vars> FinishBuilder<Param, Params, VarEntity<Param>, Vars> customVars(
            @Nonnull final Function<Iterator<VarEntity<Param>>, Vars> varsConverter,
            @Nonnull final ReevaluateFormulas strategy) {
        return customVars(() -> varsConverter, strategy);
    }

    /**
     * The same as {@link #customVars(VarsEntryConverter, Function, ReevaluateFormulas)}
     * but with an ability to pass stateful converters, with variables in form of {@link VarEntity}.
     * Default Supplier of VarsEntryConverter is used.
     *
     * @param varsConverter Supplier of Functions
     * @param strategy Strategy of formulas re-evaluation
     * @return new FinishBuilder object.
     */
    public <Vars> FinishBuilder<Param, Params, VarEntity<Param>, Vars> customVars(
            @Nonnull final Supplier<Function<Iterator<VarEntity<Param>>, Vars>> varsConverter,
            @Nonnull final ReevaluateFormulas strategy) {
        Supplier<VarsEntryConverter<Param, VarEntity<Param>>> varEntryConverter = Utils::defaultVarEntryConv;
        return customVars(varEntryConverter, varsConverter, strategy);
    }

    /**
     * The same as {@link #customVars(VarsEntryConverter, Function, ReevaluateFormulas)}
     * but with an ability to pass stateful converters.
     *
     * @param varEntryConverter Supplier of VarsEntryConverter
     * @param varsConverter Supplier of Functions
     * @param strategy Strategy of formulas re-evaluation
     * @return new FinishBuilder object.
     */
    public <Var, Vars> FinishBuilder<Param, Params, Var, Vars> customVars(
            @Nonnull final Supplier<VarsEntryConverter<Param, Var>> varEntryConverter,
            @Nonnull final Supplier<Function<Iterator<Var>, Vars>> varsConverter,
            @Nonnull final ReevaluateFormulas strategy) {
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
