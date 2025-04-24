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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.qubership.automation.configuration.dataset.excel.builder.DataSetBuilder;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class DataSetListResource<Param, Params, Vars> extends AbstractResource<Map<String, DSList<Param, Params, Vars>>> {

    protected final Function<DataSetBuilder, DSLists<Param, Params, Vars>> builder;
    private final boolean ignoreMissingRefs;
    protected DSLists<Param, Params, Vars> ds;
    private FormulaEvaluator eval;
    private Iterator<DSList<Param, Params, Vars>> listsIter;
    private Map<String, DSList<Param, Params, Vars>> resource;
    private Set<String> refsPath;

    public DataSetListResource(@Nonnull Path path, @Nonnull RefsSupplier refsSup, long checkThreshold,
                               @Nonnull Function<DataSetBuilder, DSLists<Param, Params, Vars>> builder,
                               boolean ignoreMissingRefs) {
        super(path, refsSup, checkThreshold);
        this.builder = builder;
        this.ignoreMissingRefs = ignoreMissingRefs;
    }

    @Override
    protected void beforeCollaboration(@Nonnull Path path, @Nonnull File file) throws Exception {
        Workbook wb = ResourceUtils.doWorkBook(file);
        refsPath = ResourceUtils.getRefs(wb);
        ds = builder.apply(DataSetBuilder.create(new Supplier<Workbook>() {
            @Override
            public Workbook get() {
                return wb;
            }

            @Override
            public String toString() {
                return path.toString();
            }
        }));
        Preconditions.checkNotNull(ds);
    }

    @Override
    protected void onAnyRefUpdate() {
        if (ds != null) {
            listsIter = ds.iterator();
            eval = ds.getEvaluationContext().evaluator();
            eval.setIgnoreMissingWorkbooks(ignoreMissingRefs);
            resource = null;
        }
    }

    /**
     * ds can not be null here because {@link #beforeCollaboration(Path, File)} done without errors.
     * listsIter can not be null here because {@link #onAnyRefUpdate()} should be invoked.
     */
    @Override
    protected void afterCollaboration(@Nonnull Path path, @Nonnull File file) throws Exception {
        //ds can not be null here because beforeCollaboration done without errors
        //listsIter can not be null here because onAnyRefUpdate should be invoked
        if (resource == null) {
            Map<String, DSList<Param, Params, Vars>> result = Maps.newHashMap();
            while (listsIter.hasNext()) {
                DSList<Param, Params, Vars> list = listsIter.next();
                result.put(list.getName(), list);
            }
            resource = result;
        }
    }

    @Nullable
    @Override
    protected Set<String> getMyRefsPaths() {
        return refsPath;
    }

    @Nullable
    @Override
    public FormulaEvaluator getEval() {
        return eval;
    }

    @Nullable
    @Override
    protected Map<String, DSList<Param, Params, Vars>> getRes() {
        return resource;
    }

    @Override
    public void close() throws IOException {
        super.close();
        ds = null;
        eval = null;
        resource = null;
        refsPath = null;
        listsIter = null;
    }

}
