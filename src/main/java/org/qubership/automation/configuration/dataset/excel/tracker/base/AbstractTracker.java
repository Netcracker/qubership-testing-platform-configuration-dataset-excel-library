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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.configuration.dataset.excel.builder.DataSetBuilder;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public abstract class AbstractTracker<Param, Params, Vars> {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTracker.class);

    /**
     * Cache of DatasetLists.
     */
    private final LoadingCache<Path, DataSetListResource<Param, Params, Vars>> cache;

    /**
     * Check Threshold value.
     */
    private final long checkThreshold;

    /**
     * Supplier of External References.
     */
    private final ExternalRefsSupplier extRefs;

    /**
     * Path to directory to track.
     */
    private final Path watchDir;

    /**
     * Flag ignore missing references or not.
     */
    private final boolean ignoreMissingRefs;

    /**
     * Constructor.
     *
     * @param watchDir Path to directory to track
     * @param checkThreshold Check Threshold value
     * @param ignoreMissingRefs Flag ignore missing references or not.
     */
    public AbstractTracker(final Path watchDir, final long checkThreshold, final boolean ignoreMissingRefs) {
        this.watchDir = watchDir;
        this.checkThreshold = checkThreshold;
        this.extRefs = new ExternalRefsSupplier(checkThreshold, ignoreMissingRefs);
        this.ignoreMissingRefs = ignoreMissingRefs;
        cache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .weakValues().build(new CacheLoader<Path, DataSetListResource<Param, Params, Vars>>() {
            @Override
            public DataSetListResource<Param, Params, Vars> load(@Nonnull Path key) throws Exception {
                return new DataSetListResource<>(key,
                        AbstractTracker.this.extRefs,
                        AbstractTracker.this.checkThreshold,
                        AbstractTracker.this::build,
                        AbstractTracker.this.ignoreMissingRefs);
            }
        });
    }

    /**
     * Get all datasets.
     *
     * @return Stream of Resources.
     */
    @Nullable
    public Stream<Resource<Map<String, DSList<Param, Params, Vars>>>> getAllDataSets() {
        File[] files = watchDir.toFile().listFiles(ResourceUtils.DATASET_FILTER);
        if (files == null || files.length == 0) {
            return null;
        }
        return Arrays.stream(files).map(file -> getDataSet(file.toPath()));
    }

    /**
     * Get dataset by path.
     *
     * @param path Path of dataset
     * @return Resource object.
     */
    @Nonnull
    public Resource<Map<String, DSList<Param, Params, Vars>>> getDataSet(@Nonnull final Path path) {
        DataSetListResource<Param, Params, Vars> result = cache.getUnchecked(path);
        result.getResource();
        if (result.getStatus() != ResourceStatus.SAME) {
            LOGGER.info("[{}] DataSet [{}]", result.getStatus(), result.getPath(),
                    result.getLastException().orElse(null));
        }
        return result;
    }

    /**
     * Build dataset lists.
     *
     * @param builder DataSetBuilder object
     * @return DSLists object.
     */
    @Nonnull
    protected abstract DSLists<Param, Params, Vars> build(@Nonnull DataSetBuilder builder);

    /**
     * Clear caches.
     */
    public void clearCaches() {
        cache.invalidateAll();
        extRefs.clearCache();
    }

}
