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

import java.util.ArrayList;

public class PermissionItem {
    public PermissionItem(PermissionType type, String description) {
        this.description = description;
        this.type = type;
    }

    private String description;
    private PermissionType type;

    public PermissionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    public static PermissionItem[] getTablePermissions() {
        ArrayList<PermissionItem> pilist = new ArrayList<PermissionItem>();
        pilist.add(new PermissionItem(PermissionType.Application, "Anybody with an application key"));
        pilist.add(new PermissionItem(PermissionType.User, "Only authenticated users"));
        pilist.add(new PermissionItem(PermissionType.Admin, "Only scripts and admins"));
        pilist.add(new PermissionItem(PermissionType.Public, "Everyone"));

        PermissionItem[] res = new PermissionItem[pilist.size()];
        return pilist.toArray(res);
    }

    public static String getPermitionString(PermissionType type){
        switch (type) {
            case Admin:
                return "admin";
            case Application:
                return "application";
            case Public:
                return "public";
            case User:
                return "user";
        }
        return null;
    }

    public static PermissionType getPermitionType(String type){
        if(type.equals("admin"))
            return PermissionType.Admin;
        else if(type.equals("application"))
            return PermissionType.Application;
        else if(type.equals("public"))
            return PermissionType.Public;
        else if(type.equals("user"))
            return PermissionType.User;

        return null;
    }
}
