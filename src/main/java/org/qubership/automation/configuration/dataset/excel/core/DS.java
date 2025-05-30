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

package org.qubership.automation.configuration.dataset.excel.core;

import javax.annotation.Nonnull;

public interface DS<Param, Vars> extends Named {

    /**
     * Get variables.
     *
     * @return Vars object.
     */
    Vars getVariables();

    /**
     * Get variables; works only with {@link ReevaluateFormulas#IN_CONVERTER} strategy.
     *
     * @param modificator did modifications of value cells of each variable entry;
     *                    these modifications are temporarily, just for returned vars
     * @return Vars object
     * @throws IllegalArgumentException if reevaluate formulas strategy is wrong.
     */
    Vars getVariables(@Nonnull VarsEntryModificator<Param> modificator);
}
