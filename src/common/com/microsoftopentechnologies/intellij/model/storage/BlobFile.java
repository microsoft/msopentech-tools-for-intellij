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

public class BlobFile implements ServiceTreeItem, BlobItem {
    private boolean loading;
    private String name;
    private String uri;
    private String containerName;
    private String path;
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

    public BlobFile(String name,
                    String uri,
                    String containerName,
                    String path,
                    String type,
                    String cacheControlHeader,
                    String contentEncoding,
                    String contentLanguage,
                    String contentType,
                    String contentMD5Header,
                    String eTag,
                    Calendar lastModified,
                    long size,
                    String subscriptionId) {
        this.name = name;
        this.uri = uri;
        this.containerName = containerName;
        this.path = path;
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


    public String getName() {
        return name;
    }


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }


    @Override
    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }


    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    @Override
    public BlobItemType getItemType() {
        return BlobItemType.BlobFile;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getCacheControlHeader() {
        return cacheControlHeader;
    }

    public void setCacheControlHeader(String cacheControlHeader) {
        this.cacheControlHeader = cacheControlHeader;
    }


    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }


    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }


    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    public String getContentMD5Header() {
        return contentMD5Header;
    }

    public void setContentMD5Header(String contentMD5Header) {
        this.contentMD5Header = contentMD5Header;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }


    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}