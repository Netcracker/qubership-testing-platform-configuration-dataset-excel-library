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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.qubership.automation.configuration.dataset.excel.builder.DataSetBuilder;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class DataSetListResource<Param, Params, Vars> extends AbstractResource<Map<String, DSList<Param, Params, Vars>>> {

    /**
     * Function to build DataSetList.
     */
    protected final Function<DataSetBuilder, DSLists<Param, Params, Vars>> builder;

    /**
     * Flag to ignore missing references or not.
     */
    private final boolean ignoreMissingRefs;

    /**
     * DataSet Lists object.
     */
    protected DSLists<Param, Params, Vars> ds;

    /**
     * Formula Evaluator object.
     */
    private FormulaEvaluator eval;

    /**
     * DataSet List iterator.
     */
    private Iterator<DSList<Param, Params, Vars>> listsIter;

    /**
     * Resources map.
     */
    private Map<String, DSList<Param, Params, Vars>> resource;

    /**
     * Set of String references' paths.
     */
    private Set<String> refsPath;

    /**
     * Constructor.
     *
     * @param path Path to DataSetList file
     * @param refsSup References Supplier object
     * @param checkThreshold long threshold value
     * @param builder DataSetList builder object
     * @param ignoreMissingRefs flag to ignore missing references or not.
     */
    public DataSetListResource(@Nonnull final Path path,
                               @Nonnull final RefsSupplier refsSup,
                               final long checkThreshold,
                               @Nonnull final Function<DataSetBuilder, DSLists<Param, Params, Vars>> builder,
                               final boolean ignoreMissingRefs) {
        super(path, refsSup, checkThreshold);
        this.builder = builder;
        this.ignoreMissingRefs = ignoreMissingRefs;
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

    /**
     * On-Any-Reference-Update Handler.
     */
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
     * After-collaboration-change handler.
     * ds can not be null here because {@link #beforeCollaboration(Path, File)} done without errors.
     * listsIter can not be null here because {@link #onAnyRefUpdate()} should be invoked.
     *
     * @param path Path to DataSetList file
     * @param file File object.
     */
    @Override
    protected void afterCollaboration(@Nonnull final Path path, @Nonnull final File file) {
        // ds can not be null here because beforeCollaboration done without errors
        // listsIter can not be null here because onAnyRefUpdate should be invoked
        if (resource == null) {
            Map<String, DSList<Param, Params, Vars>> result = Maps.newHashMap();
            while (listsIter.hasNext()) {
                DSList<Param, Params, Vars> list = listsIter.next();
                result.put(list.getName(), list);
            }
            resource = result;
        }
    }

    /**
     * Get myRefs.
     *
     * @return Set of String refs.
     */
    @Nullable
    @Override
    protected Set<String> getMyRefsPaths() {
        return refsPath;
    }

    /**
     * Get eval.
     *
     * @return FormulaEvaluator object.
     */
    @Nullable
    @Override
    public FormulaEvaluator getEval() {
        return eval;
    }

    /**
     * Get resource.
     *
     * @return Map of DataSetList Parameters.
     */
    @Nullable
    @Override
    protected Map<String, DSList<Param, Params, Vars>> getRes() {
        return resource;
    }

    /**
     * Close resource.
     *
     * @throws IOException in case IO errors occurred.
     */
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
