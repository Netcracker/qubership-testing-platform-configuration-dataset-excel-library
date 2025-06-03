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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;

public class FormulaEvalResource extends AbstractResource<FormulaEvaluator> {

    /**
     * Flag to ignore missing references or not.
     */
    private final boolean ignoreMissingRefs;

    /**
     * Set of String paths to references.
     */
    protected Set<String> myRefPaths;

    /**
     * Formula Evaluator object.
     */
    private FormulaEvaluator evaluator;

    /**
     * Constructor.
     *
     * @param path Path to resource
     * @param refsSup References Supplier object
     * @param checkThreshold long check threshold value
     * @param ignoreMissingRefs Flag to ignore missing references or not.
     */
    public FormulaEvalResource(@Nonnull final Path path,
                               @Nonnull final RefsSupplier refsSup,
                               final long checkThreshold,
                               final boolean ignoreMissingRefs) {
        super(path, refsSup, checkThreshold);
        this.ignoreMissingRefs = ignoreMissingRefs;
    }

    /**
     * Close resource.
     *
     * @throws IOException in case IO errors occurred.
     */
    @Override
    public void close() throws IOException {
        super.close();
        this.myRefPaths = null;
        this.evaluator = null;
    }

    /**
     * Before-collaboration-change handler.
     *
     * @param path Path to DataSetList file
     * @param file File object
     * @throws Exception in case IO or parsing errors occurred.
     */
    @Override
    protected void beforeCollaboration(@Nonnull final Path path, @Nonnull final File file) throws Exception {
        Workbook wb = ResourceUtils.doWorkBook(file);
        evaluator = wb.getCreationHelper().createFormulaEvaluator();
        evaluator.setIgnoreMissingWorkbooks(ignoreMissingRefs);
        myRefPaths = ResourceUtils.getRefs(wb);
    }

    /**
     * Get myRefsPaths.
     *
     * @return Set of String paths.
     */
    @Nullable
    @Override
    protected Set<String> getMyRefsPaths() {
        return myRefPaths;
    }

    /**
     * Get eval.
     *
     * @return FormulaEvaluator object.
     */
    @Nullable
    @Override
    public FormulaEvaluator getEval() {
        return evaluator;
    }

    /**
     * Get resource.
     *
     * @return FormulaEvaluator object.
     */
    @Nullable
    @Override
    protected FormulaEvaluator getRes() {
        return getEval();
    }

}
