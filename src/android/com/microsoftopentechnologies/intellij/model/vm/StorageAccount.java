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

import com.microsoftopentechnologies.intellij.model.ms.ServiceTreeItem;
import org.jetbrains.annotations.NotNull;

public class StorageAccount implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String type;
    private String location;
    private String affinityGroup;
    private String key;
    private String subscriptionId;

    public StorageAccount(@NotNull String name,
                          @NotNull String type,
                          @NotNull String location,
                          @NotNull String affinityGroup,
                          @NotNull String key,
                          @NotNull String subscriptionId) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.affinityGroup = affinityGroup;
        this.key = key;
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
    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
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
    public String getKey() {
        return key;
    }

    public void setKey(@NotNull String key) {
        this.key = key;
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