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
package com.microsoftopentechnologies.tooling.msservices.model.vm;

import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;


import java.util.ArrayList;
import java.util.List;

public class VirtualMachine implements ServiceTreeItem {
    public static enum Status {
        Unknown,
        Ready,
        Stopped,
        StoppedDeallocated,
        Busy,
        Creating,
        Starting,
        Stopping,
        Deleting,
        Restarting,
        Cycling,
        FailedStarting,
        Unresponsive,
        Preparing
    }

    private boolean loading;
    private String name;
    private String serviceName;
    private String deploymentName;
    private String availabilitySet;
    private String subnet;
    private String size;
    private Status status;
    private String subscriptionId;
    private List<Endpoint> endpoints;

    public VirtualMachine(String name, String serviceName, String deploymentName,
                          String availabilitySet, String subnet, String size,
                          Status status, String subscriptionId) {
        this.name = name;
        this.serviceName = serviceName;
        this.deploymentName = deploymentName;
        this.availabilitySet = availabilitySet;
        this.subnet = subnet;
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


    public String getServiceName() {
        return serviceName;
    }


    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }


    public String getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(String availabilitySet) {
        this.availabilitySet = availabilitySet;
    }


    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }


    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }


    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


    public String getSubscriptionId() {
        return subscriptionId;
    }


    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}