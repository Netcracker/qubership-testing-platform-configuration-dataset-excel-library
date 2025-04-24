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
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryModificator;
import org.qubership.automation.configuration.dataset.excel.impl.morphcells.Changelist;

public class DSImpl<Param, Var, Vars> extends Utils.CachingIterator<VarsConvInfo<Param>> implements DS<Param, Vars> {

    private final String qualifier;
    private final EvaluationContext eval;
    private final VarsSupplier<Param, Var, Vars> source;
    private String name;
    private DSListImpl myList;


    private Runnable stateCheck = DSListImpl.ILLEGAL_STATE;

    protected DSImpl(@Nonnull String sourceQualifier,
                     @Nonnull EvaluationContext eval,
                     @Nonnull String name,
                     @Nonnull Supplier<VarsEntryConverter<Param, Var>> varConv,
                     @Nonnull Supplier<Function<Iterator<Var>, Vars>> varsConv,
                     @Nonnull Supplier<? extends Iterator<?>> cellsIter) {
        super(cellsIter);
        setName(name);
        this.qualifier = sourceQualifier + "/" + name;
        this.eval = eval;
        this.source = doVarsSupplier(eval.getStrategy().onDsCleanup, eval.getStrategy().reuseConverters, varConv, varsConv, eval);
    }

    @Override
    public String toString() {
        return qualifier;
    }

    public void lazyInit(@Nonnull DSListImpl myList) {
        this.myList = myList;
        stateCheck = Runnables.doNothing();
    }

    @Nonnull
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(@Nonnull String name) {
        this.name = name;
    }

    public Vars getVariables() {
        stateCheck.run();
        return source.get();
    }

    @Override
    public Vars getVariables(@Nonnull VarsEntryModificator<Param> modificator) {
        stateCheck.run();
        Preconditions.checkArgument(eval.getStrategy() == ReevaluateFormulas.IN_CONVERTER,
                "Works only for ReevaluateFormulas.IN_CONVERTER");
        return source.get(modificator);
    }

    @Nonnull
    public EvaluationContext getEval() {
        return this.eval;
    }

    @Nonnull
    protected DSListImpl getDSList() {
        return myList;
    }

    private VarsSupplier<Param, Var, Vars> doVarsSupplier(boolean onDsCleanup,
                                                          boolean reuseConverters,
                                                          @Nonnull Supplier<VarsEntryConverter<Param, Var>> varConv,
                                                          @Nonnull Supplier<Function<Iterator<Var>, Vars>> varsConv,
                                                          @Nonnull EvaluationContext evaluator) {
        Iterable<VarsConvInfo<Param>> datasource = new MemoizingIterable<>(this);
        Runnable preGet = EvaluationContext.cleanupRunnable(onDsCleanup, evaluator);
        if (reuseConverters) {
            return new VarsSupplier<>(varConv,
                    varsConv,
                    datasource,
                    eval,
                    preGet);
        } else {
            return new MemoizingVarsSupplier<>(varConv,
                    varsConv,
                    datasource,
                    eval,
                    preGet);
        }
    }

    /**
     * calculates Vars each time accessed
     */
    private static class VarsSupplier<Param, Var, Vars> implements Supplier<Vars> {

        protected final EvaluationContext evaluator;
        private final Supplier<VarsEntryConverter<Param, Var>> varConv;
        private final Supplier<Function<Iterator<Var>, Vars>> varsConv;
        private final Runnable preGet;
        private final Iterable<VarsConvInfo<Param>> datasource;

        public VarsSupplier(@Nonnull Supplier<VarsEntryConverter<Param, Var>> varConv,
                            @Nonnull Supplier<Function<Iterator<Var>, Vars>> varsConv,
                            @Nonnull Iterable<VarsConvInfo<Param>> datasource,
                            @Nonnull EvaluationContext evaluator,
                            @Nonnull Runnable preGet) {
            this.varConv = varConv;
            this.varsConv = varsConv;
            this.preGet = preGet;
            this.datasource = datasource;
            this.evaluator = evaluator;
        }

        @Override
        public Vars get() {
            preGet.run();
            final VarsEntryConverter<Param, Var> varConv = this.varConv.get();
            final Iterator<VarsConvInfo<Param>> datasource = this.datasource.iterator();
            //new vars iterator should wrap datasource iterator and pass each element thru varConv with nulls omitting
            Iterator<Var> vars = new AbstractIterator<Var>() {
                @Override
                protected Var computeNext() {
                    Var computed = null;
                    while (computed == null && datasource.hasNext()) {
                        VarsConvInfo<Param> toConvert = datasource.next();
                        computed = varConv.doVarsEntry(toConvert.entity,
                                toConvert.param,
                                toConvert.convertedParam,
                                new DSCell(toConvert.var, evaluator));
                    }
                    if (computed == null)
                        return endOfData();
                    return computed;
                }
            };
            Function<Iterator<Var>, Vars> varsConv = this.varsConv.get();
            synchronized (evaluator) {
                return varsConv.apply(vars);
            }
        }

        public Vars get(@Nonnull VarsEntryModificator<Param> modificator) {
            Changelist changes = new Changelist(null, evaluator.getDescriptors());
            for (VarsConvInfo<Param> info : datasource) {
                Cell toModify = info.var;
                changes.setCurrentCell(toModify);
                modificator.modify(info.entity, info.param, info.convertedParam, changes);
            }
            synchronized (evaluator) {
                changes.applyChanges();
                Vars result;
                try {
                    result = get();
                } finally {
                    changes.revertChanges();
                }
                return result;
            }
        }
    }

    /**
     * runs preGet each time accessed
     */
    private static class MemoizingVarsSupplier<Param, Var, Vars> extends VarsSupplier<Param, Var, Vars> {
        protected final Runnable preGet;
        private Vars cached;
        private volatile boolean initialized;

        public MemoizingVarsSupplier(@Nonnull Supplier<VarsEntryConverter<Param, Var>> varConv,
                                     @Nonnull Supplier<Function<Iterator<Var>, Vars>> varsConv,
                                     @Nonnull Iterable<VarsConvInfo<Param>> datasource,
                                     @Nonnull EvaluationContext evaluator,
                                     @Nonnull Runnable preGet) {
            super(varConv, varsConv, datasource, evaluator, Runnables.doNothing());
            this.preGet = preGet;
        }

        @Override
        public Vars get() {
            preGet.run();
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        cached = super.get();
                        initialized = true;
                        return cached;
                    }
                }
            }
            return cached;
        }
    }

    private static class MemoizingIterable<T> implements Iterable<T> {

        final Iterator<T> toMemorize;
        volatile boolean initialized;
        Iterable<T> value;

        public MemoizingIterable(Iterator<T> toMemorize) {
            this.toMemorize = toMemorize;
        }


        @Override
        public Iterator<T> iterator() {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        value = Lists.newArrayList(toMemorize);
                        initialized = true;
                        return value.iterator();
                    }
                }
            }
            return value.iterator();
        }
    }
}
