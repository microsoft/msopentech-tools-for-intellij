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
package com.microsoftopentechnologies.intellij.model.vm;

import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;

import java.util.ArrayList;
import java.util.List;

public class VirtualMachine implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String serviceName;
    private String dns;
    private String environment;
    private String size;
    private String status;
    private String subscriptionId;
    private List<Endpoint> endpoints;

    public VirtualMachine(String name, String serviceName, String dns, String environment, String size, String status, String subscriptionId) {
        this.name = name;
        this.serviceName = serviceName;
        this.dns = dns;
        this.environment = environment;
        this.size = size;
        this.status = status;
        this.subscriptionId = subscriptionId;
        this.endpoints = new ArrayList<Endpoint>();
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

    public String getDns() {
        return dns;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getSize() {
        return size;
    }

    public String getStatus() {
        return status;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public String getServiceName() {
        return serviceName;
    }
}