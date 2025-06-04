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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.util.LocaleUtil;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;

public abstract class PropertyDescriptor<T> {

    /**
     * Data Formatter for Locale.US.
     */
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.US);

    /**
     * Date Formatter for "yyyy-MM-dd HH:mm:ss" pattern and Locale.US.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * Numeric Supplier for Windows.
     */
    protected static Supplier<Numeric> NUMERIC_WIN = Utils.memoize(
            () -> new Numeric(false, FORMATTER, DATE_FORMAT));

    /**
     * Numeric Supplier for MacOs.
     */
    protected static Supplier<Numeric> NUMERIC_MAC = Utils.memoize(
            () -> new Numeric(true, FORMATTER, DATE_FORMAT));

    /**
     * Property Descriptor for String.
     */
    protected static PropertyDescriptor<String> STRING = new PropertyDescriptor<String>(String.class) {

        @Override
        public int cellType() {
            return CellType.STRING.getCode();
        }

        @Nonnull
        @Override
        public String fromString(@Nonnull final String str, @Nonnull final Cell cell) {
            return str;
        }

        @Nonnull
        @Override
        public String toString(@Nonnull final String value, @Nonnull final Cell cell) {
            return value;
        }

        @Nonnull
        @Override
        public String getValue(@Nonnull final Cell cell) {
            return cell.getStringCellValue();
        }

        @Override
        public void setValue(@Nonnull final Cell cell, @Nonnull final String value) {
            cell.setCellValue(value);
        }
    };

    /**
     * Property Descriptor for Formula.
     */
    protected static PropertyDescriptor<String> FORMULA = new PropertyDescriptor<String>(String.class) {

        @Override
        public int cellType() {
            return CellType.FORMULA.getCode();
        }

        @Nonnull
        @Override
        public String fromString(@Nonnull final String str, @Nonnull final Cell cell) {
            return str;
        }

        @Nonnull
        @Override
        public String toString(@Nonnull final String value, @Nonnull final Cell cell) {
            return value;
        }

        @Nonnull
        @Override
        public String getValue(@Nonnull final Cell cell) {
            return cell.getCellFormula();
        }

        @Override
        public void setValue(@Nonnull final Cell cell, @Nonnull final String value) {
            cell.setCellFormula(value);
        }
    };

    /**
     * Property Descriptor for Boolean.
     */
    protected static PropertyDescriptor<Boolean> BOOLEAN = new PropertyDescriptor<Boolean>(Boolean.class) {

        @Override
        public int cellType() {
            return CellType.BOOLEAN.getCode();
        }

        @Nonnull
        @Override
        public Boolean fromString(@Nonnull final String str, @Nonnull final Cell cell) {
            return Boolean.valueOf(str);
        }

        @Nonnull
        @Override
        public String toString(@Nonnull final Boolean value, @Nonnull final Cell cell) {
            return value.toString();
        }

        @Nonnull
        @Override
        public Boolean getValue(@Nonnull final Cell cell) {
            return cell.getBooleanCellValue();
        }

        @Override
        public void setValue(@Nonnull final Cell cell, @Nonnull final Boolean value) {
            cell.setCellValue(value);
        }
    };

    /**
     * Property Descriptor for Errors.
     */
    protected static PropertyDescriptor<Byte> ERROR = new PropertyDescriptor<Byte>(Byte.class) {

        @Override
        public int cellType() {
            return CellType.ERROR.getCode();
        }

        @Nonnull
        @Override
        public Byte fromString(@Nonnull final String str, @Nonnull final Cell cell) {
            return FormulaError.forString(str).getCode();
        }

        @Nonnull
        @Override
        public String toString(@Nonnull final Byte value, @Nonnull final Cell cell) {
            return FormulaError.forInt(value).getString();
        }

        @Nonnull
        @Override
        public Byte getValue(@Nonnull final Cell cell) {
            return cell.getErrorCellValue();
        }

        @Override
        public void setValue(@Nonnull final Cell cell, @Nonnull final Byte value) {
            cell.setCellErrorValue(value);
        }
    };

    /**
     * Class of the object.
     */
    private final Class<T> type;

    /**
     * Constructor.
     *
     * @param type Class of the object.
     */
    public PropertyDescriptor(@Nonnull final Class<T> type) {
        //only for extending
        this.type = type;
    }

    /**
     * Get cell type.
     *
     * @return int cell type value.
     */
    public abstract int cellType();

    /**
     * Make Cell value from String.
     *
     * @param str String value
     * @param cell Cell object
     * @return T class value of the cell.
     */
    @Nonnull
    public abstract T fromString(@Nonnull String str, @Nonnull Cell cell);

    /**
     * Make String representation of the value,
     * see {@link DataFormatter#formatCellValue(org.apache.poi.ss.usermodel.Cell,
     * org.apache.poi.ss.usermodel.FormulaEvaluator)}.
     *
     * @param value T-class value
     * @param cell Cell object
     * @return String representation.
     */
    @Nonnull
    public abstract String toString(@Nonnull T value, @Nonnull Cell cell);

    /**
     * Get value.
     *
     * @param cell Cell object
     * @return T-class value.
     */
    @Nonnull
    public abstract T getValue(@Nonnull Cell cell);

    /**
     * Set value.
     *
     * @param cell Cell object
     * @param value T-class value to set.
     */
    public abstract void setValue(@Nonnull Cell cell, @Nonnull T value);

    /**
     * Set cell value from String.
     *
     * @param cell Cell object
     * @param value String value to set.
     */
    public void setFromString(@Nonnull final Cell cell, @Nullable final String value) {
        if (value == null) {
            cell.setCellValue((String) null);
        } else {
            setValue(cell, fromString(value, cell));
        }
    }

    /**
     * Get String representation of the cell value.
     *
     * @param cell Cell object
     * @return String result.
     */
    @Nullable
    public String getToString(@Nonnull final Cell cell) {
        return cell.getCellType() == CellType.BLANK ? null : toString(getValue(cell), cell);
    }

    /**
     * Get value type.
     *
     * @return Class this.type.
     */
    @Nonnull
    public Class<T> getValueType() {
        return this.type;
    }

    /**
     * Make String representation of the object.
     *
     * @return String representation of the object.
     */
    @Override
    public String toString() {
        return String.format("[%s] for CellType[%s] with [%s] value", this.getClass().getName(), cellType(), type);
    }

    /**
     * Get proxy of the cell.
     *
     * @param cell Cell object
     * @return UniTypeCell object.
     */
    @Nonnull
    public UniTypeCell<T> proxy(@Nonnull final Cell cell) {
        return new UniTypeCell<>(this, cell);
    }

    /**
     * when {@link DateUtil#isCellDateFormatted(org.apache.poi.ss.usermodel.Cell)}.
     * Date is used instead of Double because no acceptable way to get use1904Windowing for
     * {@link DataFormatter#formatRawCellContents(double, int, java.lang.String, boolean)} found.
     */
    public static class Numeric extends PropertyDescriptor<Double> {

        /**
         * Flag if Date starting point is 1904.
         */
        private final boolean isStartDate1904;

        /**
         * DataFormatter object.
         */
        private final DataFormatter formatter;

        /**
         * SimpleDateFormatter object.
         */
        private final SimpleDateFormat dformat;

        /**
         * Constructor.
         *
         * @param isStartDate1904 flag if date starts at 1904 or not
         * @param formatter DataFormatter object
         * @param dformat SimpleDateFormat object.
         */
        public Numeric(final boolean isStartDate1904,
                       @Nonnull final DataFormatter formatter,
                       @Nonnull final SimpleDateFormat dformat) {
            super(Double.class);
            this.isStartDate1904 = isStartDate1904;
            this.formatter = formatter;
            this.dformat = (SimpleDateFormat) dformat.clone(); // to be able to manipulate timezones
        }

        /**
         * Get cell type integer.
         *
         * @return int code of CellType.NUMERIC.
         */
        @Override
        public int cellType() {
            return CellType.NUMERIC.getCode();
        }

        /**
         * Make Cell value from String.
         *
         * @param str String value
         * @param cell Cell object
         * @return Double value of the cell.
         */
        @Nonnull
        @Override
        public Double fromString(@Nonnull final String str, @Nonnull final Cell cell) {
            if (DateUtil.isCellDateFormatted(cell)) {
                //str must be a date
                try {
                    dformat.setTimeZone(LocaleUtil.getUserTimeZone());
                    return DateUtil.getExcelDate(dformat.parse(str), isStartDate1904);
                } catch (ParseException e) {
                    throw new RuntimeException("Can not parse date from [" + str + "] using [" + dformat + "]", e);
                }
            } else {
                //str must be a double
                return Double.valueOf(str);
            }

        }

        /**
         * Make String representation of the value.
         *
         * @param value Double value
         * @param cell Cell object
         * @return String representation.
         */
        @Nonnull
        @Override
        public String toString(@Nonnull final Double value, @Nonnull final Cell cell) {
            CellStyle style = cell.getCellStyle();
            short df = style.getDataFormat();
            String dfs = style.getDataFormatString();
            return formatter.formatRawCellContents(value, df, dfs, isStartDate1904);
        }

        /**
         * Get value.
         *
         * @param cell Cell object
         * @return Double value.
         */
        @Nonnull
        @Override
        public Double getValue(@Nonnull final Cell cell) {
            return cell.getNumericCellValue();
        }

        /**
         * Set value.
         *
         * @param cell Cell object
         * @param value Double value.
         */
        @Override
        public void setValue(@Nonnull final Cell cell, @Nonnull final Double value) {
            cell.setCellValue(value);
        }
    }

}
