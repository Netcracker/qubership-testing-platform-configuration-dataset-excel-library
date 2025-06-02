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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * acts like a proxy for {@link #getCurrentCell()},
 * tracks all modifications to cells and can {@link #revertChanges()} them.
 */
public class Changelist implements Cell {

    /**
     * Property Descriptors object.
     */
    private final PropertyDescriptors descriptors;

    /**
     * Map of Cell - Change.
     */
    private Multimap<Cell, Change> changes;

    /**
     * The current Cell.
     */
    private Cell currentCell;

    /**
     * Constructor.
     *
     * @param currentCell Cell current cell object
     * @param descriptors Property Descriptors
     */
    public Changelist(final Cell currentCell, @Nonnull final PropertyDescriptors descriptors) {
        this.currentCell = currentCell;
        this.descriptors = descriptors;
    }

    /**
     * Set value.
     *
     * @param value String value to set.
     */
    @Override
    public void setCellValue(@Nullable final String value) {
        track(descriptors.convertReplaceValue(getCurrentCell(), value));
    }

    /**
     * Set value.
     *
     * @param value double value to set.
     */
    @Override
    public void setCellValue(final double value) {
        replaceCurrentCellValue(value);
    }

    /**
     * Set value.
     *
     * @param value Date value to set.
     */
    @Override
    public void setCellValue(@Nonnull final Date value) {
        Preconditions.checkNotNull(value);
        replaceCurrentCellValue(descriptors.dateToDouble(value));
    }

    /**
     * Set value.
     *
     * @param localDateTime LocalDateTime value to set.
     */
    @Override
    public void setCellValue(final LocalDateTime localDateTime) {
        Preconditions.checkNotNull(localDateTime);
        replaceCurrentCellValue(descriptors.dateToDouble(localDateTime));
    }

    /**
     * Set value.
     *
     * @param value Calendar value to set.
     */
    @Override
    public void setCellValue(final Calendar value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set value.
     *
     * @param value RichTextString value to set.
     */
    @Override
    public void setCellValue(final RichTextString value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set value.
     *
     * @param value boolean value to set.
     */
    @Override
    public void setCellValue(final boolean value) {
        replaceCurrentCellValue(value);
    }

    /**
     * Set value (with a special meaning - 'Cell Error' value).
     *
     * @param value byte value to set.
     */
    @Override
    public void setCellErrorValue(final byte value) {
        replaceCurrentCellValue(value);
    }

    /**
     * Set Cell as Active Cell; unsupported actually.
     */
    @Override
    public void setAsActiveCell() {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove Cell Comment; unsupported actually.
     */
    @Override
    public void removeCellComment() {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove Hyperlink; unsupported actually.
     */
    @Override
    public void removeHyperlink() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get Array Formula Range.
     *
     * @return CellRangeAddress object.
     */
    //region getters
    @Override
    public CellRangeAddress getArrayFormulaRange() {
        return getCurrentCell().getArrayFormulaRange();
    }

    /**
     * Check if the current cell is a part of Array Formula Group.
     *
     * @return true/false.
     */
    @Override
    public boolean isPartOfArrayFormulaGroup() {
        return getCurrentCell().isPartOfArrayFormulaGroup();
    }

    /**
     * Get Hyperlink.
     *
     * @return Hyperlink object.
     */
    @Override
    public Hyperlink getHyperlink() {
        return getCurrentCell().getHyperlink();
    }

    /**
     * Set Hyperlink.
     *
     * @param link Hyperlink to set.
     */
    @Override
    public void setHyperlink(final Hyperlink link) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get cell address.
     *
     * @return CellAddress of the current cell.
     */
    @Override
    public CellAddress getAddress() {
        return getCurrentCell().getAddress();
    }
    //endregion

    /**
     * Get cell comment.
     *
     * @return Comment of the current cell.
     */
    @Override
    public Comment getCellComment() {
        return getCurrentCell().getCellComment();
    }

    /**
     * Set Cell Comment; unsupported currently.
     *
     * @param comment Comment object to set.
     */
    @Override
    public void setCellComment(final Comment comment) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get boolean cell value.
     *
     * @return boolean value.
     */
    @Override
    public boolean getBooleanCellValue() {
        return getCurrentCell().getBooleanCellValue();
    }

    /**
     * Get Error Cell Value.
     *
     * @return byte error value.
     */
    @Override
    public byte getErrorCellValue() {
        return getCurrentCell().getErrorCellValue();
    }

    /**
     * Get Cell Style.
     *
     * @return CellStyle object.
     */
    @Override
    public CellStyle getCellStyle() {
        return getCurrentCell().getCellStyle();
    }

    /**
     * Set Cell Style.
     *
     * @param style CellStyle object to set.
     */
    @Override
    public void setCellStyle(final CellStyle style) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get numeric cell value.
     *
     * @return double value.
     */
    @Override
    public double getNumericCellValue() {
        return getCurrentCell().getNumericCellValue();
    }

    /**
     * Get Date cell value.
     *
     * @return Date object.
     */
    @Override
    public Date getDateCellValue() {
        return getCurrentCell().getDateCellValue();
    }

    /**
     * Get LocalDateTime cell value.
     *
     * @return LocalDateTime object.
     */
    @Override
    public LocalDateTime getLocalDateTimeCellValue() {
        return getCurrentCell().getLocalDateTimeCellValue();
    }

    /**
     * Get RichTextString cell value.
     *
     * @return RichTextString object.
     */
    @Override
    public RichTextString getRichStringCellValue() {
        return getCurrentCell().getRichStringCellValue();
    }

    /**
     * Get String cell value.
     *
     * @return String object (cell value converted to String).
     */
    @Override
    public String getStringCellValue() {
        Cell current = getCurrentCell();
        return descriptors.forCell(current).getToString(current);
    }

    /**
     * Get Cell Formula.
     *
     * @return String representation of the formula.
     */
    @Override
    public String getCellFormula() {
        return getCurrentCell().getCellFormula();
    }

    /**
     * Set Cell Formula.
     *
     * @param formula String formula to set
     * @throws FormulaParseException in case formula expression parsing exception.
     */
    @Override
    public void setCellFormula(@Nonnull final String formula) throws FormulaParseException {
        replaceCurrentCellValue(formula);
    }

    /**
     * Remove Cell Formula. It performs replacing of the formula value to empty String value.
     *
     * @throws IllegalStateException in case various errors occurred.
     */
    @Override
    public void removeFormula() throws IllegalStateException {
        replaceCurrentCellValue(StringUtils.EMPTY);
    }

    /**
     * Get Column Index of the current cell.
     *
     * @return int index of the column.
     */
    @Override
    public int getColumnIndex() {
        return getCurrentCell().getColumnIndex();
    }

    /**
     * Get Row Index of the current cell.
     *
     * @return int index of the row.
     */
    @Override
    public int getRowIndex() {
        return getCurrentCell().getRowIndex();
    }

    /**
     * Get Sheet of the current cell.
     *
     * @return Sheet object.
     */
    @Override
    public Sheet getSheet() {
        return getCurrentCell().getSheet();
    }

    /**
     * Get Row of the current cell.
     *
     * @return Row object.
     */
    @Override
    public Row getRow() {
        return getCurrentCell().getRow();
    }

    /**
     * Get Cell Type.
     *
     * @return CellType object of the current cell.
     */
    @Override
    public CellType getCellType() {
        return getCurrentCell().getCellType();
    }

    /**
     * Get Cell Type Enum.
     *
     * @return CellType Enum value of the current cell.
     */
    @Override
    public CellType getCellTypeEnum() {
        return getCurrentCell().getCellTypeEnum();
    }

    //region not implemented

    /**
     * Set Cell Type; actually unsupported.
     *
     * @param cellType int cell type to set.
     */
    public void setCellType(final int cellType) {
        throw new UnsupportedOperationException("Will not change original cell type");
    }

    /**
     * Set Cell Type; actually unsupported.
     *
     * @param cellType CellType cell type to set.
     */
    @Override
    public void setCellType(final CellType cellType) {
        throw new UnsupportedOperationException("Will not change original cell type");
    }

    /**
     * Set Cell Value to empty String.
     */
    @Override
    public void setBlank() {
        track(descriptors.convertReplaceValue(getCurrentCell(), StringUtils.EMPTY));
    }

    /**
     * Get cached formula result type.
     *
     * @return CellType cached formula result type.
     */
    @Override
    public CellType getCachedFormulaResultType() {
        return getCurrentCell().getCachedFormulaResultType();
    }

    /**
     * Get cached formula result type enum.
     *
     * @return CellType cached formula result type enum.
     */
    @Override
    public CellType getCachedFormulaResultTypeEnum() {
        return getCurrentCell().getCachedFormulaResultTypeEnum();
    }

    //endregion

    /**
     * Get the current cell.
     *
     * @return Cell current cell.
     */
    @Nonnull
    public Cell getCurrentCell() {
        return this.currentCell;
    }

    /**
     * Set the current cell.
     *
     * @param cell Cell object to set.
     */
    public void setCurrentCell(@Nonnull final Cell cell) {
        this.currentCell = cell;
    }

    /**
     * Replace value of the current cell.
     *
     * @param newValue Object new value to set.
     */
    protected void replaceCurrentCellValue(@Nonnull final Object newValue) {
        Preconditions.checkNotNull(newValue);
        track(descriptors.replaceValue(getCurrentCell(), newValue));
    }

    /**
     * Track the change in the changes map.
     *
     * @param change Change object to track.
     */
    protected void track(@Nonnull final Change change) {
        if (changes == null) {
            changes = MultimapBuilder.hashKeys().hashSetValues().build();
        }
        changes.put(change.getTarget(), change);
    }

    /**
     * Apply all changes.
     * Before applying a change, adds it to the special List in order to be able to revert it.
     * In case exceptions, all changes are reverted.
     */
    public void applyChanges() {
        if (changes == null) {
            return;
        }
        List<Change> toRevert = Lists.newArrayList();
        for (Change change : changes.values()) {
            try {
                toRevert.add(change);
                change.apply();
            } catch (Exception e) {
                for (Change revert : toRevert) {
                    revert.revert();
                }
                throw new RuntimeException("Can not apply change [" + change + "]", e);
            }
        }
    }

    /**
     * Revert all changes.
     */
    public void revertChanges() {
        if (changes == null) {
            return;
        }
        for (Change change : changes.values()) {
            change.revert();
        }
    }
}
