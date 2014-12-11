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

package com.microsoftopentechnologies.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice.MobileServiceModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.VMServiceModule;

public class AzureServiceModule extends Node {
    private static final String AZURE_SERVICE_MODULE_ID = AzureServiceModule.class.getName();
    private static final String ICON_PATH = "azure.png";
    private static final String BASE_MODULE_NAME = "Azure";

    private Project project;
    private MobileServiceModule mobileServiceModule = new MobileServiceModule(this);
    private VMServiceModule vmServiceModule = new VMServiceModule(this);

    public AzureServiceModule(Project project) {
        this(null, ICON_PATH, null);
        this.project = project;
    }

    public AzureServiceModule(Node parent, String iconPath, Object data) {
        super(AZURE_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, iconPath, true);
    }

    @Override
    protected void refreshItems() {
        // add the mobile service module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item
        if(!isDirectChild(mobileServiceModule)) {
            addChildNode(mobileServiceModule);
        }
        mobileServiceModule.load();

        // add the VM service module
        if(!isDirectChild(vmServiceModule)) {
            addChildNode(vmServiceModule);
        }
        vmServiceModule.load();
    }

    @Override
    public Project getProject() {
        return project;
    }
}
