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

import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public abstract class ResourceState<T> implements Resource<T>, Closeable {
    protected final Path path;
    protected final File file;

    protected boolean exists;
    protected long lastModified;
    protected boolean directory;
    protected long length;

    protected long lastRefreshed = -1L;
    protected long lastUpdated = -1L;

    protected Exception lastException;


    public ResourceState(@Nonnull Path path) {
        this.path = path;
        this.file = path.toFile();
    }

    protected ResourceStatus beforeCollaborationUpdate(long checkThreshold) {
        long curTime = System.currentTimeMillis();
        if ((curTime - lastRefreshed) < checkThreshold)
            return ResourceStatus.SAME;
        boolean origExists = exists;
        if (!_refresh())
            return ResourceStatus.SAME;
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

    protected boolean _refresh() {
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
        return exists != origExists ||
                lastModified != origLastModified ||
                directory != origDirectory ||
                length != origLength;
    }

    protected void beforeCollaboration(@Nonnull Path path, @Nonnull File file) throws Exception {

    }

    protected void afterCollaboration(@Nonnull Path path, @Nonnull File file) throws Exception {

    }

    @Override
    public Optional<Exception> getLastException() {
        return Optional.ofNullable(lastException);
    }

    @Nonnull
    @Override
    public Path getPath() {
        return path;
    }

    @Nonnull
    public File getFile() {
        return file;
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdated;
    }

    @Override
    public void close() throws IOException {
        lastException = null;
        lastUpdated = -1L;
    }
}
