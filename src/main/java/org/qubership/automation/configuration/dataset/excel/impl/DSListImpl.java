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

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Sheet;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.DSList;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Runnables;

public class DSListImpl<Param, Params, Vars> extends Utils.CachingIterator<Param>
        implements DSList<Param, Params, Vars> {

    /**
     * Constant Runnable throwing exception in case init is not performed.
     */
    public static final Runnable ILLEGAL_STATE = () -> {
        throw new IllegalStateException("I'm should be lazy inited");
    };

    /**
     * Sheet object.
     */
    private final Sheet mySheet;

    /**
     * Function to convert parameters.
     */
    private final Function<Iterator<Param>, Params> converterFunc;

    /**
     * EvaluationContext object.
     */
    private final EvaluationContext eval;

    /**
     * DatasetList qualifier.
     */
    private final String qualifier;

    /**
     * DatasetList name.
     */
    private String name;

    /**
     * Cached Datasets.
     */
    private volatile Set<DS<Param, Vars>> cachedDS;

    /**
     * Cached parameters.
     */
    private volatile Params cachedParams;

    /**
     * Runnable to check state.
     */
    private Runnable stateCheck = ILLEGAL_STATE;

    /**
     * Datasets iterator.
     */
    private Iterator<? extends DS<Param, Vars>> dsIter;

    /**
     * Constructor.
     *
     * @param eval EvaluationContext object
     * @param mySheet Sheet object
     * @param converterFunc Function to convert parameters
     * @param sourceQualifier String qualifier of source
     * @param cellsIter Supplier of Cells iterator.
     */
    protected DSListImpl(@Nonnull final EvaluationContext eval,
                         @Nonnull final Sheet mySheet,
                         @Nonnull final Function<Iterator<Param>, Params> converterFunc,
                         @Nonnull final String sourceQualifier,
                         @Nonnull final Supplier<? extends Iterator<?>> cellsIter) {
        super(cellsIter);
        this.name = mySheet.getSheetName();
        this.mySheet = mySheet;
        this.converterFunc = converterFunc;
        this.eval = eval;
        this.qualifier = sourceQualifier + "/" + mySheet.getSheetName();
    }

    /**
     * Get eval.
     *
     * @return EvaluationContext object.
     */
    @Nonnull
    public EvaluationContext getEval() {
        return eval;
    }

    /**
     * Lazy initialization.
     *
     * @param dsIter DS iterator.
     */
    public void lazyInit(@Nonnull final Iterator<? extends DS<Param, Vars>> dsIter) {
        this.dsIter = dsIter;
        stateCheck = Runnables.doNothing();
    }

    /**
     * Get name.
     *
     * @return String name.
     */
    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set name.
     *
     * @param name String name to set.
     */
    @Override
    public void setName(@Nonnull final String name) {
        this.name = name;
    }

    /**
     * Get datasets; init cachedDS if necessary.
     *
     * @return Set of DS cachedDS.
     */
    @Nonnull
    @Override
    public Set<DS<Param, Vars>> getDataSets() {
        if (cachedDS == null) {
            synchronized (this) {
                if (cachedDS == null) {
                    stateCheck.run();
                    cachedDS = Sets.newHashSet(dsIter);
                }
            }
        }
        return cachedDS;
    }

    /**
     * Get Dataset by name.
     *
     * @param name String dataset name
     * @return DS object if found; otherwise null.
     */
    @Override
    @Nullable
    public DS<Param, Vars> getDataSet(@Nonnull final String name) {
        return Iterables.tryFind(getDataSets(), input -> name.equals(input.getName())).orNull();
    }

    /**
     * Get parameters; init cachedParams if necessary.
     *
     * @return Params cachedParams.
     */
    @Nonnull
    @Override
    public Params getParameters() {
        if (cachedParams == null) {
            synchronized (this) {
                if (cachedParams == null) {
                    stateCheck.run();
                    cachedParams = converterFunc.apply(this);
                }
            }
        }
        return cachedParams;
    }

    /**
     * Get Sheet.
     *
     * @return Sheet mySheet.
     */
    @Nonnull
    protected Sheet getSheet() {
        return mySheet;
    }

    /**
     * Make String representation of the object.
     *
     * @return String representation of the object.
     */
    @Override
    public String toString() {
        return qualifier;
    }
}
