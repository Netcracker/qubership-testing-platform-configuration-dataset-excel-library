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

import org.apache.poi.ss.usermodel.Cell;

public class VarsConvInfo<Param> {

    /**
     * Dataset Cell entity object (parent).
     */
    public final DSCell entity;

    /**
     * Dataset Cell parameter object.
     */
    public final DSCell param;

    /**
     * Converted Param object.
     */
    public final Param convertedParam;

    /**
     * Cell variable object.
     */
    public final Cell var;

    /**
     * Constructor.
     *
     * @param entity Dataset Cell entity object
     * @param param Dataset Cell parameter object
     * @param convertedParam Converted Param object
     * @param var Cell variable object.
     */
    public VarsConvInfo(@Nullable final DSCell entity,
                        @Nonnull final DSCell param,
                        @Nonnull final Param convertedParam,
                        @Nonnull final Cell var) {
        this.entity = entity;
        this.param = param;
        this.convertedParam = convertedParam;
        this.var = var;
    }
}
