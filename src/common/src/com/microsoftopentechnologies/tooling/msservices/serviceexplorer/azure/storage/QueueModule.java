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

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Queue;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public class QueueModule extends AzureRefreshableNode {
    private static final String QUEUES = "Queues";
    final ClientStorageAccount storageAccount;

    public QueueModule(ClientStorageNode parent, ClientStorageAccount storageAccount) {
        super(QUEUES + storageAccount.getName(), QUEUES, parent, null);

        this.storageAccount = storageAccount;
        this.parent = parent;
    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        removeAllChildNodes();

        final List<Queue> queues = StorageClientSDKManagerImpl.getManager().getQueues(storageAccount);

        if (eventState.isEventTriggered()) {
            return;
        }

        for (Queue queue : queues) {
            addChildNode(new QueueNode(this, storageAccount, queue));
        }
    }

    public ClientStorageAccount getStorageAccount() {
        return storageAccount;
    }
}