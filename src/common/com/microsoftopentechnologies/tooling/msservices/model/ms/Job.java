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

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class Job implements MobileServiceScriptTreeItem {

    private UUID id;
    private String appName;
    private String name;
    private boolean enabled;
    private String intervalUnit;
    private int intervalPeriod;
    private Date startTime;

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    private boolean loading;

    public static String[] getUnits() {
        return new String[] {
            "minute",
            "hour",
            "day",
            "month"
        };
    }

    public String getLocalFilePath(String serviceName) {
        String tmpdir = System.getProperty("java.io.tmpdir");
        StringBuilder sb = new StringBuilder();
        sb.append(tmpdir);

        if(!tmpdir.endsWith(File.separator))
            sb.append(File.separator);

        sb.append("TempAzure");
        sb.append(File.separator);
        sb.append(serviceName);
        sb.append(File.separator);
        sb.append("Job");
        sb.append(File.separator);
        sb.append(name);
        sb.append(".js");

        return sb.toString();
    }

    public String getLocalDirPath(String serviceName) {
        String tmpdir = System.getProperty("java.io.tmpdir");
        StringBuilder sb = new StringBuilder();
        sb.append(tmpdir);

        if(!tmpdir.endsWith(File.separator))
            sb.append(File.separator);

        sb.append("TempAzure");
        sb.append(File.separator);
        sb.append(serviceName);
        sb.append(File.separator);
        sb.append("Job");

        return sb.toString();
    }


    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(String intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public int getIntervalPeriod() {
        return intervalPeriod;
    }

    public void setIntervalPeriod(int intervalPeriod) {
        this.intervalPeriod = intervalPeriod;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }


}
