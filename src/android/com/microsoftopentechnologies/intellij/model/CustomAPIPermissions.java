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

public class CustomAPIPermissions {
    private PermissionType getPermission;
    private PermissionType postPermission;
    private PermissionType putPermission;
    private PermissionType patchPermission;
    private PermissionType deletePermission;

    public PermissionType getGetPermission() {
        return getPermission;
    }

    public PermissionType getPostPermission() {
        return postPermission;
    }

    public PermissionType getPutPermission() {
        return putPermission;
    }

    public PermissionType getPatchPermission() {
        return patchPermission;
    }

    public PermissionType getDeletePermission() {
        return deletePermission;
    }


    public void setGetPermission(PermissionType getPermission) {
        this.getPermission = getPermission;
    }

    public void setPostPermission(PermissionType postPermission) {
        this.postPermission = postPermission;
    }

    public void setPutPermission(PermissionType putPermission) {
        this.putPermission = putPermission;
    }

    public void setPatchPermission(PermissionType patchPermission) {
        this.patchPermission = patchPermission;
    }

    public void setDeletePermission(PermissionType deletePermission) {
        this.deletePermission = deletePermission;
    }






    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("get=");
        sb.append(PermissionItem.getPermitionString(getPermission));
        sb.append(",");
        sb.append("put=");
        sb.append(PermissionItem.getPermitionString(putPermission));
        sb.append(",");
        sb.append("post=");
        sb.append(PermissionItem.getPermitionString(postPermission));
        sb.append(",");
        sb.append("patch=");
        sb.append(PermissionItem.getPermitionString(patchPermission));
        sb.append(",");
        sb.append("delete=");
        sb.append(PermissionItem.getPermitionString(deletePermission));

        return sb.toString();
    }


}
