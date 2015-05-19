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

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;


import java.util.Set;
import java.util.TreeSet;

public class VirtualNetwork implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String id;
    private String location;
    private String affinityGroup;
    private Set<String> subnets;
    private String subscriptionId;

    public VirtualNetwork(@NotNull String name,
                          @NotNull String id,
                          @NotNull String location,
                          @NotNull String affinityGroup,
                          @NotNull String subscriptionId) {
        this.name = name;
        this.id = id;
        this.location = location;
        this.affinityGroup = affinityGroup;
        this.subnets = new TreeSet<String>();
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
    public String getId() {
        return id;
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
    public Set<String> getSubnets() {
        return subnets;
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