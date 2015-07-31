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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.windowsazure.management.storage.models.StorageAccountTypes;
import com.microsoftopentechnologies.tooling.msservices.helpers.ExternalStorageHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public class StorageModule extends AzureRefreshableNode {
    private static final String STORAGE_MODULE_ID = StorageModule.class.getName();
    private static final String ICON_PATH = "storage.png";
    private static final String BASE_MODULE_NAME = "Storage";

    public StorageModule(Node parent) {
        super(STORAGE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        removeAllChildNodes();

        // load all Storage Accounts
        List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();

        for (Subscription subscription : subscriptionList) {
            List<StorageAccount> storageAccounts = AzureManagerImpl.getManager().getStorageAccounts(subscription.getId());

            if (eventState.isEventTriggered()) {
                return;
            }

            for (StorageAccount sm : storageAccounts) {
                String type = sm.getType();

                if(type.equals(StorageAccountTypes.STANDARD_GRS)
                        || type.equals(StorageAccountTypes.STANDARD_LRS)
                        || type.equals(StorageAccountTypes.STANDARD_RAGRS)
                        || type.equals(StorageAccountTypes.STANDARD_ZRS)) {

                    addChildNode(new StorageNode(this, sm));
                }
            }
        }

        // load External Accounts
        for (ClientStorageAccount clientStorageAccount : ExternalStorageHelper.getList()) {
            ClientStorageAccount storageAccount = StorageClientSDKManagerImpl.getManager().getStorageAccount(clientStorageAccount.getConnectionString());

            if (eventState.isEventTriggered()) {
                return;
            }

            addChildNode(new ExternalStorageNode(this, storageAccount));
        }
    }
}