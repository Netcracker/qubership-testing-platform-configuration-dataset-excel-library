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

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyDescriptors {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyDescriptors.class);

    /**
     * Flag if Date starting point is 1904.
     */
    private final boolean isStartDate1904;

    private PropertyDescriptors(final boolean isStartDate1904) {
        this.isStartDate1904 = isStartDate1904;
    }

    /**
     * Make Property Descriptor for cell.
     *
     * @param cellType int type of cell
     * @param cell Cell object
     * @param isStartDate1904 Flag if Date starting point is 1904
     * @return PropertyDescriptor object.
     */
    @Nonnull
    public static PropertyDescriptor forCell(final int cellType,
                                             @Nonnull final Cell cell,
                                             final boolean isStartDate1904) {
        return forCell(CellType.forInt(cellType), cell, isStartDate1904);
    }

    /**
     * Make Property Descriptor for cell.
     * If cellType is unknown, it throws IllegalArgumentException.
     *
     * @param cellType CellType type of cell
     * @param cell Cell object
     * @param isStartDate1904 Flag if Date starting point is 1904
     * @return PropertyDescriptor object.
     */
    @Nonnull
    public static PropertyDescriptor forCell(final CellType cellType,
                                             @Nonnull final Cell cell,
                                             final boolean isStartDate1904) {
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

    /**
     * Get PropertyDescriptors for wb Workbook given.
     * Result depends on isStartDate1904 of the workbook.
     *
     * @param wb Workbook object
     * @return a new PropertyDescriptors object.
     */
    @Nonnull
    public static PropertyDescriptors get(@Nonnull final Workbook wb) {
        return new PropertyDescriptors(isStartDate1904(wb));
    }

    /**
     * Get isStartDate1904 flag of wb Workbook given.
     *
     * @param wb Workbook object
     * @return boolean isStartDate1904 flag of the 1st Sheet of wb Workbook.
     * If there is no Sheets return false.
     */
    public static boolean isStartDate1904(final Workbook wb) {
        Iterator<Sheet> shIter = wb.sheetIterator();
        return shIter.hasNext() && isStartDate1904(shIter.next());
    }

    /**
     * Check isStartDate1904 of the sheet given.
     * It's performed via creating row, then creating cell in it, then setting 0.0 as cell value,
     * then checking isStartDate1904 flag of the cell. Then row is removed.
     * See alandix.com/code/apache-poi-detect-1904-date-option.
     *
     * @param sheet Sheet object
     * @return boolean isStartDate1904 flag; in case any exceptions return false.
     */
    public static boolean isStartDate1904(final Sheet sheet) {
        try {
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell cell = row.createCell(0);
            cell.setCellValue(0.0);
            boolean is1904StartDate = isStartDate1904(cell);
            sheet.removeRow(row);
            return is1904StartDate;
        } catch (Exception e) {
            LOGGER.warn("Can not check 1900/1904 start date using sheet [{}]", sheet, e);
            return false;
        }
    }

    /**
     * Check isStartDate1904 flag of the cell given.
     * Throws an exception for non-numeric cells.
     *
     * @param cell Cell object
     * @return boolean value.
     */
    private static boolean isStartDate1904(final Cell cell) {
        double value = cell.getNumericCellValue();
        Date date = cell.getDateCellValue();
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        long yearSince1900 = cal.get(Calendar.YEAR) - 1900;
        long yearEstimated1900 = Math.round(value / (365.25));
        return yearSince1900 > yearEstimated1900;
    }

    /**
     * Create a Change object to convert String newValue and set cell value to it.
     * The Change includes both apply() and revert() methods.
     *
     * @param cell Cell object
     * @param newValue String new value to convert and set
     * @return new Change object.
     */
    @Nonnull
    public Change convertReplaceValue(@Nonnull final Cell cell,
                                      @Nullable final String newValue) {
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

    /**
     * Create a Change object to replace cell value to Object newValue.
     * The Change includes both apply() and revert() methods.
     *
     * @param cell Cell object
     * @param newValue Object new value to set
     * @return new Change object.
     */
    @Nonnull
    public Change replaceValue(@Nonnull final Cell cell, @Nonnull final Object newValue) {
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

    /**
     * Make PropertyDescriptor for int cellType and cell given.
     *
     * @param cellType int type of cell
     * @param cell Cell object
     * @return PropertyDescriptor object.
     */
    @Nonnull
    public PropertyDescriptor forCell(final int cellType, @Nonnull final Cell cell) {
        return forCell(CellType.forInt(cellType), cell);
    }

    /**
     * Make PropertyDescriptor for CellType type of cell and cell given.
     *
     * @param cellType CellType type of cell
     * @param cell Cell object
     * @return PropertyDescriptor object.
     */
    @Nonnull
    public PropertyDescriptor forCell(final CellType cellType, @Nonnull final Cell cell) {
        return forCell(cellType, cell, isStartDate1904);
    }

    /**
     * Get double value of Date object given, using isStartDate1904 flag.
     *
     * @param date Date object
     * @return double value.
     */
    public double dateToDouble(@Nonnull final Date date) {
        return DateUtil.getExcelDate(date, isStartDate1904);
    }

    /**
     * Get double value of LocalDateTime object given, using isStartDate1904 flag.
     *
     * @param date LocalDateTime object
     * @return double value.
     */
    public double dateToDouble(@Nonnull final LocalDateTime date) {
        return DateUtil.getExcelDate(date, isStartDate1904);
    }

    /**
     * Get PropertyDescriptor for the cell given.
     *
     * @param cell Cell object
     * @return PropertyDescriptor object.
     */
    @Nonnull
    public PropertyDescriptor forCell(@Nonnull final Cell cell) {
        return forCell(cell.getCellType(), cell, isStartDate1904);
    }

    /**
     * Check if Date starting point is 1904.
     *
     * @return true/false value of flag if Date starting point is 1904.
     */
    public boolean isStartDate1904() {
        return isStartDate1904;
    }
}
