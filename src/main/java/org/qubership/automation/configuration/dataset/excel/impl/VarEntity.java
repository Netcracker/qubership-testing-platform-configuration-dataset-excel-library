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
import javax.annotation.Nullable;

public class VarEntity<Param> {
    private final DSCell entity;
    private final DSCell param;
    private final Param convertedParam;
    private final DSCell var;

    public VarEntity(@Nullable DSCell entity,
                     @Nonnull DSCell param,
                     @Nonnull Param convertedParam,
                     @Nonnull DSCell var) {
        this.entity = entity;
        this.param = param;
        this.convertedParam = convertedParam;
        this.var = var;
    }

    @Nullable
    public DSCell getEntity() {
        return entity;
    }

    @Nonnull
    public DSCell getParam() {
        return param;
    }

    @Nonnull
    public Param getConvertedParam() {
        return convertedParam;
    }

    @Nonnull
    public DSCell getVar() {
        return var;
    }
}
