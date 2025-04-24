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

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.*;

public class PropertyDescriptors {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyDescriptors.class);

    private final boolean isStartDate1904;

    private PropertyDescriptors(boolean isStartDate1904) {
        this.isStartDate1904 = isStartDate1904;
    }

    @Nonnull
    public static PropertyDescriptor forCell(int cellType, @Nonnull Cell cell, boolean isStartDate1904) {
        return forCell(CellType.forInt(cellType), cell, isStartDate1904);
    }

    @Nonnull
    public static PropertyDescriptor forCell(CellType cellType, @Nonnull Cell cell, boolean isStartDate1904) {
        switch (cellType) {
            case BLANK:
            case STRING:
                return PropertyDescriptor.STRING;
            case NUMERIC:
                return isStartDate1904 ? PropertyDescriptor.NUMERIC_MAC.get() : PropertyDescriptor.NUMERIC_WIN.get();
            case BOOLEAN:
                return PropertyDescriptor.BOOLEAN;
            case ERROR:
                return PropertyDescriptor.ERROR;
            case FORMULA:
                return PropertyDescriptor.FORMULA;
            case _NONE:
            default:
                throw new IllegalArgumentException(String.format("Unknown cell [%s] eval type: [%s]", cell,
                        cell.getCellType()));
        }
    }

    @Nonnull
    public static PropertyDescriptors get(@Nonnull Workbook wb) {
        return new PropertyDescriptors(isStartDate1904(wb));
    }

    public static boolean isStartDate1904(Workbook wb) {
        Iterator<Sheet> shIter = wb.sheetIterator();
        return shIter.hasNext() && isStartDate1904(shIter.next());
    }

    /**
     * see alandix.com/code/apache-poi-detect-1904-date-option
     */
    public static boolean isStartDate1904(Sheet sheet) {
        try {
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell cell = row.createCell(0);
            cell.setCellValue(0.0);
            boolean is1904StartDate = isStartDate1904(cell);
            sheet.removeRow(row);
            return is1904StartDate;
        } catch (Exception e) {
            LOGGER.warn("Can not check 1900/1904 start date using sheet [" + sheet + "]", e);
            return false;
        }
    }

    /**
     * throws an exception for non-numeric cells
     */
    private static boolean isStartDate1904(Cell cell) {
        double value = cell.getNumericCellValue();
        Date date = cell.getDateCellValue();
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        long yearSince1900 = cal.get(Calendar.YEAR) - 1900;
        long yearEstimated1900 = Math.round(value / (365.25));
        return yearSince1900 > yearEstimated1900;
    }

    @Nonnull
    public Change convertReplaceValue(@Nonnull final Cell cell,
                                      final @Nullable String newValue) {
        final PropertyDescriptor descriptor = forCell(cell);
        final Object origValue = descriptor.getValue(cell);
        return new Change(cell) {
            @Override
            public void apply() {
                descriptor.setFromString(getTarget(), newValue);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void revert() {
                descriptor.setValue(getTarget(), origValue);
            }

            @Override
            public String toString() {
                return String.format("Replace value change from [%s] to [%s] for [%s]", origValue, newValue,
                        cell.getAddress());
            }
        };
    }

    @Nonnull
    public Change replaceValue(@Nonnull final Cell cell, @Nonnull Object newValue) {
        final PropertyDescriptor descriptor = forCell(cell);
        final Object newValueTypeSafe = descriptor.getValueType().cast(newValue);
        final Object origValue = descriptor.getValue(cell);

        return new Change(cell) {
            @SuppressWarnings("unchecked")
            @Override
            public void apply() {
                descriptor.setValue(getTarget(), newValueTypeSafe);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void revert() {
                descriptor.setValue(getTarget(), origValue);
            }

            @Override
            public String toString() {
                return String.format("Replace value change from [%s] to [%s] for [%s]", origValue, newValueTypeSafe,
                        cell.getAddress());
            }
        };
    }

    @Nonnull
    public PropertyDescriptor forCell(int cellType, @Nonnull Cell cell) {
        return forCell(CellType.forInt(cellType), cell);
    }

    @Nonnull
    public PropertyDescriptor forCell(CellType cellType, @Nonnull Cell cell) {
        return forCell(cellType, cell, isStartDate1904);
    }

    public double dateToDouble(@Nonnull Date date) {
        return DateUtil.getExcelDate(date, isStartDate1904);
    }

    public double dateToDouble(@Nonnull LocalDateTime date) {
        return DateUtil.getExcelDate(date, isStartDate1904);
    }

    @Nonnull
    public PropertyDescriptor forCell(@Nonnull Cell cell) {
        return forCell(cell.getCellType(), cell, isStartDate1904);
    }

    public boolean isStartDate1904() {
        return isStartDate1904;
    }
}
