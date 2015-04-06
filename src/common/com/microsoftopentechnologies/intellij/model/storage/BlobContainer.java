/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.model.storage;

import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;


import java.util.Calendar;

public class BlobContainer implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String uri;
    private String eTag;
    private Calendar lastModified;
    private String publicReadAccessType;
    private String subscriptionId;

    public BlobContainer(String name,
                         String uri,
                         String eTag,
                         Calendar lastModified,
                         String publicReadAccessType,
                         String subscriptionId) {
        this.name = name;
        this.uri = uri;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.publicReadAccessType = publicReadAccessType;
        this.subscriptionId = subscriptionId;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }


    public String getName() {
        return name;
    }


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }


    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }


    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(Calendar lastModified) {
        this.lastModified = lastModified;
    }


    public String getPublicReadAccessType() {
        return publicReadAccessType;
    }

    public void setPublicReadAccessType(String publicReadAccessType) {
        this.publicReadAccessType = publicReadAccessType;
    }


    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}