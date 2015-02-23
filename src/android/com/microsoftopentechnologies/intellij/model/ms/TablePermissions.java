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

public class TablePermissions {
    private PermissionType insert;
    private PermissionType read;
    private PermissionType update;
    private PermissionType delete;

    public PermissionType getInsert() {
        return insert;
    }

    public void setInsert(PermissionType insert) {
        this.insert = insert;
    }

    public PermissionType getRead() {
        return read;
    }

    public void setRead(PermissionType read) {
        this.read = read;
    }

    public PermissionType getUpdate() {
        return update;
    }

    public void setUpdate(PermissionType update) {
        this.update = update;
    }

    public PermissionType getDelete() {
        return delete;
    }

    public void setDelete(PermissionType delete) {
        this.delete = delete;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert=");
        sb.append(PermissionItem.getPermitionString(insert));
        sb.append(",");
        sb.append("read=");
        sb.append(PermissionItem.getPermitionString(read));
        sb.append(",");
        sb.append("update=");
        sb.append(PermissionItem.getPermitionString(update));
        sb.append(",");
        sb.append("delete=");
        sb.append(PermissionItem.getPermitionString(delete));

        return sb.toString();
    }
}
