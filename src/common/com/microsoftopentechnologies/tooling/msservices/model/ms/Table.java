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

package com.microsoftopentechnologies.tooling.msservices.model.ms;

import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;

import java.util.ArrayList;

public class Table implements ServiceTreeItem {
    public Table() {
        columns = new ArrayList<Column>();
        scripts = new ArrayList<Script>();
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }

    private boolean loading;
    private String idType;
    private String name;
    private String selfLink;
    private ArrayList<Column> columns;
    private ArrayList<Script> scripts;
    private TablePermissions tablePermissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public ArrayList<Column> getColumns() {
        return columns;
    }

    public ArrayList<Script> getScripts() {
        return scripts;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public TablePermissions getTablePermissions() {
        return tablePermissions;
    }

    public void setTablePermissions(TablePermissions tablePermissions) {
        this.tablePermissions = tablePermissions;
    }
}
