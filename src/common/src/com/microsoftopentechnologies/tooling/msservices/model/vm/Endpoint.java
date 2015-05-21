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


public class Endpoint implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String protocol;
    private int privatePort;
    private int publicPort;

    public Endpoint(@NotNull String name, @NotNull String protocol, int privatePort, int publicPort) {
        this.name = name;
        this.protocol = protocol;
        this.privatePort = privatePort;
        this.publicPort = publicPort;
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
    public String getProtocol() {
        return protocol;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    public int getPublicPort() {
        return publicPort;
    }

    @Override
    public String toString(){
        return name + (loading ? " (loading...)" : "");
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setPrivatePort(int privatePort) {
        this.privatePort = privatePort;
    }

    public void setPublicPort(int publicPort) {
        this.publicPort = publicPort;
    }
}