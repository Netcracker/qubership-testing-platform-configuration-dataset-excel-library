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
 * only CREATED or UPDATED statuses became the SAME after the next update
 */
public enum ResourceStatus {
    CREATED(true, true),
    UPDATED(true, true),
    DELETED(true, false),
    FAILED_TO_CREATE(false, false),
    FAILED_TO_UPDATE(false, true),
    SAME(false, true);

    private final boolean resourceWasUpdated;
    private final boolean haveResource;

    ResourceStatus(boolean resourceWasUpdated,
                   boolean haveResource) {
        this.resourceWasUpdated = resourceWasUpdated;
        this.haveResource = haveResource;
    }

    @Nonnull
    public static ResourceStatus fail(@Nonnull ResourceStatus status) {
        switch (status) {
            case CREATED:
                return FAILED_TO_CREATE;
            case UPDATED:
                return FAILED_TO_UPDATE;
            default:
                return status;
        }
    }

    @Nonnull
    public static ResourceStatus merge(@Nullable ResourceStatus oldRS, @Nonnull ResourceStatus newRS) {
        if (oldRS == null) {
            return newRS;
        }

        switch (newRS) {
            case SAME:
                //should not override negative statuses
                if (oldRS.resourceWasUpdated() && oldRS.haveResource()) {
                    return SAME;
                } else {
                    return oldRS;
                }
            default:
                return newRS;
        }
    }

    public boolean resourceWasUpdated() {
        return resourceWasUpdated;
    }

    public boolean haveResource() {
        return haveResource;
    }

    public ResourceStatus failed() {
        return fail(this);
    }

    public ResourceStatus merge(@Nullable ResourceStatus newStatus) {
        if (newStatus == null || this == newStatus)
            return this;
        return merge(this, newStatus);
    }
}
