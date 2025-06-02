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
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;

public class ParamsBuilder {

    /**
     * SheetsDataSetBuilder object.
     */
    final SheetsDataSetBuilder parent;

    /**
     * Constructor.
     *
     * @param parent SheetsDataSetBuilder object.
     */
    ParamsBuilder(@Nonnull final SheetsDataSetBuilder parent) {
        this.parent = parent;
    }

    /**
     * Makes the {@link DSList#getParameters()} to return Param object.
     *
     * @param paramEntryConv function to generate the Params. See {@link ParamsEntryConverter}
     * @return VariablesBuilder object.
     */
    public <Param> VariablesBuilder<Param, List<Param>> listParams(
            @Nonnull final ParamsEntryConverter<Param> paramEntryConv) {
        return customParams(paramEntryConv, Utils.listParamsFunc());
    }

    /**
     * Makes the {@link DSList#getParameters()} to return the VariablesBuilder.
     *
     * @return VariablesBuilder object.
     */
    public VariablesBuilder<String, List<String>> listOfStringsParams() {
        return listParams(Utils.STRING_PARAMS_ENTRY_CONVERTER);
    }

    /**
     * Create VariablesBuilder based on converters given.
     *
     * @param paramEntryConv  will be used to do the Param-to-Value mapping. See {@link ParamsEntryConverter}
     * @param paramsConverter aggregate function for the all Param.
     *                        Makes the {@link DSList#getParameters()} to return the Params
     * @param <Param>         used in the {@link VarsEntryConverter}
     * @param <Params>        type of the {@link DSList#getParameters()}
     * @return new VariablesBuilder object based on suppliers of these converters.
     */
    public <Param, Params> VariablesBuilder<Param, Params> customParams(
            @Nonnull final ParamsEntryConverter<Param> paramEntryConv,
            @Nonnull final Function<Iterator<Param>, Params> paramsConverter) {
        return customParams(() -> paramEntryConv, () -> paramsConverter);
    }

    /**
     * The same as {@link #customParams(ParamsEntryConverter, Function)} but with stateful converters.
     *
     * @param paramEntryConv Supplier of ParamsEntryConverters
     * @param paramsConverter Supplier of Functions
     * @return new VariablesBuilder object based on these suppliers.
     */
    public <Param, Params> VariablesBuilder<Param, Params> customParams(
            @Nonnull final Supplier<ParamsEntryConverter<Param>> paramEntryConv,
            @Nonnull final Supplier<Function<Iterator<Param>, Params>> paramsConverter) {
        return new VariablesBuilder<>(this, paramEntryConv, paramsConverter);
    }
}
