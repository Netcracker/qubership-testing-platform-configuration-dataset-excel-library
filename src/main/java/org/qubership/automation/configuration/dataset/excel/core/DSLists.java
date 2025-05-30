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

import org.qubership.automation.configuration.dataset.excel.builder.config.BaseConfig;
import org.qubership.automation.configuration.dataset.excel.impl.EvaluationContext;

public interface DSLists<Param, Params, Vars> extends Iterable<DSList<Param, Params, Vars>> {

    /**
     * Get config.
     *
     * @return BaseConfig object.
     */
    BaseConfig<Param, Params, ?, Vars> getConfig();

    /**
     * Get Evaluation Context.
     *
     * @return EvaluationContext object.
     */
    EvaluationContext getEvaluationContext();
}
