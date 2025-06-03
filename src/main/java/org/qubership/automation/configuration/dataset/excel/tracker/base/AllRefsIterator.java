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

package org.qubership.automation.configuration.dataset.excel.tracker.base;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Sets;

/**
 * Checks parents too.
 */
public abstract class AllRefsIterator<T> extends AbstractIterator<AbstractResource<T>> {

    /**
     * Items iterator deque.
     */
    private final Deque<Iterator<? extends AbstractResource<T>>> items;

    /**
     * Set of processed resources, to avoid infinite cycles.
     */
    private final Set<AbstractResource<T>> cyclicProtection;

    /**
     * Last processed resource object.
     */
    private AbstractResource<T> lastProcessed;

    /**
     * Constructor.
     *
     * @param parents Iterator of AbstractResource objects (parent objects).
     */
    public AllRefsIterator(@Nonnull final Iterator<? extends AbstractResource<T>> parents) {
        items = new LinkedList<>();
        cyclicProtection = Sets.newHashSetWithExpectedSize(5);
        items.add(parents);
    }

    /**
     * Compute next resource.
     *
     * @return AbstractResource object.
     */
    @Override
    protected AbstractResource<T> computeNext() {
        if (lastProcessed != null) {
            Iterator<? extends AbstractResource<T>> children = getChildren(lastProcessed);
            if (children != null) {
                items.push(children);
            }
            lastProcessed = null;
        }
        Iterator<? extends AbstractResource<T>> temp = items.peek();
        while (!temp.hasNext()) {
            items.remove();
            temp = items.peek();
            if (temp == null) {
                return endOfData();
            }
        }
        AbstractResource<T> result = temp.next();
        if (cyclicProtection.contains(result)) {
            return computeNext();
        }
        lastProcessed = result;
        cyclicProtection.add(result);
        return result;
    }

    /**
     * Get children resources of the parent.
     *
     * @param parent AbstractResource object
     * @return Iterator of AbstractResource objects.
     */
    @Nullable
    protected abstract Iterator<? extends AbstractResource<T>> getChildren(@Nonnull AbstractResource<?> parent);
}
