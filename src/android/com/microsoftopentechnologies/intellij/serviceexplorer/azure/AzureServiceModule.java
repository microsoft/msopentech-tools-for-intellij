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
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice.MobileServiceModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.StorageModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.VMServiceModule;

import java.util.Map;

public class AzureServiceModule extends Node {
    private static final String AZURE_SERVICE_MODULE_ID = AzureServiceModule.class.getName();
    private static final String ICON_PATH = "azure.png";
    private static final String BASE_MODULE_NAME = "Azure";

    private Project project;
    private MobileServiceModule mobileServiceModule = new MobileServiceModule(this);
    private VMServiceModule vmServiceModule = new VMServiceModule(this);
    private StorageModule storageServiceModule = new StorageModule(this);

    public AzureServiceModule(Project project) {
        this(null, ICON_PATH, null);
        this.project = project;
    }

    public AzureServiceModule(Node parent, String iconPath, Object data) {
        super(AZURE_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, iconPath, true);
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        // register the "manage subscriptions" action
        addAction("Manage Subscriptions", new ManageSubscriptionsAction());
        return null;
    }

    public class ManageSubscriptionsAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            ManageSubscriptionForm form = new ManageSubscriptionForm(getProject());
            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);
            load();
        }
    }

    @Override
    protected void refreshItems() {
        // add the mobile service module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item

        if(!mobileServiceModule.isLoading()) {
            if (!isDirectChild(mobileServiceModule)) {
                addChildNode(mobileServiceModule);
            }
            mobileServiceModule.load();
        }

        if(!vmServiceModule.isLoading()) {
            if (!isDirectChild(vmServiceModule)) {
                addChildNode(vmServiceModule);
            }
            vmServiceModule.load();
        }


        if(!storageServiceModule.isLoading()) {
            if (!isDirectChild(storageServiceModule)) {
                addChildNode(storageServiceModule);
            }
            storageServiceModule.load();
        }
    }

    @Override
    public Project getProject() {
        return project;
    }
}
