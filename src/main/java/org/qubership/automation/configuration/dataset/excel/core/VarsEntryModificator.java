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
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;
import org.qubership.automation.configuration.dataset.excel.impl.DSCell;
import org.qubership.automation.configuration.dataset.excel.impl.morphcells.Changelist;

public interface VarsEntryModificator<Param> {

    /**
     * @param value proxy, which can track changes,
     *              see {@link Changelist}
     */
    void modify(@Nullable DSCell entity,
                @Nonnull DSCell param,
                @Nonnull Param convertedParam,
                @Nonnull Cell value);
}
