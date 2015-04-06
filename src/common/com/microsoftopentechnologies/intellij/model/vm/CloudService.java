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


import java.util.Set;
import java.util.TreeSet;

public class CloudService implements ServiceTreeItem {
    public static class Deployment {
        private String name;
        private String slot;
        private Set<String> virtualMachines;
        private Set<String> computeRoles;
        private String virtualNetwork;
        private Set<String> availabilitySets;

        private Deployment(String slot) {
            this.slot = slot;
            this.name = "";
            this.virtualNetwork = "";
            this.virtualMachines = new TreeSet<String>();
            this.computeRoles = new TreeSet<String>();
            this.availabilitySets = new TreeSet<String>();
        }


        public String getSlot() {
            return slot;
        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }


        public Set<String> getVirtualMachines() {
            return virtualMachines;
        }


        public Set<String> getComputeRoles() {
            return computeRoles;
        }


        public String getVirtualNetwork() {
            return virtualNetwork;
        }

        public void setVirtualNetwork(String virtualNetwork) {
            this.virtualNetwork = virtualNetwork;
        }


        public Set<String> getAvailabilitySets() {
            return availabilitySets;
        }
    }

    private boolean loading;
    private String name;
    private String location;
    private String affinityGroup;
    private Deployment productionDeployment;
    private Deployment stagingDeployment;
    private String subscriptionId;

    public CloudService(String name,
                        String location,
                        String affinityGroup,
                        String subscriptionId) {
        this.name = name;
        this.location = location;
        this.affinityGroup = affinityGroup;
        this.productionDeployment = new Deployment("Production");
        this.stagingDeployment = new Deployment("Staging");
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


    public String getLocation() {
        return location;
    }


    public String getAffinityGroup() {
        return affinityGroup;
    }


    public Deployment getProductionDeployment() {
        return productionDeployment;
    }


    public Deployment getStagingDeployment() {
        return stagingDeployment;
    }


    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}