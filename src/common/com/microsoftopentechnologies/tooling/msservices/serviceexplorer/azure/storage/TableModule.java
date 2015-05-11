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

import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Table;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;

public class TableModule extends Node {

    private static final String TABLES = "Tables";
    final StorageAccount storageAccount;

    public TableModule(StorageNode parent, StorageAccount storageAccount) {
        super(TABLES + storageAccount.getName(), TABLES, parent, null, true);

        this.storageAccount = storageAccount;
        this.parent = parent;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        removeAllChildNodes();

        for (Table table : AzureSDKManagerImpl.getManager().getTables(storageAccount)) {
            addChildNode(new TableNode(this, storageAccount, table));
        }
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }
}
