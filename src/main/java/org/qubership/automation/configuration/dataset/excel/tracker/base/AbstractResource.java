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
import org.qubership.automation.configuration.dataset.excel.tracker.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class AbstractResource<T> extends ResourceState<T> {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResource.class);

    /**
     * References Supplier object.
     */
    protected final RefsSupplier refsSup;

    /**
     * Default Check Threshold value.
     */
    protected final long defaultCheckThreshold;

    /**
     * Flag should update references (true) or not.
     * Should update refs if this resource has been updated {@link #beforeCollaborationUpdate(long)}
     * or some of myRefs have been updated/deleted {@link #notifyRefsToMe()}.
     */
    protected boolean shouldUpdateRefs = false;

    /**
     * Set of AbstractResource references.
     * Acts like a collection of event handlers for updates of this,
     * see {@link #onRefUpdate(AbstractResource)}.
     */
    protected Set<AbstractResource> refsToMe;

    /**
     * Map of String - AbstractResource.
     */
    protected BiMap<String, AbstractResource<FormulaEvaluator>> myRefs;

    /**
     * ResourceStatus object.
     */
    protected ResourceStatus status = ResourceStatus.DELETED;

    /**
     * Constructor.
     *
     * @param path Path to resource
     * @param refsSup RefsSupplier object
     * @param checkThreshold long value.
     */
    public AbstractResource(@Nonnull final Path path, @Nonnull final RefsSupplier refsSup, final long checkThreshold) {
        super(path);
        this.refsSup = refsSup;
        this.defaultCheckThreshold = checkThreshold;
    }

    /**
     * Get tree of refsToMe.
     *
     * @param parents Iterator of AbstractResource object
     * @return Iterator of AbstractResource object.
     */
    @SuppressWarnings("unchecked")
    public static Iterator<? extends AbstractResource> getAllRefsToMeTree(
            final Iterator<? extends AbstractResource> parents) {
        return new AllRefsIterator(parents) {
            @Nullable
            @Override
            protected Iterator<? extends AbstractResource> getChildren(@Nonnull final AbstractResource parent) {
                return parent.refsToMe == null ? null : parent.refsToMe.iterator();
            }
        };
    }

    /**
     * Get all references tree.
     *
     * @param parents Iterator of AbstractResource object
     * @return Iterator of AbstractResource object.
     */
    @Nonnull
    public static Iterator<AbstractResource<FormulaEvaluator>> getAllRefsTree(
            final Iterator<AbstractResource<FormulaEvaluator>> parents) {
        return new AllRefsIterator<FormulaEvaluator>(parents) {
            @Nullable
            @Override
            protected Iterator<? extends AbstractResource<FormulaEvaluator>> getChildren(
                    @Nonnull final AbstractResource<?> parent) {
                return parent.myRefs == null ? null : parent.myRefs.values().iterator();
            }
        };
    }

    /**
     * Get collaboration.
     *
     * @param parents Iterator of AbstractResource object
     * @return Iterator of AbstractResource object.
     */
    @SuppressWarnings("unchecked")
    public static Iterator<? extends AbstractResource> getCollaboration(
            final Iterator<? extends AbstractResource> parents) {
        return new AllRefsIterator(parents) {

            @Nullable
            @Override
            protected Iterator<? extends AbstractResource> getChildren(@Nonnull final AbstractResource parent) {
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

    /**
     * Handler of reference update.
     */
    protected void onAnyRefUpdate() {
    }

    /**
     * Get myRefs paths.
     *
     * @return Set of Strings.
     */
    @Nullable
    protected abstract Set<String> getMyRefsPaths();

    /**
     * Get eval.
     *
     * @return FormulaEvaluator object.
     */
    @Nullable
    public abstract FormulaEvaluator getEval();

    /**
     * Get resource.
     *
     * @return T-class object.
     */
    @Nullable
    protected abstract T getRes();

    /**
     * Before-collaboration-update handler.
     *
     * @return ResourceStatus object.
     */
    public ResourceStatus beforeCollaborationUpdate() {
        return beforeCollaborationUpdate(defaultCheckThreshold);
    }

    /**
     * Before-collaboration-update handler, using checkThreshold parameter.
     *
     * @param checkThreshold long threshold value
     * @return ResourceStatus object.
     */
    @Override
    protected ResourceStatus beforeCollaborationUpdate(final long checkThreshold) {
        ResourceStatus status = super.beforeCollaborationUpdate(checkThreshold);
        if (status.resourceWasUpdated()) {
            shouldUpdateRefs = true;
            notifyRefsToMe();
        }
        return status;
    }

    /**
     * Updates refs.
     * Updates collaborating environment if necessary.
     * Synchronized.
     *
     * @return Optional T-class object.
     */
    @Override
    public synchronized Optional<T> getResource() {
        // can return 'SAME' status even if resource is 'DELETED'
        ResourceStatus status = beforeCollaborationHierarchyUpdate();
        // tries to create a resource
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
     * Does callable if status {@link ResourceStatus#haveResource()}<br>
     * Returns result if all is ok, fails status with {@link ResourceStatus#FAILED_TO_UPDATE} if not.
     *
     * @param newStatus ResourceStatus object
     * @param callable Callable task
     * @return V-class object.
     */
    protected <V> Optional<V> processResource(@Nullable ResourceStatus newStatus,
                                              @Nonnull final Callable<V> callable) {
        V result;
        newStatus = getStatus().merge(newStatus); // should merge statuses because of the 'SAME'
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
     * Checks files for modifications (references included).<br>
     * Recreates workbooks if something changed.<br>
     * Manages {@link #shouldUpdateRefs} properties: sets to true if this or some ref has been changed.<br>
     * Stateless method, so doesn't know current resource status.<br>
     *
     * @return may return the 'SAME' status even if resource is 'DELETED' for example.
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
     * Builds tree of AbstractResources:<br>
     * Takes refs path with {@link #getMyRefsPaths()},<br>
     * Takes corresponding resources using {@link #refsSup},<br>
     * Puts it to {@link #myRefs} and {@link #refsToMe}.<br>
     *
     * @return true if this or some ref has been changed.
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

    /**
     * Update myRefs.
     *
     * @throws Exception in case some errors occurred.
     */
    protected void updateMyRefs() throws Exception {
        if (!shouldUpdateRefs) {
            return;
        }
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

    private void addRef(final String path, final AbstractResource<FormulaEvaluator> ref) {
        if (myRefs == null) {
            myRefs = HashBiMap.create();
        }
        myRefs.put(path, ref);
        if (ref.refsToMe == null) {
            ref.refsToMe = new HashSet<>();
        }
        ref.refsToMe.add(this);
    }

    @Nullable
    private AbstractResource<FormulaEvaluator> removeRef(final String path) {
        if (myRefs == null || myRefs.isEmpty()) {
            return null;
        }
        AbstractResource<FormulaEvaluator> removed = myRefs.remove(path);
        if (removed == null) {
            return null;
        }
        if (removed.refsToMe != null) {
            removed.refsToMe.remove(this);
        }
        return removed;
    }

    private void onRefUpdate(@Nonnull final AbstractResource<FormulaEvaluator> myRef) {
        shouldUpdateRefs = true;
    }

    private void notifyRefsToMe() {
        if (refsToMe != null) {
            for (AbstractResource h : refsToMe) {
                h.onRefUpdate(this);
            }
        }
    }

    /**
     * Get myRefs.
     *
     * @return Set of Resources.
     */
    @Nonnull
    @Override
    public Set<? extends Resource> getMyRefs() {
        return myRefs == null ? Collections.emptySet() : Collections.unmodifiableSet(myRefs.values());
    }

    /**
     * Get refsToMe.
     *
     * @return Set of Resources.
     */
    @Nonnull
    @Override
    public Set<? extends Resource> getRefsToMe() {
        return refsToMe == null ? Collections.emptySet() : Collections.unmodifiableSet(refsToMe);
    }

    /**
     * Get all refs to me tree.
     *
     * @return Iterator of AbstractResource.
     */
    @Nonnull
    @Override
    public Iterator<? extends AbstractResource> getAllRefsToMeTree() {
        return refsToMe == null ? Collections.emptyIterator() : getAllRefsToMeTree(refsToMe.iterator());
    }

    /**
     * Get collaboration.
     *
     * @return Iterator of AbstractResource.
     */
    public Iterator<? extends AbstractResource> getCollaboration() {
        return getCollaboration(Iterators.singletonIterator(this));
    }

    /**
     * Get all refs tree.
     *
     * @return Iterator of AbstractResource.
     */
    @Override
    @Nonnull
    public Iterator<AbstractResource<FormulaEvaluator>> getAllRefsTree() {
        return myRefs == null ? Collections.emptyIterator() : getAllRefsTree(myRefs.values().iterator());
    }

    /**
     * Get actual path.
     *
     * @param ignoreMissingRefs flag if missing refs are ignored or not
     * @return String result
     * @throws Exception in case errors occurred.
     */
    @Nonnull
    public String getActualPath(final boolean ignoreMissingRefs) throws Exception {
        String result = null;
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
                    if (result == null || str.length() < result.length()) {
                        result = str;
                    }
                    warning.append("[")
                            .append(str)
                            .append("] from [")
                            .append(entry.getValue().getPath().toAbsolutePath())
                            .append("]\n");
                }
                warning.append("Only one will be used: [")
                        .append(result)
                        .append("]. The file will not be available by using the other links!");
                if (ignoreMissingRefs) {
                    LOGGER.warn(warning.toString());
                } else {
                    throw new Exception(warning.toString());
                }
            }
        }
        return ResourceUtils.encodeRefPath(result);
    }

    /**
     * Get status.
     *
     * @return ResourceStatus object.
     */
    @Nonnull
    @Override
    public ResourceStatus getStatus() {
        return status;
    }

    private void setStatus(@Nonnull final ResourceStatus status) {
        this.status = ResourceStatus.merge(this.status, status);
    }

    /**
     * Make String representation.
     *
     * @return String representation or the object.
     */
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
