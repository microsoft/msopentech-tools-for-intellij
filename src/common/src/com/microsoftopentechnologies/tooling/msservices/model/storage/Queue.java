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

public class Queue implements StorageServiceTreeItem {
    private boolean loading;
    private String name;
    private String uri;
    private long approximateMessageCount;

    public Queue(@NotNull String name,
                 @NotNull String uri,
                 long approximateMessageCount) {
        this.name = name;
        this.uri = uri;
        this.approximateMessageCount = approximateMessageCount;
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

    public long getApproximateMessageCount() {
        return approximateMessageCount;
    }

    public void setApproximateMessageCount(long approximateMessageCount) {
        this.approximateMessageCount = approximateMessageCount;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}