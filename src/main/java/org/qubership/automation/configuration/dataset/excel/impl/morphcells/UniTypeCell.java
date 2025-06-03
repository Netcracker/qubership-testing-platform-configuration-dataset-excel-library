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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;

import com.google.common.base.Strings;

/**
 * To be able to manipulate cells with any type with unified API.
 */
public class UniTypeCell<T> {

    /**
     * PropertyDescriptor object.
     */
    private final PropertyDescriptor<T> descriptor;

    /**
     * Cell object.
     */
    private final Cell cell;

    /**
     * Constructor.
     *
     * @param descriptor PropertyDescriptor object
     * @param cell Cell object.
     */
    public UniTypeCell(@Nonnull final PropertyDescriptor<T> descriptor, @Nonnull final Cell cell) {
        this.descriptor = descriptor;
        this.cell = cell;
    }

    /**
     * Get cell type as int.
     *
     * @return int type.
     */
    public int cellType() {
        return descriptor.cellType();
    }

    /**
     * Make T-class value of cell from str given.
     *
     * @param str String value
     * @return T-class value.
     */
    @Nonnull
    public T fromString(@Nonnull final String str) {
        return descriptor.fromString(str, cell);
    }

    /**
     * Convert T-class value given to String.
     *
     * @param value T-class value to process
     * @return String representation.
     */
    @Nonnull
    public String toString(@Nonnull final T value) {
        return descriptor.toString(value, cell);
    }

    /**
     * Get cell value.
     *
     * @return T-class value
     */
    @Nonnull
    public T getValue() {
        return descriptor.getValue(cell);
    }

    /**
     * Set cell value.
     *
     * @param value T-class value to set
     */
    public void setValue(@Nonnull final T value) {
        descriptor.setValue(cell, value);
    }

    /**
     * Set cell value from String.
     *
     * @param value String value to set.
     */
    public void setFromString(@Nullable final String value) {
        descriptor.setFromString(cell, value);
    }

    /**
     * Get to-String representation of the cell given.
     *
     * @return String representation of the cell value.
     */
    @Nullable
    public String getToString() {
        return descriptor.getToString(cell);
    }

    /**
     * Get Class of value type.
     *
     * @return T-Class.
     */
    @Nonnull
    public Class<T> getValueType() {
        return descriptor.getValueType();
    }

    /**
     * Make String representation of the object. Nulls are displayed as empty objects.
     *
     * @return String representation of the object.
     */
    public String toString() {
        return Strings.nullToEmpty(getToString());
    }
}
