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

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;

public abstract class AbstractResource<T> extends ResourceState<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResource.class);

    protected final RefsSupplier refsSup;
    protected final long defaultCheckThreshold;
    /**
     * should update refs if this resource has been updated {@link #beforeCollaborationUpdate(long)}
     * or some of myRefs have been updated/deleted {@link #notifyRefsToMe()}
     */
    protected boolean shouldUpdateRefs = false;
    /**
     * acts like a collection of event handlers for updates of this,
     * see {@link #onRefUpdate(AbstractResource)}
     */
    protected Set<AbstractResource> refsToMe;
    protected BiMap<String, AbstractResource<FormulaEvaluator>> myRefs;
    protected ResourceStatus status = ResourceStatus.DELETED;


    /**
     * creates / inits resource
     */
    public AbstractResource(@Nonnull Path path, @Nonnull RefsSupplier refsSup, long checkThreshold) {
        super(path);
        this.refsSup = refsSup;
        this.defaultCheckThreshold = checkThreshold;

    }

    @SuppressWarnings("unchecked")
    public static Iterator<? extends AbstractResource> getAllRefsToMeTree(Iterator<? extends AbstractResource> parents) {
        return new AllRefsIterator(parents) {
            @Nullable
            @Override
            protected Iterator<? extends AbstractResource> getChildren(@Nonnull AbstractResource parent) {
                return parent.refsToMe == null ? null : parent.refsToMe.iterator();
            }
        };
    }

    @Nonnull
    public static Iterator<AbstractResource<FormulaEvaluator>> getAllRefsTree(Iterator<AbstractResource<FormulaEvaluator>> parents) {
        return new AllRefsIterator<FormulaEvaluator>(parents) {
            @Nullable
            @Override
            protected Iterator<? extends AbstractResource<FormulaEvaluator>> getChildren(@Nonnull AbstractResource<?> parent) {
                return parent.myRefs == null ? null : parent.myRefs.values().iterator();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Iterator<? extends AbstractResource> getCollaboration(Iterator<? extends AbstractResource> parents) {
        return new AllRefsIterator(parents) {

            @Nullable
            @Override
            protected Iterator<? extends AbstractResource> getChildren(@Nonnull AbstractResource parent) {
                Iterator<? extends AbstractResource> refsToMe = parent.refsToMe == null ?
                        ImmutableSet.<AbstractResource>of().iterator()
                        : parent.refsToMe.iterator();
                Iterator<? extends AbstractResource> myRefs = parent.myRefs == null ?
                        ImmutableSet.<AbstractResource>of().iterator()
                        : parent.myRefs.values().iterator();
                return Iterators.concat(myRefs, refsToMe);
            }
        };
    }

    protected void onAnyRefUpdate() {
    }

    @Nullable
    protected abstract Set<String> getMyRefsPaths();

    @Nullable
    public abstract FormulaEvaluator getEval();

    @Nullable
    protected abstract T getRes();

    public ResourceStatus beforeCollaborationUpdate() {
        return beforeCollaborationUpdate(defaultCheckThreshold);
    }

    @Override
    protected ResourceStatus beforeCollaborationUpdate(long checkThreshold) {
        ResourceStatus status = super.beforeCollaborationUpdate(checkThreshold);
        if (status.resourceWasUpdated()) {
            shouldUpdateRefs = true;
            notifyRefsToMe();
        }
        return status;
    }

    /**
     * updates refs
     * updates collaborating environment if necessary
     * synchronized
     */
    @Override
    public synchronized Optional<T> getResource() {
        ResourceStatus status = beforeCollaborationHierarchyUpdate();//can return 'SAME' status even if resource is 'DELETED'
        //tries to create a resource
        return processResource(status, () -> {
            boolean refsUpdated = updateMyRefsTree();
            if (refsUpdated) {
                onAnyRefUpdate();
                refsSup.setupCollaboratingEnv(AbstractResource.this);
                afterCollaboration(path, file);
            }
            return getRes();
        });
    }

    /**
     * does callable if status {@link ResourceStatus#haveResource()}<br/>
     * returns result if all is ok<br/>
     * fails status with {@link ResourceStatus#FAILED_TO_UPDATE} if not<br/>
     */
    protected <V> Optional<V> processResource(@Nullable ResourceStatus newStatus, @Nonnull Callable<V> callable) {
        V result;
        newStatus = getStatus().merge(newStatus);//should merge statuses because of the 'SAME'
        if (!newStatus.haveResource()) {
            setStatus(newStatus);
            return Optional.empty();
        }
        try {
            result = callable.call();
            setStatus(newStatus);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            lastException = e;
            setStatus(ResourceStatus.FAILED_TO_UPDATE);
            return Optional.empty();
        }
    }

    /**
     * checks files for modifications (references included)<br/>
     * recreates workbooks if something changed<br/>
     * manages {@link #shouldUpdateRefs} properties: sets to true if this or some ref has been changed<br/>
     * stateless method, so doesn't know current resource status<br/>
     *
     * @return may return the 'SAME' status even if resource is 'DELETED' for example<br/>
     */
    protected ResourceStatus beforeCollaborationHierarchyUpdate() {
        ResourceStatus status = beforeCollaborationUpdate();
        Iterator<AbstractResource<FormulaEvaluator>> allrefs = getAllRefsTree();
        while (allrefs.hasNext()) {
            AbstractResource<FormulaEvaluator> ref = allrefs.next();
            ref.beforeCollaborationUpdate();
        }
        return status;
    }

    /**
     * builds tree of AbstractResources:<br/>
     * takes refs path with {@link #getMyRefsPaths()}<br/>
     * takes corresponding resources using {@link #refsSup}<br/>
     * puts it to {@link #myRefs} and {@link #refsToMe}<br/>
     *
     * @return true if this or some ref has been changed
     */
    protected boolean updateMyRefsTree() throws Exception  {
        boolean somethingChanged = shouldUpdateRefs;
        updateMyRefs();
        Iterator<AbstractResource<FormulaEvaluator>> allrefs = getAllRefsTree();
        while (allrefs.hasNext()) {
            AbstractResource<FormulaEvaluator> ref = allrefs.next();
            somethingChanged = somethingChanged || ref.shouldUpdateRefs;
            ref.updateMyRefs();
        }
        return somethingChanged;
    }

    protected void updateMyRefs() throws Exception {
        if (!shouldUpdateRefs)
            return;
        Set<String> refPaths = getMyRefsPaths();
        refPaths = refPaths == null ? Collections.<String>emptySet() : refPaths;
        Set<String> currentRefPaths = myRefs == null ? Collections.<String>emptySet() : myRefs.keySet();

        for (String toDelete : Sets.difference(currentRefPaths, refPaths).immutableCopy()) {
            removeRef(toDelete);
        }
        for (String toAdd : Sets.difference(refPaths, currentRefPaths).immutableCopy()) {
            addRef(toAdd, refsSup.getRef(this, toAdd));
        }
        shouldUpdateRefs = false;
    }

    private void addRef(String path, AbstractResource<FormulaEvaluator> ref) {
        if (myRefs == null) {
            myRefs = HashBiMap.create();
        }
        myRefs.put(path, ref);
        if (ref.refsToMe == null)
            ref.refsToMe = new HashSet<>();
        ref.refsToMe.add(this);
    }

    @Nullable
    private AbstractResource<FormulaEvaluator> removeRef(String path) {
        if (myRefs == null || myRefs.isEmpty())
            return null;

        AbstractResource<FormulaEvaluator> removed = myRefs.remove(path);
        if (removed == null)
            return null;
        if (removed.refsToMe != null)
            removed.refsToMe.remove(this);
        return removed;
    }

    private void onRefUpdate(@Nonnull AbstractResource<FormulaEvaluator> myRef) {
        shouldUpdateRefs = true;
    }

    private void notifyRefsToMe() {
        if (refsToMe != null) {
            for (AbstractResource h : refsToMe) {
                h.onRefUpdate(this);
            }
        }
    }

    @Nonnull
    @Override
    public Set<? extends Resource> getMyRefs() {
        if (myRefs == null)
            return Collections.emptySet();
        else
            return Collections.unmodifiableSet(myRefs.values());
    }

    @Nonnull
    @Override
    public Set<? extends Resource> getRefsToMe() {
        if (refsToMe == null)
            return Collections.emptySet();
        else
            return Collections.unmodifiableSet(refsToMe);
    }

    @Nonnull
    @Override
    public Iterator<? extends AbstractResource> getAllRefsToMeTree() {
        if (refsToMe == null)
            return Collections.emptyIterator();
        return getAllRefsToMeTree(refsToMe.iterator());
    }

    public Iterator<? extends AbstractResource> getCollaboration() {
        return getCollaboration(Iterators.singletonIterator(this));
    }

    @Override
    @Nonnull
    public Iterator<AbstractResource<FormulaEvaluator>> getAllRefsTree() {
        if (myRefs == null)
            return Collections.emptyIterator();
        return getAllRefsTree(myRefs.values().iterator());
    }

    @Nonnull
    public String getActualPath(boolean ignoreMissingRefs) throws Exception {
        String result = null;

        //will find in refs to me
        if (refsToMe == null || refsToMe.isEmpty()) {
            result = getFile().getName();
        } else if (refsToMe.size() == 1) {
            result = ((AbstractResource<?>) refsToMe.iterator().next()).myRefs.inverse().get(this);
        } else {
            Map<String, AbstractResource> multipleBindings = Maps.newHashMapWithExpectedSize(refsToMe.size());
            for (AbstractResource res : refsToMe) {
                multipleBindings.put(((AbstractResource<?>) res).myRefs.inverse().get(this), res);
            }
            if (multipleBindings.size() <= 1) {
                result = multipleBindings.keySet().iterator().next();
            } else {
                StringBuilder warning = new StringBuilder("You've got multiple bindings to [")
                        .append(getPath().toAbsolutePath()).append("] file:\r\n");
                for (Map.Entry<String, AbstractResource> entry : multipleBindings.entrySet()) {
                    String str = entry.getKey();
                    if (result == null || str.length() < result.length())
                        result = str;
                    warning.append("[").append(str).append("] from [").append(entry.getValue().getPath().toAbsolutePath()).append("]\n");
                }
                warning.append("Only one will be used: [").append(result).append("]. The file will not be available by using the other links!");
                if (ignoreMissingRefs) {
                    LOGGER.warn(warning.toString());
                } else {
                    throw new Exception(warning.toString());
                }
            }
        }
        return ResourceUtils.encodeRefPath(result);

    }

    @Nonnull
    @Override
    public ResourceStatus getStatus() {
        return status;
    }

    private void setStatus(@Nonnull ResourceStatus status) {
        this.status = ResourceStatus.merge(this.status, status);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[")
                .append(getStatus())
                .append("|DataSet|")
                .append(getPath())
                .append("]");
        return result.toString();
    }
}
