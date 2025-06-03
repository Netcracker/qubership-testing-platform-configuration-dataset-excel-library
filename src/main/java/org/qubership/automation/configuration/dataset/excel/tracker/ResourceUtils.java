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

package org.qubership.automation.configuration.dataset.excel.tracker;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.model.ExternalLinksTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qubership.automation.configuration.dataset.excel.tracker.base.Resource;
import org.qubership.automation.configuration.dataset.excel.tracker.base.ResourceStatus;

import com.google.common.collect.Sets;

public class ResourceUtils {

    /**
     * Function to get the last exception for input Resource given.
     */
    public static final Function<? super Resource<?>, Exception> GET_EXCEPTION
            = new Function<Resource<?>, Exception>() {
        @Nullable
        @Override
        public Exception apply(Resource<?> input) {
            Optional<Exception> excOpt = input.getLastException();
            return excOpt.get();
        }
    };

    /**
     * Function to get resource path.
     */
    public static final Function<? super Resource<?>, Path> GET_PATH
            = new Function<Resource, Path>() {
        @Nullable
        @Override
        public Path apply(Resource input) {
            return input.getPath();
        }
    };

    /**
     * Function to get resource file.
     */
    public static final Function<? super Resource<?>, File> GET_FILE = (Function<Resource, File>) Resource::getFile;

    /**
     * Predicate to check if the status of given resource equals ResourceStatus.DELETED.
     */
    public static final Predicate<Resource> DELETED = statusPredicate(ResourceStatus.DELETED);

    /**
     * Predicate to check if the status of given resource equals ResourceStatus.UPDATED.
     */
    public static final Predicate<Resource> UPDATED = statusPredicate(ResourceStatus.UPDATED);

    /**
     * Predicate to check if the status of given resource equals ResourceStatus.CREATED.
     */
    public static final Predicate<Resource> CREATED = statusPredicate(ResourceStatus.CREATED);

    /**
     * Predicate to check if the status of given resource equals ResourceStatus.FAILED_TO_CREATE.
     */
    public static final Predicate<Resource> FAILED_TO_CREATE = statusPredicate(ResourceStatus.FAILED_TO_CREATE);

    /**
     * Predicate to check if the status of given resource equals ResourceStatus.FAILED_TO_UPDATE.
     */
    public static final Predicate<Resource> FAILED_TO_UPDATE = statusPredicate(ResourceStatus.FAILED_TO_UPDATE);

    /**
     * Predicate to check if the status of given resource equals ResourceStatus.SAME.
     */
    public static final Predicate<Resource> SAME = statusPredicate(ResourceStatus.SAME);

    /**
     * Predicate to check if the status of given resource .resourceWasUpdated() == true.
     */
    public static final Predicate<Resource> CHANGED = input -> input.getStatus().resourceWasUpdated();

    /**
     * Predicate to check if the status of given resource .haveResource() == true.
     */
    public static final Predicate<Resource> HAS_VALUE = input -> input.getStatus().haveResource();

    /**
     * Filter to identify dataset files.
     */
    public static final FileFilter DATASET_FILTER = pathname -> pathname.isFile() &&
            (/*pathname.getName().endsWith(".xls") || */pathname.getName().endsWith(".xlsx")) &&
            !pathname.getName().startsWith("~$");

    /**
     * Function to get resource.
     */
    private static final Function<? extends Resource<?>, Object> GET_RESOURCE
            = new Function<Resource<?>, Object>() {
        @Nullable
        @Override
        public Object apply(final Resource input) {
            return input.getResource().get();
        }
    };

    /**
     * Get Predicate checking input Resource status against status parameter given.
     *
     * @param status ResourceStatus value to check
     * @return Predicate of Resource; checking itself returns boolean.
     */
    protected static Predicate<Resource> statusPredicate(@Nonnull final ResourceStatus status) {
        return input -> input.getStatus() == status;
    }

    /**
     * Get resource function.
     *
     * @param &lt;T&gt; T-class parameter.
     * @return T Function.
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<? extends Resource<T>, T> resourceFunc() {
        return (Function<? extends Resource<T>, T>) GET_RESOURCE;
    }

    /**
     * Create Workbook from the file given.
     *
     * @param file File object
     * @return Workbook object created
     * @throws Exception in case file processing errors occurred.
     */
    @Nonnull
    public static Workbook doWorkBook(@Nonnull final File file) throws Exception {
        try (Workbook wb = WorkbookFactory.create(file, null, true)) {
            return wb;
        }
    }

    /**
     * Get references from workbook given.
     *
     * @param wb Workbook object
     * @return Set of String paths of external references of workbook given.
     */
    @Nullable
    public static Set<String> getRefs(@Nonnull final Workbook wb) {
        XSSFWorkbook xlsx = (XSSFWorkbook) wb;
        List<ExternalLinksTable> links = xlsx.getExternalLinksTable();
        if (links == null || links.isEmpty()) {
            return null;
        }
        Set<String> paths = Sets.newHashSetWithExpectedSize(links.size());
        for (ExternalLinksTable t : links) {
            String fileName = t.getLinkedFileName();
            if (fileName == null) {
                continue;
            }
            String path = PackagingURIHelper.decodeURI(URI.create(fileName));
            Paths.get(path);
            paths.add(path);
        }
        return paths;
    }

    /**
     * Decode reference path.
     *
     * @param path String path to decode
     * @return String decoded and checked path.
     */
    @Nonnull
    public static String decodeRefPath(@Nonnull final String path) {
        return PackagingURIHelper.decodeURI(URI.create(path));
    }

    /**
     * Encode reference path.
     *
     * @param path String path to encode
     * @return String encoded.
     */
    @Nonnull
    public static String encodeRefPath(@Nonnull final String path) {
        return PackagingURIHelper.encode(path);
    }

}
