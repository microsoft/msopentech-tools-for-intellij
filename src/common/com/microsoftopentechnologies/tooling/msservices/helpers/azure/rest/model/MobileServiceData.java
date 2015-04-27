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

package com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.model;

import java.util.List;

public class MobileServiceData {
    private String name;
    private String platform;
    private String type;
    private String state;
    private String selflink;
    private String applicationUrl;
    private String applicationKey;
    private String masterKey;
    private List<Table> tables;
    private String webspace;
    private String region;
    private String managementPortalLink;
    private String sourceRepositoryUrl;
    private String deploymentTriggerUrl;
    private String backendVersion;
    private String enableExternalPushEntity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSelflink() {
        return selflink;
    }

    public void setSelflink(String selflink) {
        this.selflink = selflink;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public String getWebspace() {
        return webspace;
    }

    public void setWebspace(String webspace) {
        this.webspace = webspace;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getManagementPortalLink() {
        return managementPortalLink;
    }

    public void setManagementPortalLink(String managementPortalLink) {
        this.managementPortalLink = managementPortalLink;
    }

    public String getSourceRepositoryUrl() {
        return sourceRepositoryUrl;
    }

    public void setSourceRepositoryUrl(String sourceRepositoryUrl) {
        this.sourceRepositoryUrl = sourceRepositoryUrl;
    }

    public String getDeploymentTriggerUrl() {
        return deploymentTriggerUrl;
    }

    public void setDeploymentTriggerUrl(String deploymentTriggerUrl) {
        this.deploymentTriggerUrl = deploymentTriggerUrl;
    }

    public String getBackendVersion() {
        return backendVersion;
    }

    public void setBackendVersion(String backendVersion) {
        this.backendVersion = backendVersion;
    }

    public String getEnableExternalPushEntity() {
        return enableExternalPushEntity;
    }

    public void setEnableExternalPushEntity(String enableExternalPushEntity) {
        this.enableExternalPushEntity = enableExternalPushEntity;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public class Table {
        private String name;
        private String selflink;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSelflink() {
            return selflink;
        }

        public void setSelflink(String selflink) {
            this.selflink = selflink;
        }
    }
}
