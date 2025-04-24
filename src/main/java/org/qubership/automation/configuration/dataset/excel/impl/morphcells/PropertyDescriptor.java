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

import org.qubership.automation.configuration.dataset.excel.impl.Utils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.LocaleUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Supplier;

public abstract class PropertyDescriptor<T> {

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.US);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    protected static Supplier<Numeric> NUMERIC_WIN = Utils.memoize(() -> new Numeric(false, FORMATTER, DATE_FORMAT));
    protected static Supplier<Numeric> NUMERIC_MAC = Utils.memoize(() -> new Numeric(true, FORMATTER, DATE_FORMAT));
    protected static PropertyDescriptor<String> STRING = new PropertyDescriptor<String>(String.class) {
        @Override
        public int cellType() {
            return CellType.STRING.getCode();
        }

        @Nonnull
        @Override
        public String fromString(@Nonnull String str, @Nonnull Cell cell) {
            return str;
        }

        @Nonnull
        @Override
        public String toString(@Nonnull String value, @Nonnull Cell cell) {
            return value;
        }

        @Nonnull
        @Override
        public String getValue(@Nonnull Cell cell) {
            return cell.getStringCellValue();
        }

        @Override
        public void setValue(@Nonnull Cell cell, @Nonnull String value) {
            cell.setCellValue(value);
        }
    };
    protected static PropertyDescriptor<String> FORMULA = new PropertyDescriptor<String>(String.class) {
        @Override
        public int cellType() {
            return CellType.FORMULA.getCode();
        }

        @Nonnull
        @Override
        public String fromString(@Nonnull String str, @Nonnull Cell cell) {
            return str;
        }

        @Nonnull
        @Override
        public String toString(@Nonnull String value, @Nonnull Cell cell) {
            return value;
        }

        @Nonnull
        @Override
        public String getValue(@Nonnull Cell cell) {
            return cell.getCellFormula();
        }

        @Override
        public void setValue(@Nonnull Cell cell, @Nonnull String value) {
            cell.setCellFormula(value);
        }
    };
    protected static PropertyDescriptor<Boolean> BOOLEAN = new PropertyDescriptor<Boolean>(Boolean.class) {
        @Override
        public int cellType() {
            return CellType.BOOLEAN.getCode();
        }

        @Nonnull
        @Override
        public Boolean fromString(@Nonnull String str, @Nonnull Cell cell) {
            return Boolean.valueOf(str);
        }

        @Nonnull
        @Override
        public String toString(@Nonnull Boolean value, @Nonnull Cell cell) {
            return value.toString();
        }

        @Nonnull
        @Override
        public Boolean getValue(@Nonnull Cell cell) {
            return cell.getBooleanCellValue();
        }

        @Override
        public void setValue(@Nonnull Cell cell, @Nonnull Boolean value) {
            cell.setCellValue(value);
        }
    };
    protected static PropertyDescriptor<Byte> ERROR = new PropertyDescriptor<Byte>(Byte.class) {
        @Override
        public int cellType() {
            return CellType.ERROR.getCode();
        }

        @Nonnull
        @Override
        public Byte fromString(@Nonnull String str, @Nonnull Cell cell) {
            return FormulaError.forString(str).getCode();
        }

        @Nonnull
        @Override
        public String toString(@Nonnull Byte value, @Nonnull Cell cell) {
            return FormulaError.forInt(value).getString();
        }

        @Nonnull
        @Override
        public Byte getValue(@Nonnull Cell cell) {
            return cell.getErrorCellValue();
        }

        @Override
        public void setValue(@Nonnull Cell cell, @Nonnull Byte value) {
            cell.setCellErrorValue(value);
        }
    };
    private final Class<T> type;

    public PropertyDescriptor(@Nonnull Class<T> type) {
        //only for extending
        this.type = type;
    }

    public abstract int cellType();

    @Nonnull
    public abstract T fromString(@Nonnull String str, @Nonnull Cell cell);

    /**
     * see {@link DataFormatter#formatCellValue(org.apache.poi.ss.usermodel.Cell, org.apache.poi.ss.usermodel.FormulaEvaluator)}
     */
    @Nonnull
    public abstract String toString(@Nonnull T value, @Nonnull Cell cell);

    @Nonnull
    public abstract T getValue(@Nonnull Cell cell);

    public abstract void setValue(@Nonnull Cell cell, @Nonnull T value);

    public void setFromString(@Nonnull Cell cell, @Nullable String value) {
        if (value == null) {
            cell.setCellValue((String) null);
        } else {
            setValue(cell, fromString(value, cell));
        }
    }

    @Nullable
    public String getToString(@Nonnull Cell cell) {
        if (cell.getCellType() == CellType.BLANK)
            return null;
        return toString(getValue(cell), cell);
    }

    @Nonnull
    public Class<T> getValueType() {
        return this.type;
    }

    @Override
    public String toString() {
        return String.format("[%s] for CellType[%s] with [%s] value", this.getClass().getName(), cellType(), type);
    }

    @Nonnull
    public UniTypeCell<T> proxy(@Nonnull Cell cell) {
        return new UniTypeCell<>(this, cell);
    }

    /**
     * when {@link DateUtil#isCellDateFormatted(org.apache.poi.ss.usermodel.Cell)}.
     * Date is used instead of Double because no acceptable way to get use1904Windowing for
     * {@link DataFormatter#formatRawCellContents(double, int, java.lang.String, boolean)} found.
     */
    public static class Numeric extends PropertyDescriptor<Double> {
        private final boolean isStartDate1904;
        private final DataFormatter formatter;
        private final SimpleDateFormat dformat;

        public Numeric(boolean isStartDate1904, @Nonnull DataFormatter formatter, @Nonnull SimpleDateFormat dformat) {
            super(Double.class);
            this.isStartDate1904 = isStartDate1904;
            this.formatter = formatter;
            this.dformat = (SimpleDateFormat) dformat.clone();//to be able to manipulate timezones
        }

        @Override
        public int cellType() {
            return CellType.NUMERIC.getCode();
        }

        @Nonnull
        @Override
        public Double fromString(@Nonnull String str, @Nonnull Cell cell) {
            if (DateUtil.isCellDateFormatted(cell)) {
                //str must be a date
                Date date;
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

        @Nonnull
        @Override
        public String toString(@Nonnull Double value, @Nonnull Cell cell) {
            CellStyle style = cell.getCellStyle();
            short df = style.getDataFormat();
            String dfs = style.getDataFormatString();
            return formatter.formatRawCellContents(value, df, dfs, isStartDate1904);
        }

        @Nonnull
        @Override
        public Double getValue(@Nonnull Cell cell) {
            return cell.getNumericCellValue();
        }

        @Override
        public void setValue(@Nonnull Cell cell, @Nonnull Double value) {
            cell.setCellValue(value);
        }
    }

}
