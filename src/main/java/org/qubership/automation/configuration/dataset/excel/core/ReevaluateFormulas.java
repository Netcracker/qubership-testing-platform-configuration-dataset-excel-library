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

import org.qubership.automation.configuration.dataset.excel.impl.DSCell;

public enum ReevaluateFormulas {

    /**
     * Enum value to <b>never</b> evaluate formulas.
     */
    NEVER(false, false, false, false),

    /**<pre>
     * used when {@link DSCell#getValue()} is return param of {@link VarsEntryConverter}
     * and string value is not cached in converter
     *
     * cleans evaluation context on {@link DS#getVariables()} call,
     * so all the following operations with variables should be atomic and synchronized
     * </pre>
     */
    ON_DS_ACCESS(false, true, false, true),

    /**<pre>
     * used when {@link VarsEntryConverter} caches the {@link DSCell#getValue()}
     * in form of string for example by using {@link DSCell#getStringValue()}
     *
     * cleans evaluation context on {@link DS#getVariables()} call
     * and reuses {@link VarsEntryConverter} to be able to cache new evaluation results
     * </pre>
     */
    IN_CONVERTER(false, true, true, true),

    /**
     * Enum value to <b>always</b> evaluate formulas.
     */
    ALWAYS(true, false, true, true);

    public final boolean evaluationContextCleanup;
    public final boolean onDsCleanup;
    public final boolean reuseConverters;
    public final boolean reevaluateHeaders;

    /**
     * Constructor.
     *
     * @param evaluationContextCleanup boolean flag to clean up context before evaluation or not
     * @param onDsCleanup boolean flag to clean up context on DS change or not
     * @param reuseConverters boolean flag reuse converters or not
     * @param reevaluateHeaders boolean flag re-evaluate headers or not.
     */
    ReevaluateFormulas(final boolean evaluationContextCleanup,
                       final boolean onDsCleanup,
                       final boolean reuseConverters,
                       final boolean reevaluateHeaders) {
        this.evaluationContextCleanup = evaluationContextCleanup;
        this.onDsCleanup = onDsCleanup;
        this.reuseConverters = reuseConverters;
        this.reevaluateHeaders = reevaluateHeaders;
    }

}
