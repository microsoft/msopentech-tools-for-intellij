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

import java.io.File;


public class CustomAPI implements MobileServiceScriptTreeItem{
    private String name;
    private boolean loading;
    private CustomAPIPermissions customAPIPermissions;


    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
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
        sb.append("CustomAPI");
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
        sb.append("CustomAPI");

        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public CustomAPIPermissions getCustomAPIPermissions() {
        return customAPIPermissions;
    }

    public void setCustomAPIPermissions(CustomAPIPermissions customAPIPermissions) {
        this.customAPIPermissions = customAPIPermissions;
    }
}
