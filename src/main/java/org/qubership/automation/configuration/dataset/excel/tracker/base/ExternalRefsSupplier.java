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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ExternalRefsSupplier implements RefsSupplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalRefsSupplier.class);
    private final Cache<Path, FormulaEvalResource> existingRefs;
    private final long checkThreshold;
    private final boolean ignoreMissingRefs;

    public ExternalRefsSupplier(long checkThreshold, boolean ignoreMissingRefs) {
        this.checkThreshold = checkThreshold;
        this.ignoreMissingRefs = ignoreMissingRefs;
        existingRefs = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).weakValues().build();
    }

    @Nonnull
    @Override
    public AbstractResource<FormulaEvaluator> getRef(@Nonnull Resource dependent, @Nonnull String path) throws Exception {
        Path actual = dependent.getPath().getParent().resolve(Paths.get(path));
        FormulaEvalResource result;
        try {
            result = existingRefs.get(actual, () -> {
                FormulaEvalResource resource;
                resource = new FormulaEvalResource(actual, ExternalRefsSupplier.this, checkThreshold, ignoreMissingRefs);
                resource.beforeCollaborationUpdate();
                return resource;
            });
        } catch (ExecutionException e) {
            throw new Exception(String.format("Can not resolve reference for [%s] using path [%s]", dependent, actual));
        }
        return result;
    }

    @Override
    public synchronized void setupCollaboratingEnv(@Nonnull AbstractResource<?> resource) throws Exception {
        Iterator<? extends AbstractResource> qwe = resource.getCollaboration();
        Map<String, FormulaEvaluator> actualEnv = Maps.newHashMap();
        while (qwe.hasNext()) {
            AbstractResource<?> res = qwe.next();
            FormulaEvaluator eval = res.getEval();
            String actualPath = res.getActualPath(ignoreMissingRefs);
            if (eval == null) {
                String messaqge = String.format("Can not initialize %s because it's ref %s on path [%s] is broken"
                        , resource
                        , res
                        , actualPath);
                Exception cause = res.getLastException().orElse(null);
                if (ignoreMissingRefs) {
                    LOGGER.warn(messaqge, cause);
                } else {
                    throw new Exception(messaqge, cause);
                }
                continue;
            }
            actualEnv.put(actualPath, eval);
        }
        if (actualEnv.size() == 1)
            return;
        CollaboratingWorkbooksEnvironment.setupFormulaEvaluator(actualEnv);

    }
    public void clearChache(){
        existingRefs.invalidateAll();
    }
}
