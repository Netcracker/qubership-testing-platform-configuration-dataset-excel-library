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

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Runnables;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.DSList;

public class DSListImpl<Param, Params, Vars> extends Utils.CachingIterator<Param> implements DSList<Param, Params, Vars> {
    public static final Runnable ILLEGAL_STATE = () -> {
        throw new IllegalStateException("I'm should be lazy inited");
    };
    private final Sheet mySheet;
    private final Function<Iterator<Param>, Params> converterFunc;
    private final EvaluationContext eval;
    private final String qualifier;
    private String name;
    private volatile Set<DS<Param, Vars>> cachedDS;
    private volatile Params cachedParams;
    private Runnable stateCheck = ILLEGAL_STATE;
    private Iterator<? extends DS<Param, Vars>> dsIter;

    protected DSListImpl(@Nonnull EvaluationContext eval, @Nonnull Sheet mySheet,
                         @Nonnull Function<Iterator<Param>, Params> converterFunc, @Nonnull String sourceQualifier,
                         @Nonnull Supplier<? extends Iterator<?>> cellsIter) {
        super(cellsIter);
        this.name = mySheet.getSheetName();
        this.mySheet = mySheet;
        this.converterFunc = converterFunc;
        this.eval = eval;
        this.qualifier = sourceQualifier + "/" + mySheet.getSheetName();
    }

    @Nonnull
    public EvaluationContext getEval() {
        return eval;
    }

    public void lazyInit(@Nonnull Iterator<? extends DS<Param, Vars>> dsIter) {
        this.dsIter = dsIter;
        stateCheck = Runnables.doNothing();
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(@Nonnull String name) {
        this.name = name;
    }

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

    @Override
    @Nullable
    public DS<Param, Vars> getDataSet(@Nonnull final String name) {
        return Iterables.tryFind(getDataSets(), input -> name.equals(input.getName())).orNull();
    }

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

    @Nonnull
    protected Sheet getSheet() {
        return mySheet;
    }

    @Override
    public String toString() {
        return qualifier;
    }
}
