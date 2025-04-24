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

package org.qubership.automation.configuration.dataset.excel.impl.morphcells;

import com.google.common.base.Strings;
import org.apache.poi.ss.usermodel.Cell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * To be able to manipulate cells with any type with unified API
 */
public class UniTypeCell<T> {
    private final PropertyDescriptor<T> descriptor;
    private final Cell cell;

    public UniTypeCell(@Nonnull PropertyDescriptor<T> descriptor, @Nonnull Cell cell) {
        this.descriptor = descriptor;
        this.cell = cell;
    }

    public int cellType() {
        return descriptor.cellType();
    }

    @Nonnull
    public T fromString(@Nonnull String str) {
        return descriptor.fromString(str, cell);
    }

    @Nonnull
    public String toString(@Nonnull T value) {
        return descriptor.toString(value, cell);
    }

    @Nonnull
    public T getValue() {
        return descriptor.getValue(cell);
    }

    public void setValue(@Nonnull T value) {
        descriptor.setValue(cell, value);
    }

    public void setFromString(@Nullable String value) {
        descriptor.setFromString(cell, value);
    }

    @Nullable
    public String getToString() {
        return descriptor.getToString(cell);
    }

    @Nonnull
    public Class<T> getValueType() {
        return descriptor.getValueType();
    }

    public String toString() {
        return Strings.nullToEmpty(getToString());
    }
}
