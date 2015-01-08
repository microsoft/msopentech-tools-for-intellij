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

import java.util.Set;
import java.util.TreeSet;

public class StorageAccount implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String type;
    private String location;
    private String affinityGroup;
    private String subscriptionId;
    private Set<String> availabilitySets;

    public StorageAccount(@NotNull String name,
                          @NotNull String type,
                          @NotNull String location,
                          @NotNull String affinityGroup,
                          @NotNull String subscriptionId) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.affinityGroup = affinityGroup;
        this.subscriptionId = subscriptionId;
        this.availabilitySets = new TreeSet<String>();
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

    @NotNull
    public String getLocation() {
        return location;
    }

    @NotNull
    public String getAffinityGroup() {
        return affinityGroup;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @NotNull
    public Set<String> getAvailabilitySets() {
        return availabilitySets;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}