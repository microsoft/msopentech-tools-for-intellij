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
package com.microsoftopentechnologies.intellij.model.storage;

import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Map;

public class TableEntity implements ServiceTreeItem {
    private boolean loading;
    private String partitionKey;
    private String rowKey;
    private String tableName;
    private String eTag;
    private Calendar timestamp;
    private Map<String, String> properties;
    private String subscriptionId;

    public TableEntity(@NotNull String partitionKey,
                       @NotNull String rowKey,
                       @NotNull String tableName,
                       @NotNull String eTag,
                       @NotNull Calendar timestamp,
                       @NotNull Map<String, String> properties,
                       @NotNull String subscriptionId) {
        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
        this.tableName = tableName;
        this.eTag = eTag;
        this.timestamp = timestamp;
        this.properties = properties;
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
    public String getPartitionKey() {
        return partitionKey;
    }

    @NotNull
    public String getRowKey() {
        return rowKey;
    }

    @NotNull
    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NotNull String tableName) {
        this.tableName = tableName;
    }

    @NotNull
    public String getETag() {
        return eTag;
    }

    public void setETag(@NotNull String eTag) {
        this.eTag = eTag;
    }

    @NotNull
    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NotNull Calendar timestamp) {
        this.timestamp = timestamp;
    }

    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return partitionKey + " - " + rowKey + (loading ? " (loading...)" : "");
    }
}