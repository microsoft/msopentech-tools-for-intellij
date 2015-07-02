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
package com.microsoftopentechnologies.tooling.msservices.model.ms;

import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;

import java.util.ArrayList;

public class MobileService implements ServiceTreeItem {
    public static final String NODE_RUNTIME = "JavaScript";
    public static final String NET_RUNTIME = ".NET Framework";

    private boolean loading;

    public MobileService() {
        tables = new ArrayList<Table>();
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }

    private String name;
    private String type;
    private String state;
    private String selfLink;
    private String appUrl;
    private String appKey;
    private String masterKey;
    private ArrayList<Table> tables;
    private String webspace;
    private String region;
    private String mgmtPortalLink;
    private String subcriptionId;
    private String runtime;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public ArrayList<Table> getTables() {
        return tables;
    }

    public String getWebspace() {
        return webspace;
    }

    public String getRegion() {
        return region;
    }

    public String getMgmtPortalLink() {
        return mgmtPortalLink;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public void setWebspace(String webspace) {
        this.webspace = webspace;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setMgmtPortalLink(String mgmtPortalLink) {
        this.mgmtPortalLink = mgmtPortalLink;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getSubcriptionId() {
        return subcriptionId;
    }

    public void setSubcriptionId(String subcriptionId) {
        this.subcriptionId = subcriptionId;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }
}