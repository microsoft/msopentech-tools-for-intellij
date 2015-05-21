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
import java.util.GregorianCalendar;

public class StorageAccount extends ClientStorageAccount {
    private String type = "";
    private String description = "";
    private String label = "";
    private String status = "";
    private String location = "";
    private String affinityGroup = "";
    private String secondaryKey = "";
    private String managementUri = "";
    private String primaryRegion = "";
    private String primaryRegionStatus = "";
    private String secondaryRegion = "";
    private String secondaryRegionStatus = "";
    private Calendar lastFailover = new GregorianCalendar();
    private String subscriptionId;

    public StorageAccount(@NotNull String name,
                          @NotNull String subscriptionId) {
        super(name);
        this.subscriptionId = subscriptionId;
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
}