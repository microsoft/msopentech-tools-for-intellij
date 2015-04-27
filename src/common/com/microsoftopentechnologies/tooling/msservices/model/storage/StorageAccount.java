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

    public StorageAccount(String name,
                          String type,
                          String description,
                          String label,
                          String status,
                          String location,
                          String affinityGroup,
                          String primaryKey,
                          String secondaryKey,
                          String managementUri,
                          String blobsUri,
                          String queuesUri,
                          String tablesUri,
                          String primaryRegion,
                          String primaryRegionStatus,
                          String secondaryRegion,
                          String secondaryRegionStatus,
                          Calendar lastFailover,
                          String subscriptionId) {
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


    public String getName() {
        return name;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(String affinityGroup) {
        this.affinityGroup = affinityGroup;
    }


    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }


    public String getSecondaryKey() {
        return secondaryKey;
    }

    public void setSecondaryKey(String secondaryKey) {
        this.secondaryKey = secondaryKey;
    }


    public String getManagementUri() {
        return managementUri;
    }

    public void setManagementUri(String managementUri) {
        this.managementUri = managementUri;
    }


    public String getBlobsUri() {
        return blobsUri;
    }

    public void setBlobsUri(String blobsUri) {
        this.blobsUri = blobsUri;
    }


    public String getQueuesUri() {
        return queuesUri;
    }

    public void setQueuesUri(String queuesUri) {
        this.queuesUri = queuesUri;
    }


    public String getTablesUri() {
        return tablesUri;
    }

    public void setTablesUri(String tablesUri) {
        this.tablesUri = tablesUri;
    }


    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public void setPrimaryRegion(String primaryRegion) {
        this.primaryRegion = primaryRegion;
    }


    public String getPrimaryRegionStatus() {
        return primaryRegionStatus;
    }

    public void setPrimaryRegionStatus(String primaryRegionStatus) {
        this.primaryRegionStatus = primaryRegionStatus;
    }


    public String getSecondaryRegion() {
        return secondaryRegion;
    }

    public void setSecondaryRegion(String secondaryRegion) {
        this.secondaryRegion = secondaryRegion;
    }


    public String getSecondaryRegionStatus() {
        return secondaryRegionStatus;
    }

    public void setSecondaryRegionStatus(String secondaryRegionStatus) {
        this.secondaryRegionStatus = secondaryRegionStatus;
    }


    public Calendar getLastFailover() {
        return lastFailover;
    }

    public void setLastFailover(Calendar lastFailover) {
        this.lastFailover = lastFailover;
    }


    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}