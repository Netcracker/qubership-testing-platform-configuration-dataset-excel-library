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

package org.qubership.automation.configuration.dataset.excel.tracker.base;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.FormulaEvaluator;

public interface RefsSupplier {

    /**
     * Get reference.
     *
     * @param dependent Resource dependent resource object
     * @param path String path to resource
     * @return AbstractResource ot FormulaEvaluator
     * @throws Exception in case different errors occurred.
     */
    @Nonnull
    AbstractResource<FormulaEvaluator> getRef(@Nonnull Resource dependent, @Nonnull String path) throws Exception;

    /**
     * Set Up collaborating Environment.
     *
     * @param resource AbstractResource object
     * @throws Exception in case different errors occurred.
     */
    void setupCollaboratingEnv(@Nonnull AbstractResource<?> resource) throws Exception;
}
