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

package com.microsoftopentechnologies.tooling.msservices.model;

public class Subscription {
    private String id;
    private String name;
    private String managementCertificate;
    private String serviceManagementUrl;
    private boolean selected;
    private String tenantId;

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagementCertificate() {
        return managementCertificate;
    }

    public void setManagementCertificate(String managementCertificate) {
        this.managementCertificate = managementCertificate;
    }

    public String getServiceManagementUrl() {
        return serviceManagementUrl;
    }

    public void setServiceManagementUrl(String serviceManagementUrl) {
        this.serviceManagementUrl = serviceManagementUrl;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
