/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.CreateStorageAccountForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Subscription;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
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
        ArrayList<Subscription> subscriptionList = AzureRestAPIManagerImpl.getManager().getSubscriptionList();
        if (subscriptionList != null) {
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
            // check if we have a valid subscription handy
            AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();

            if (apiManager.getAuthenticationMode() == AzureAuthenticationMode.Unknown) {
                UIHelper.showException("Please configure an Azure subscription by right-clicking on the \"Azure\" " +
                        "node and selecting \"Manage subscriptions\".", null, "No Azure subscription found");
                return;
            }

            try {
                ArrayList<Subscription> subscriptions = apiManager.getSubscriptionList();

                if (subscriptions == null || subscriptions.isEmpty()) {
                    UIHelper.showException("No active Azure subscription was found. Please enable one more Azure " +
                                    "subscriptions by right-clicking on the \"Azure\" " +
                                    "node and selecting \"Manage subscriptions\".",
                            null, "No active Azure subscription found");
                    return;
                }
            } catch (AzureCmdException e1) {
                UIHelper.showException("An error occurred while creating the storage account.", e1);
            }

            CreateStorageAccountForm createStorageAccountForm = new CreateStorageAccountForm();
            createStorageAccountForm.fillFields(null, project);
            UIHelper.packAndCenterJDialog(createStorageAccountForm);

            createStorageAccountForm.setOnCreate(new Runnable() {
                @Override
                public void run() {
                    try {
                        refreshItems();
                    } catch (AzureCmdException ex) {
                        UIHelper.showException("Error refreshing storage accounts", ex, "Service Explorer", false, true);
                    }
                }
            });

            createStorageAccountForm.setVisible(true);
        }
    }
}
