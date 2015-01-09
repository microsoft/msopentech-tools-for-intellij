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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VirtualMachine implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String serviceName;
    private String deploymentName;
    private String availabilitySet;
    private String dns;
    private String imageName;
    private String size;
    private String status;
    private String subscriptionId;
    private List<Endpoint> endpoints;

    public VirtualMachine(@NotNull String name, @NotNull String serviceName, @NotNull String deploymentName,
                          @NotNull String availabilitySet, @NotNull String dns, @NotNull String imageName,
                          @NotNull String size, @NotNull String status, @NotNull String subscriptionId) {
        this.name = name;
        this.serviceName = serviceName;
        this.deploymentName = deploymentName;
        this.availabilitySet = availabilitySet;
        this.dns = dns;
        this.imageName = imageName;
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

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getServiceName() {
        return serviceName;
    }

    @NotNull
    public String getDeploymentName() {
        return deploymentName;
    }

    @NotNull
    public String getAvailabilitySet() {
        return availabilitySet;
    }

    @NotNull
    public String getDns() {
        return dns;
    }

    public void setDns(@NotNull String dns) {
        this.dns = dns;
    }

    @NotNull
    public String getImageName() {
        return imageName;
    }

    @NotNull
    public String getSize() {
        return size;
    }

    public void setSize(@NotNull String size) {
        this.size = size;
    }

    @NotNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NotNull String status) {
        this.status = status;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @NotNull
    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}