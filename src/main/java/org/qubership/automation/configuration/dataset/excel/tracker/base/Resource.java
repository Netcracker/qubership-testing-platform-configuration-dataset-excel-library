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
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

public interface Resource<T> {

    /**
     * Get myRefs.
     *
     * @return Set of Resource objects.
     */
    @Nonnull
    Set<? extends Resource> getMyRefs();

    /**
     * Get all references tree.
     *
     * @return Iterator of Resource objects.
     */
    @Nonnull
    Iterator<? extends Resource> getAllRefsTree();

    /**
     * Get references to the resource.
     *
     * @return Set of Resource objects.
     */
    @Nonnull
    Set<? extends Resource> getRefsToMe();

    /**
     * Get all references to the resource tree.
     *
     * @return Iterator of Resource objects.
     */
    @Nonnull
    Iterator<? extends Resource> getAllRefsToMeTree();

    /**
     * Get the last exception.
     *
     * @return Optional of Exception object.
     */
    Optional<Exception> getLastException();

    /**
     * Get Resource.
     *
     * @return Optional of T-class object.
     */
    Optional<T> getResource();

    /**
     * Get path.
     *
     * @return Path object.
     */
    @Nonnull
    Path getPath();

    /**
     * Get File.
     *
     * @return File object.
     */
    @Nonnull
    File getFile();

    /**
     * Get the last update time.
     *
     * @return long datetime value.
     */
    long getLastUpdateTime();

    /**
     * Get status.
     *
     * @return ResourceStatus object.
     */
    @Nonnull
    ResourceStatus getStatus();
}
