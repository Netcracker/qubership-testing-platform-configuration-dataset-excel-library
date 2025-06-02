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

    /**
     * Dataset Cell entity object (parent).
     */
    private final DSCell entity;

    /**
     * Dataset Cell parameter object.
     */
    private final DSCell param;

    /**
     * Converted Param object.
     */
    private final Param convertedParam;

    /**
     * Dataset Cell variable object.
     */
    private final DSCell var;

    /**
     * Constructor.
     *
     * @param entity Dataset Cell entity object
     * @param param Dataset Cell parameter object
     * @param convertedParam Converted Param object
     * @param var Dataset Cell variable object.
     */
    public VarEntity(@Nullable final DSCell entity,
                     @Nonnull final DSCell param,
                     @Nonnull final Param convertedParam,
                     @Nonnull final DSCell var) {
        this.entity = entity;
        this.param = param;
        this.convertedParam = convertedParam;
        this.var = var;
    }

    /**
     * Get entity.
     *
     * @return DSCell object.
     */
    @Nullable
    public DSCell getEntity() {
        return entity;
    }

    /**
     * Get param.
     *
     * @return DSCell object.
     */
    @Nonnull
    public DSCell getParam() {
        return param;
    }

    /**
     * Get convertedParam.
     *
     * @return Param object.
     */
    @Nonnull
    public Param getConvertedParam() {
        return convertedParam;
    }

    /**
     * Get var.
     *
     * @return DSCell object.
     */
    @Nonnull
    public DSCell getVar() {
        return var;
    }
}
