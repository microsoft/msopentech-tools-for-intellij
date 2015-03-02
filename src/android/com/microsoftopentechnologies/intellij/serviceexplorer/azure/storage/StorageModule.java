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

package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.CreateStorageAccountForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Subscription;
import com.microsoftopentechnologies.intellij.model.vm.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageModule extends Node {

    private static final String STORAGE_MODULE_ID = StorageModule.class.getName();
    private static final String ICON_PATH = "storage.png";
    private static final String BASE_MODULE_NAME = "Storage";
    private Project project;

    public StorageModule(Node parent) {
        super(STORAGE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH, true);

        project = parent.getProject();
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        // register the "manage subscriptions" action
        addAction("Create Storage Account", new CreateStorageAccountAction());
        return null;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        removeAllChildNodes();

        // load all VMs
        ArrayList<Subscription> subscriptionList = AzureRestAPIManager.getManager().getSubscriptionList();
        if(subscriptionList != null) {
            for (Subscription subscription : subscriptionList) {
                List<StorageAccount> storageAccounts = AzureSDKManagerImpl.getManager().getStorageAccounts(subscription.getId().toString());
                for (StorageAccount sm : storageAccounts) {
                    addChildNode(new StorageNode(this, sm));
                }
            }
        }

    }

    private class CreateStorageAccountAction extends NodeActionListener {

        @Override
        public void actionPerformed(NodeActionEvent e) {
            CreateStorageAccountForm createStorageAccountForm = new CreateStorageAccountForm();
            createStorageAccountForm.fillFields(null, project);
            UIHelper.packAndCenterJDialog(createStorageAccountForm);
            createStorageAccountForm.setVisible(true);
        }
    }
}
