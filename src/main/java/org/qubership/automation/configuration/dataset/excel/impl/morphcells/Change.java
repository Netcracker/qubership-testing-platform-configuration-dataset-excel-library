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

package org.qubership.automation.configuration.dataset.excel.impl.morphcells;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;

public abstract class Change {

    /**
     * Cell object (target of the change).
     */
    private final Cell cell;

    /**
     * Constructor.
     *
     * @param cell Cell object.
     */
    public Change(@Nonnull final Cell cell) {
        this.cell = cell;
    }

    /**
     * Apply the change.
     */
    public abstract void apply();

    /**
     * Revert the change.
     */
    public abstract void revert();

    /**
     * Get Cell target of the Change.
     *
     * @return cell.
     */
    @Nonnull
    public Cell getTarget() {
        return cell;
    }
}
