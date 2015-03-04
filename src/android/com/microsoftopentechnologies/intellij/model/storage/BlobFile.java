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
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

public class BlobFile implements ServiceTreeItem, BlobItem {
    private boolean loading;
    private String name;
    private String uri;
    private String type;
    private String cacheControlHeader;
    private String contentEncoding;
    private String contentLanguage;
    private String contentType;
    private String contentMD5Header;
    private String eTag;
    private Calendar lastModified;
    private long size;
    private String subscriptionId;

    public BlobFile(@NotNull String name,
                    @NotNull String uri,
                    @NotNull String type,
                    @NotNull String cacheControlHeader,
                    @NotNull String contentEncoding,
                    @NotNull String contentLanguage,
                    @NotNull String contentType,
                    @NotNull String contentMD5Header,
                    @NotNull String eTag,
                    @NotNull Calendar lastModified,
                    @NotNull long size,
                    @NotNull String subscriptionId) {
        this.name = name;
        this.uri = uri;
        this.type = type;
        this.cacheControlHeader = cacheControlHeader;
        this.contentEncoding = contentEncoding;
        this.contentLanguage = contentLanguage;
        this.contentType = contentType;
        this.contentMD5Header = contentMD5Header;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.size = size;
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
    @Override
    public BlobItemType getItemType() {
        return BlobItemType.BlobFile;
    }

    @NotNull
    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

    @NotNull
    public String getCacheControlHeader() {
        return cacheControlHeader;
    }

    public void setCacheControlHeader(@NotNull String cacheControlHeader) {
        this.cacheControlHeader = cacheControlHeader;
    }

    @NotNull
    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(@NotNull String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    @NotNull
    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(@NotNull String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    @NotNull
    public String getContentType() {
        return contentType;
    }

    public void setContentType(@NotNull String contentType) {
        this.contentType = contentType;
    }

    @NotNull
    public String getContentMD5Header() {
        return contentMD5Header;
    }

    public void setContentMD5Header(@NotNull String contentMD5Header) {
        this.contentMD5Header = contentMD5Header;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}