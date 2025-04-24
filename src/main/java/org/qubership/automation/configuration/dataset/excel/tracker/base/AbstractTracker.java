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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.qubership.automation.configuration.dataset.excel.builder.DataSetBuilder;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class AbstractTracker<Param, Params, Vars> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTracker.class);
    private final LoadingCache<Path, DataSetListResource<Param, Params, Vars>> cache;
    private final long checkThreshold;
    private final ExternalRefsSupplier extRefs;
    private final Path watchDir;
    private final boolean ignoreMissingRefs;

    public AbstractTracker(Path watchDir, long checkThreshold, boolean ignoreMissingRefs) {
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

    @Nullable
    public Stream<Resource<Map<String, DSList<Param, Params, Vars>>>> getAllDataSets() {
        File[] files = watchDir.toFile().listFiles(ResourceUtils.DATASET_FILTER);
        if (files == null || files.length == 0) {
            return null;
        }
        return Arrays.stream(files).map(file -> getDataSet(file.toPath()));
    }

    @Nonnull
    public Resource<Map<String, DSList<Param, Params, Vars>>> getDataSet(@Nonnull Path path) {
        DataSetListResource<Param, Params, Vars> result = cache.getUnchecked(path);
        result.getResource();
        if (result.getStatus() != ResourceStatus.SAME) {
            LOGGER.info(String.format("[%s] DataSet [%s]", result.getStatus(), result.getPath()), result.getLastException().orElse(null));
        }
        return result;
    }

    @Nonnull
    protected abstract DSLists<Param, Params, Vars> build(@Nonnull DataSetBuilder builder);

    public void clearCaches(){
        cache.invalidateAll();
        extRefs.clearChache();
    }

}
