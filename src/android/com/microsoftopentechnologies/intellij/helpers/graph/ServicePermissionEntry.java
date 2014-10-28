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

package com.microsoftopentechnologies.intellij.helpers.graph;

import com.microsoftopentechnologies.intellij.model.Office365PermissionList;
import com.microsoftopentechnologies.intellij.model.Office365Service;

import java.util.Map;

public class ServicePermissionEntry implements Map.Entry<Office365Service, Office365PermissionList> {
    private Office365Service service;
    private Office365PermissionList permissionSet;

    public ServicePermissionEntry(Office365Service service, Office365PermissionList permissionSet) {
        this.service = service;
        this.permissionSet = permissionSet;
    }

    @Override
    public Office365Service getKey() {
        return service;
    }

    @Override
    public Office365PermissionList getValue() {
        return permissionSet;
    }

    @Override
    public Office365PermissionList setValue(Office365PermissionList value) {
        Office365PermissionList old = permissionSet;
        permissionSet = value;
        return old;
    }
}