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

package com.microsoftopentechnologies.intellij.model;

import java.io.File;

public class Script implements MobileServiceScriptTreeItem {

    private String operation;
    private String selfLink;
    private int bytes;
    private boolean loading;
    private String scriptName;

    public String getName() {
        return scriptName;
    }

    public void setName(String scriptName) {
        this.scriptName = scriptName;
    }



    public static String[] getOperationList() {
        return new String[]{
                "insert",
                "update",
                "delete",
                "read"
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
        sb.append(scriptName);
        sb.append(File.separator);
        sb.append(operation);
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
        sb.append(scriptName);

        return sb.toString();
    }


    @Override
    public String toString() {
        return operation + ".js"  + (loading ? " (loading...)" : "");
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public int getBytes() {
        return bytes;
    }

    public void setBytes(int bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}
