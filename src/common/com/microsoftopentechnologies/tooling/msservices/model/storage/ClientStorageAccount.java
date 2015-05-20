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
import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;

public class ClientStorageAccount implements ServiceTreeItem {
    public static final String DEFAULT_ENDPOINTS_PROTOCOL_KEY = "DefaultEndpointsProtocol";
    public static final String ACCOUNT_NAME_KEY = "AccountName";
    public static final String ACCOUNT_KEY_KEY = "AccountKey";
    public static final String BLOB_ENDPOINT_KEY = "BlobEndpoint";
    public static final String QUEUE_ENDPOINT_KEY = "QueueEndpoint";
    public static final String TABLE_ENDPOINT_KEY = "TableEndpoint";
    public static final String DEFAULT_CONN_STR_TEMPLATE = DEFAULT_ENDPOINTS_PROTOCOL_KEY + "=%s;" +
            ACCOUNT_NAME_KEY + "=%s;" +
            ACCOUNT_KEY_KEY + "=%s";
    public static final String CUSTOM_CONN_STR_TEMPLATE = BLOB_ENDPOINT_KEY + "=%s;" +
            QUEUE_ENDPOINT_KEY + "=%s;" +
            TABLE_ENDPOINT_KEY + "=%s;" +
            ACCOUNT_NAME_KEY + "=%s;" +
            ACCOUNT_KEY_KEY + "=%s";

    private String name;
    private String primaryKey = "";
    private String protocol = "";
    private String blobsUri = "";
    private String queuesUri = "";
    private String tablesUri = "";
    private boolean useCustomEndpoints;
    private boolean loading;

    public ClientStorageAccount(@NotNull String name) {
        this.name = name;
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
    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(@NotNull String primaryKey) {
        this.primaryKey = primaryKey;
    }

    @NotNull
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(@NotNull String protocol) {
        this.protocol = protocol;
    }

    @NotNull
    public String getBlobsUri() {
        return blobsUri;
    }

    public void setBlobsUri(@NotNull String blobsUri) {
        this.blobsUri = blobsUri;
    }

    @NotNull
    public String getQueuesUri() {
        return queuesUri;
    }

    public void setQueuesUri(@NotNull String queuesUri) {
        this.queuesUri = queuesUri;
    }

    @NotNull
    public String getTablesUri() {
        return tablesUri;
    }

    public void setTablesUri(@NotNull String tablesUri) {
        this.tablesUri = tablesUri;
    }

    public boolean isUseCustomEndpoints() {
        return useCustomEndpoints;
    }

    public void setUseCustomEndpoints(boolean useCustomEndpoints) {
        this.useCustomEndpoints = useCustomEndpoints;
    }

    @NotNull
    public String getConnectionString() {
        return isUseCustomEndpoints() ?
                String.format(ClientStorageAccount.CUSTOM_CONN_STR_TEMPLATE,
                        getBlobsUri(),
                        getQueuesUri(),
                        getTablesUri(),
                        getName(),
                        getPrimaryKey()) :
                String.format(ClientStorageAccount.DEFAULT_CONN_STR_TEMPLATE,
                        getProtocol(),
                        getName(),
                        getPrimaryKey());
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}