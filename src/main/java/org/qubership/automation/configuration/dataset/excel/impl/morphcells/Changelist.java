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

    private final PropertyDescriptors descriptors;
    private Multimap<Cell, Change> changes;
    private Cell currentCell;

    public Changelist(Cell currentCell, @Nonnull PropertyDescriptors descriptors) {
        this.currentCell = currentCell;
        this.descriptors = descriptors;
    }

    @Override
    public void setCellValue(@Nullable String value) {
        track(descriptors.convertReplaceValue(getCurrentCell(), value));
    }

    @Override
    public void setCellValue(double value) {
        replaceCurrentCellValue(value);
    }

    @Override
    public void setCellValue(@Nonnull Date value) {
        Preconditions.checkNotNull(value);
        replaceCurrentCellValue(descriptors.dateToDouble(value));
    }

    @Override
    public void setCellValue(LocalDateTime localDateTime) {
        Preconditions.checkNotNull(localDateTime);
        replaceCurrentCellValue(descriptors.dateToDouble(localDateTime));
    }

    @Override
    public void setCellValue(Calendar value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCellValue(RichTextString value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCellValue(boolean value) {
        replaceCurrentCellValue(value);
    }

    @Override
    public void setCellErrorValue(byte value) {
        replaceCurrentCellValue(value);
    }

    @Override
    public void setAsActiveCell() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCellComment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeHyperlink() {
        throw new UnsupportedOperationException();
    }

    //region getters
    @Override
    public CellRangeAddress getArrayFormulaRange() {
        return getCurrentCell().getArrayFormulaRange();
    }

    @Override
    public boolean isPartOfArrayFormulaGroup() {
        return getCurrentCell().isPartOfArrayFormulaGroup();
    }

    @Override
    public Hyperlink getHyperlink() {
        return getCurrentCell().getHyperlink();
    }

    @Override
    public void setHyperlink(Hyperlink link) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CellAddress getAddress() {
        return getCurrentCell().getAddress();
    }
    //endregion

    @Override
    public Comment getCellComment() {
        return getCurrentCell().getCellComment();
    }

    @Override
    public void setCellComment(Comment comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBooleanCellValue() {
        return getCurrentCell().getBooleanCellValue();
    }

    @Override
    public byte getErrorCellValue() {
        return getCurrentCell().getErrorCellValue();
    }

    @Override
    public CellStyle getCellStyle() {
        return getCurrentCell().getCellStyle();
    }

    @Override
    public void setCellStyle(CellStyle style) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getNumericCellValue() {
        return getCurrentCell().getNumericCellValue();
    }

    @Override
    public Date getDateCellValue() {
        return getCurrentCell().getDateCellValue();
    }

    @Override
    public LocalDateTime getLocalDateTimeCellValue() {
        return getCurrentCell().getLocalDateTimeCellValue();
    }

    @Override
    public RichTextString getRichStringCellValue() {
        return getCurrentCell().getRichStringCellValue();
    }

    @Override
    public String getStringCellValue() {
        Cell current = getCurrentCell();
        return descriptors.forCell(current).getToString(current);
    }

    @Override
    public String getCellFormula() {
        return getCurrentCell().getCellFormula();
    }

    @Override
    public void setCellFormula(@Nonnull String formula) throws FormulaParseException {
        replaceCurrentCellValue(formula);
    }

    @Override
    public void removeFormula() throws IllegalStateException {
        replaceCurrentCellValue(StringUtils.EMPTY);
    }

    @Override
    public int getColumnIndex() {
        return getCurrentCell().getColumnIndex();
    }

    @Override
    public int getRowIndex() {
        return getCurrentCell().getRowIndex();
    }

    @Override
    public Sheet getSheet() {
        return getCurrentCell().getSheet();
    }

    @Override
    public Row getRow() {
        return getCurrentCell().getRow();
    }

    @Override
    public CellType getCellType() {
        return getCurrentCell().getCellType();
    }

    @Override
    public CellType getCellTypeEnum() {
        return getCurrentCell().getCellTypeEnum();
    }

    //region not implemented
    public void setCellType(int cellType) {
        throw new UnsupportedOperationException("Will not change original cell type");
    }

    @Override
    public void setCellType(CellType cellType) {
        throw new UnsupportedOperationException("Will not change original cell type");
    }

    @Override
    public void setBlank() {
        track(descriptors.convertReplaceValue(getCurrentCell(), StringUtils.EMPTY));
    }

    @Override
    public CellType getCachedFormulaResultType() {
        return getCurrentCell().getCachedFormulaResultType();
    }

    @Override
    public CellType getCachedFormulaResultTypeEnum() {
        return getCurrentCell().getCachedFormulaResultTypeEnum();
    }
    //endregion

    @Nonnull
    public Cell getCurrentCell() {
        return this.currentCell;
    }

    public void setCurrentCell(@Nonnull Cell cell) {
        this.currentCell = cell;
    }

    protected void replaceCurrentCellValue(@Nonnull Object newValue) {
        Preconditions.checkNotNull(newValue);
        track(descriptors.replaceValue(getCurrentCell(), newValue));
    }

    protected void track(@Nonnull Change change) {
        if (changes == null) {
            changes = MultimapBuilder.hashKeys().hashSetValues().build();
        }
        changes.put(change.getTarget(), change);
    }

    public void applyChanges() {
        if (changes == null)
            return;
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

    public void revertChanges() {
        if (changes == null)
            return;
        for (Change change : changes.values()) {
            change.revert();
        }
    }
}
