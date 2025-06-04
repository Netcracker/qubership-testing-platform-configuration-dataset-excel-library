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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * only CREATED or UPDATED statuses became the SAME after the next update.
 */
public enum ResourceStatus {

    /**
     * Constant for 'Created' resource status.
     */
    CREATED(true, true),

    /**
     * Constant for 'Updated' resource status.
     */
    UPDATED(true, true),

    /**
     * Constant for 'Deleted' resource status.
     */
    DELETED(true, false),

    /**
     * Constant for 'Failed to create' resource status.
     */
    FAILED_TO_CREATE(false, false),

    /**
     * Constant for 'Failed to update' resource status.
     */
    FAILED_TO_UPDATE(false, true),

    /**
     * Constant for 'The same' resource status.
     */
    SAME(false, true);

    /**
     * Flag if resource was updated or not.
     */
    private final boolean resourceWasUpdated;

    /**
     * Flag 'have resource' or not.
     */
    private final boolean haveResource;

    /**
     * Constructor.
     *
     * @param resourceWasUpdated flag resource was updated or not
     * @param haveResource flag 'have resource' or not.
     */
    ResourceStatus(final boolean resourceWasUpdated, final boolean haveResource) {
        this.resourceWasUpdated = resourceWasUpdated;
        this.haveResource = haveResource;
    }

    /**
     * Fail status.
     *
     * @param status ResourceStatus status object
     * @return ResourceStatus failed status.
     */
    @Nonnull
    public static ResourceStatus fail(@Nonnull final ResourceStatus status) {
        switch (status) {
            case CREATED:
                return FAILED_TO_CREATE;
            case UPDATED:
                return FAILED_TO_UPDATE;
            default:
                return status;
        }
    }

    /**
     * Merge old and new statuses.
     *
     * @param oldRS ResourceStatus old status
     * @param newRS ResourceStatus new status
     * @return ResourceStatus merged status.
     */
    @Nonnull
    public static ResourceStatus merge(@Nullable final ResourceStatus oldRS, @Nonnull final ResourceStatus newRS) {
        if (oldRS == null) {
            return newRS;
        }
        if (newRS == ResourceStatus.SAME) {
            // Should not override negative statuses
            return oldRS.resourceWasUpdated() && oldRS.haveResource() ? SAME : oldRS;
        }
        return newRS;
    }

    /**
     * Get resourceWasUpdated flag.
     *
     * @return boolean.
     */
    public boolean resourceWasUpdated() {
        return resourceWasUpdated;
    }

    /**
     * Get haveResource flag.
     *
     * @return boolean.
     */
    public boolean haveResource() {
        return haveResource;
    }

    /**
     * Fail status.
     *
     * @return ResourceStatus object.
     */
    public ResourceStatus failed() {
        return fail(this);
    }

    /**
     * Merge resource status with a new one.
     *
     * @param newStatus ResourceStatus new status
     * @return ResourceStatus merged result.
     */
    public ResourceStatus merge(@Nullable final ResourceStatus newStatus) {
        return newStatus == null || this == newStatus ? this : merge(this, newStatus);
    }
}
