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

package org.qubership.automation.configuration.dataset.excel.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.qubership.automation.configuration.dataset.excel.builder.config.BaseConfig;
import org.qubership.automation.configuration.dataset.excel.core.ColumnHandler;
import org.qubership.automation.configuration.dataset.excel.core.Consumer;
import org.qubership.automation.configuration.dataset.excel.core.DSList;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public abstract class AbstractDSFormer<Param, Params, Var, Vars>
        implements ColumnHandler, Supplier<DSList<Param, Params, Vars>> {

    /**
     * BaseConfig object.
     */
    protected final BaseConfig<Param, Params, Var, Vars> settings;

    /**
     * Evaluation Context.
     */
    protected final EvaluationContext evaluationContext;

    /**
     * Predicate of Cells.
     */
    protected final Predicate<Cell> columnsPred;

    /**
     * Sheet object.
     */
    private final Sheet sheet;

    /**
     * Rows iterator.
     */
    private final Iterator<Row> rows;

    /**
     * DataSet List object.
     */
    private DSListImpl<Param, Params, Vars> dsList;

    /**
     * Supplier of cells iterator.
     */
    private Utils.MutableSupplier<Iterator<Cell>> dataSourceSup = Utils.MutableSupplier.create();

    /**
     * Datasets collection.
     */
    private Collection<DSImpl<Param, Var, Vars>> dataSets;

    /**
     * Constructor.
     *
     * @param sheet Sheet object
     * @param settings Base Config
     * @param evaluationContext EvaluationContext object.
     */
    protected AbstractDSFormer(@Nonnull final Sheet sheet,
                               @Nonnull final BaseConfig<Param, Params, Var, Vars> settings,
                               @Nonnull final EvaluationContext evaluationContext) {
        this.sheet = sheet;
        this.settings = settings;
        this.rows = sheet.rowIterator();
        this.evaluationContext = evaluationContext;
        this.columnsPred = Utils.statefulHeaderPredicate(settings.columnsPred, evaluationContext);
    }

    /**
     * Returns iterator which iterate over significant data only.
     * This data is filtered using rowStrategy.
     *
     * @param rows        - rows iterator
     * @param rowStrategy - column selector strategy
     * @param newLineCb   - Runnable object
     * @return iterator to cycle over all passed rows using row strategy.
     */
    @Nonnull
    private static Iterator<Cell> applyRowsStrategy(@Nonnull final Iterator<Row> rows,
                                                    @Nonnull final Function<Iterator<Cell>, Iterator<Cell>> rowStrategy,
                                                    @Nonnull final Runnable newLineCb) {
        return Iterators.concat(new AbstractIterator<Iterator<Cell>>() {
            @Override
            protected Iterator<Cell> computeNext() {
                if (!rows.hasNext()) {
                    return endOfData();
                }
                Iterator<Cell> newCellIter = rows.next().cellIterator();
                newLineCb.run();
                return rowStrategy.apply(newCellIter);
            }
        });
    }

    @Nonnull
    private static ColumnsMemory doColumnsMemory(@Nonnull final Predicate<Cell> predicate,
                                                 @Nullable final List<Predicate<Cell>> mandatory) {
        ColumnsMemory result = new ColumnsMemory();
        result.getPredicates().add(predicate);
        if (mandatory != null) {
            for (Predicate<Cell> pred : Lists.reverse(mandatory)) {
                result.getPredicates().addFirst(pred);
            }
        }
        return result;
    }

    /**
     * Get mandatory columns list.
     *
     * @return List of Cell Predicates.
     */
    @Nullable
    protected abstract List<Predicate<Cell>> getMandatoryColumns();

    /**
     * Get Dataset List.
     *
     * @return DSList object.
     */
    @Nullable
    @Override
    public DSList<Param, Params, Vars> get() {
        if (!rows.hasNext()) {
            return null;
        }
        Row header = rows.next();
        ColumnsMemory memory = doColumnsMemory();
        Iterator<Cell> toMemorize = memory.apply(header.cellIterator());
        Preconditions.checkNotNull(toMemorize, "[%s] should not return null", memory);
        while (toMemorize.hasNext()) // read header; column handlers will be set via 'getHandler(Cell,Predicate)'
        {
            toMemorize.next();
        }
        return doList(memory);
    }

    /**
     * Get Dataset List.
     *
     * @param memory ColumnsMemory object
     * @return DSList object.
     */
    @Nullable
    protected DSList<Param, Params, Vars> doList(final ColumnsMemory memory) {
        if (dsList == null) {
            return null;
        }
        Iterator<Cell> dataCells = applyRowsStrategy(rows, memory, this::nextRow);
        dataSourceSup.set(dataCells);
        Iterator<DSImpl<Param, Var, Vars>> dataSetsIter;
        if (dataSets == null) {
            dataSetsIter = Collections.emptyIterator();
        } else {
            for (DSImpl<Param, Var, Vars> dsImpl : dataSets) {
                dsImpl.lazyInit(dsList);
            }
            dataSetsIter = dataSets.iterator();
        }
        dsList.lazyInit(dataSetsIter);
        return dsList;
    }

    /**
     * Get handler.
     *
     * @param headerCell Cell of column header
     * @param predicate  Predicate of Cells
     * @return Consumer of Cells object.
     */
    @Nullable
    @Override
    public abstract Consumer<Cell> getHandler(@Nonnull Cell headerCell, @Nonnull Predicate<Cell> predicate);

    /**
     * Go to next row.
     */
    protected abstract void nextRow();

    /**
     * Make DSList.
     *
     * @return new DSListImpl object.
     */
    protected DSList<Param, Params, Vars> doDSList() {
        dsList = new DSListImpl<>(evaluationContext, sheet, settings.paramsConverter, settings.sourceQualifier, dataSourceSup);
        return dsList;
    }

    /**
     * Push Param into DSList.
     *
     * @param input Param to be pushed.
     */
    protected void pushToDSList(final Param input) {
        dsList.accept(input);
    }

    /**
     * Make Dataset.
     *
     * @param headerCell Cell object of header
     * @return new DSImpl object.
     */
    @Nonnull
    protected DSImpl<Param, Var, Vars> doDS(@Nonnull final Cell headerCell) {
        final DSImpl<Param, Var, Vars> ds = new DSImpl<>(Objects.toString(dsList),
                evaluationContext,
                evaluationContext.getCellValue(headerCell).toString(),
                settings.varConverter,
                settings.varsConverter,
                dataSourceSup);
        if (dataSets == null) {
            dataSets = Lists.newArrayList();
        }
        dataSets.add(ds);
        return ds;
    }

    @Nonnull
    private ColumnsMemory doColumnsMemory() {
        ColumnsMemory result = doColumnsMemory(columnsPred, getMandatoryColumns());
        result.setCBProvider(this);
        return result;
    }
}
