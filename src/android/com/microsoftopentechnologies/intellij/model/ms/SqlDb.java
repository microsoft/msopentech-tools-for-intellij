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


package com.microsoftopentechnologies.intellij.model.ms;

public class SqlDb {
    private int id;
    private String name;
    private String edition;
    private SqlServer server;
    private int maxSizeGB;

    public String toString() {
        return String.format("%s - %s (%s)",name, server.getName(), server.getRegion());
    }

    public int getMaxSizeGB() {
        return maxSizeGB;
    }

    public void setMaxSizeGB(int maxSizeGB) {
        this.maxSizeGB = maxSizeGB;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SqlServer getServer() {
        return server;
    }

    public void setServer(SqlServer server) {
        this.server = server;
    }
}
