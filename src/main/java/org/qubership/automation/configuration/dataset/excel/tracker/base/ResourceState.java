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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

public abstract class ResourceState<T> implements Resource<T>, Closeable {

    /**
     * Path to the resource.
     */
    protected final Path path;

    /**
     * File of the resource.
     */
    protected final File file;

    /**
     * Flag if the resource exists or not.
     */
    protected boolean exists;

    /**
     * Last modification time.
     */
    protected long lastModified;

    /**
     * Flag if the resource is a directory or not.
     */
    protected boolean directory;

    /**
     * Resource content length.
     */
    protected long length;

    /**
     * Last refresh time.
     */
    protected long lastRefreshed = -1L;

    /**
     * Last update time.
     */
    protected long lastUpdated = -1L;

    /**
     * Last exception.
     */
    protected Exception lastException;

    /**
     * Constructor.
     *
     * @param path Path to resource.
     */
    public ResourceState(@Nonnull final Path path) {
        this.path = path;
        this.file = path.toFile();
    }

    /**
     * Before-collaboration-update handler.
     *
     * @param checkThreshold long threshold value
     * @return ResourceStatus object.
     */
    protected ResourceStatus beforeCollaborationUpdate(final long checkThreshold) {
        long curTime = System.currentTimeMillis();
        if ((curTime - lastRefreshed) < checkThreshold) {
            return ResourceStatus.SAME;
        }
        boolean origExists = exists;
        if (!refreshResourceState()) {
            return ResourceStatus.SAME;
        }
        lastRefreshed = curTime;
        if (!exists) {
            IOUtils.closeQuietly(this);
            return ResourceStatus.DELETED;
        }
        try {
            beforeCollaboration(path, file);
            lastException = null;
            lastUpdated = curTime;
            return origExists ? ResourceStatus.UPDATED : ResourceStatus.CREATED;
        } catch (Exception e) {
            lastException = e;
            return origExists ? ResourceStatus.FAILED_TO_UPDATE : ResourceStatus.FAILED_TO_CREATE;
        }
    }

    /**
     * Refresh method.
     *
     * @return true if some of {exists, lastModified, directory, length} was changed.
     */
    protected boolean refreshResourceState() {
        // cache original values
        final boolean origExists = exists;
        final long origLastModified = lastModified;
        final boolean origDirectory = directory;
        final long origLength = length;

        // refresh the values
        exists = file.exists();
        directory = exists && file.isDirectory();
        lastModified = exists ? file.lastModified() : 0;
        length = exists && !directory ? file.length() : 0;

        // Return if there are changes
        return exists != origExists || lastModified != origLastModified
                || directory != origDirectory || length != origLength;
    }

    /**
     * Before-collaboration-update handler.
     *
     * @param path Path to resource
     * @param file File of resource
     * @throws Exception in case IO errors occurred.
     */
    protected void beforeCollaboration(@Nonnull final Path path, @Nonnull final File file) throws Exception {

    }

    /**
     * After-collaboration-update handler.
     *
     * @param path Path to resource
     * @param file File of resource.
     */
    protected void afterCollaboration(@Nonnull final Path path, @Nonnull final File file) {

    }

    /**
     * Get the last exception.
     *
     * @return Optional of Exception object.
     */
    @Override
    public Optional<Exception> getLastException() {
        return Optional.ofNullable(lastException);
    }

    /**
     * Get path.
     *
     * @return Path object.
     */
    @Nonnull
    @Override
    public Path getPath() {
        return path;
    }

    /**
     * Get File.
     *
     * @return File object.
     */
    @Nonnull
    public File getFile() {
        return file;
    }

    /**
     * Get the last update time.
     *
     * @return long value.
     */
    @Override
    public long getLastUpdateTime() {
        return lastUpdated;
    }

    /**
     * Close object.
     *
     * @throws IOException in case IO errors occurred.
     */
    @Override
    public void close() throws IOException {
        lastException = null;
        lastUpdated = -1L;
    }
}
