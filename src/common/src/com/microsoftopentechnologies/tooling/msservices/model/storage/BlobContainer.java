/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.tooling.msservices.model.storage;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;

import java.util.Calendar;

public class BlobContainer implements StorageServiceTreeItem {
    private boolean loading;
    private String name;
    private String uri;
    private String eTag;
    private Calendar lastModified;
    private String publicReadAccessType;

    public BlobContainer(@NotNull String name,
                         @NotNull String uri,
                         @NotNull String eTag,
                         @NotNull Calendar lastModified,
                         @NotNull String publicReadAccessType) {
        this.name = name;
        this.uri = uri;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.publicReadAccessType = publicReadAccessType;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getUri() {
        return uri;
    }

    public void setUri(@NotNull String uri) {
        this.uri = uri;
    }

    @NotNull
    public String getETag() {
        return eTag;
    }

    public void setETag(@NotNull String eTag) {
        this.eTag = eTag;
    }

    @NotNull
    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(@NotNull Calendar lastModified) {
        this.lastModified = lastModified;
    }

    @NotNull
    public String getPublicReadAccessType() {
        return publicReadAccessType;
    }

    public void setPublicReadAccessType(@NotNull String publicReadAccessType) {
        this.publicReadAccessType = publicReadAccessType;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}