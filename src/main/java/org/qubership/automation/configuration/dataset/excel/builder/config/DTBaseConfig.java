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

import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;

import javax.annotation.Nonnull;

public class DTBaseConfig<Param, Params, Var, Vars> {

    public final BaseConfig<Param, Params, Var, Vars> config;
    public final ParamsEntryConverter<Param> paramsEntryConverter;
    public final VarsEntryConverter<Param, Var> varsEntryConverter;

    public DTBaseConfig(@Nonnull BaseConfig<Param, Params, Var, Vars> config,
                        @Nonnull ParamsEntryConverter<Param> paramsEntryConverter,
                        @Nonnull VarsEntryConverter<Param, Var> varsEntryConverter) {
        this.config=config;
        this.paramsEntryConverter = paramsEntryConverter;
        this.varsEntryConverter = varsEntryConverter;
    }

    @Nonnull
    public DSLists<Param, Params, Vars> build() {
        return new DSListsImpl<>(this);
    }
}
