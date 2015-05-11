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
package com.microsoftopentechnologies.tooling.msservices.model.storage;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;

import java.util.Calendar;

public class StorageAccount implements ServiceTreeItem {
    public static final String CONN_STR_TEMPLATE = "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s";

    private boolean loading;
    private String name;
    private String type;
    private String description;
    private String label;
    private String status;
    private String location;
    private String affinityGroup;
    private String primaryKey;
    private String secondaryKey;
    private String managementUri;
    private String blobsUri;
    private String queuesUri;
    private String tablesUri;
    private String primaryRegion;
    private String primaryRegionStatus;
    private String secondaryRegion;
    private String secondaryRegionStatus;
    private Calendar lastFailover;
    private String subscriptionId;

    public StorageAccount(@NotNull String name,
                          @NotNull String type,
                          @NotNull String description,
                          @NotNull String label,
                          @NotNull String status,
                          @NotNull String location,
                          @NotNull String affinityGroup,
                          @NotNull String primaryKey,
                          @NotNull String secondaryKey,
                          @NotNull String managementUri,
                          @NotNull String blobsUri,
                          @NotNull String queuesUri,
                          @NotNull String tablesUri,
                          @NotNull String primaryRegion,
                          @NotNull String primaryRegionStatus,
                          @NotNull String secondaryRegion,
                          @NotNull String secondaryRegionStatus,
                          @NotNull Calendar lastFailover,
                          @NotNull String subscriptionId) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.label = label;
        this.status = status;
        this.location = location;
        this.affinityGroup = affinityGroup;
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.managementUri = managementUri;
        this.blobsUri = blobsUri;
        this.queuesUri = queuesUri;
        this.tablesUri = tablesUri;
        this.primaryRegion = primaryRegion;
        this.primaryRegionStatus = primaryRegionStatus;
        this.secondaryRegion = secondaryRegion;
        this.secondaryRegionStatus = secondaryRegionStatus;
        this.lastFailover = lastFailover;
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
    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    public void setLabel(@NotNull String label) {
        this.label = label;
    }

    @NotNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NotNull String status) {
        this.status = status;
    }

    @NotNull
    public String getLocation() {
        return location;
    }

    public void setLocation(@NotNull String location) {
        this.location = location;
    }

    @NotNull
    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(@NotNull String affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    @NotNull
    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(@NotNull String primaryKey) {
        this.primaryKey = primaryKey;
    }

    @NotNull
    public String getSecondaryKey() {
        return secondaryKey;
    }

    public void setSecondaryKey(@NotNull String secondaryKey) {
        this.secondaryKey = secondaryKey;
    }

    @NotNull
    public String getManagementUri() {
        return managementUri;
    }

    public void setManagementUri(@NotNull String managementUri) {
        this.managementUri = managementUri;
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

    @NotNull
    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public void setPrimaryRegion(@NotNull String primaryRegion) {
        this.primaryRegion = primaryRegion;
    }

    @NotNull
    public String getPrimaryRegionStatus() {
        return primaryRegionStatus;
    }

    public void setPrimaryRegionStatus(@NotNull String primaryRegionStatus) {
        this.primaryRegionStatus = primaryRegionStatus;
    }

    @NotNull
    public String getSecondaryRegion() {
        return secondaryRegion;
    }

    public void setSecondaryRegion(@NotNull String secondaryRegion) {
        this.secondaryRegion = secondaryRegion;
    }

    @NotNull
    public String getSecondaryRegionStatus() {
        return secondaryRegionStatus;
    }

    public void setSecondaryRegionStatus(@NotNull String secondaryRegionStatus) {
        this.secondaryRegionStatus = secondaryRegionStatus;
    }

    @NotNull
    public Calendar getLastFailover() {
        return lastFailover;
    }

    public void setLastFailover(@NotNull Calendar lastFailover) {
        this.lastFailover = lastFailover;
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