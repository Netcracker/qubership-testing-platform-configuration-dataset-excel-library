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

import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class FormulaEvalResource extends AbstractResource<FormulaEvaluator> {

    private final boolean ignoreMissingRefs;
    protected Set<String> myRefPaths;
    private FormulaEvaluator evaluator;

    public FormulaEvalResource(@Nonnull Path path, @Nonnull RefsSupplier refsSup, long checkThreshold, boolean ignoreMissingRefs) {
        super(path, refsSup, checkThreshold);
        this.ignoreMissingRefs = ignoreMissingRefs;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.myRefPaths = null;
        this.evaluator = null;
    }

    @Override
    protected void beforeCollaboration(@Nonnull Path path, @Nonnull File file) throws Exception {
        Workbook wb = ResourceUtils.doWorkBook(file);
        evaluator = wb.getCreationHelper().createFormulaEvaluator();
        evaluator.setIgnoreMissingWorkbooks(ignoreMissingRefs);
        myRefPaths = ResourceUtils.getRefs(wb);
    }

    @Nullable
    @Override
    protected Set<String> getMyRefsPaths() {
        return myRefPaths;
    }

    @Nullable
    @Override
    public FormulaEvaluator getEval() {
        return evaluator;
    }

    @Nullable
    @Override
    protected FormulaEvaluator getRes() {
        return getEval();
    }

}
