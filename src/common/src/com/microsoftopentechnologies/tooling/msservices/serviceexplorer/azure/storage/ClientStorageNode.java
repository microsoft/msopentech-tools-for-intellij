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
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

public abstract class ClientStorageNode extends AzureRefreshableNode {
    protected final ClientStorageAccount clientStorageAccount;

    public ClientStorageNode(String id, String name, Node parent, String iconPath, ClientStorageAccount sm) {
        super(id, name, parent, iconPath);
        this.clientStorageAccount = sm;
    }

    public ClientStorageNode(String id, String name, Node parent, String iconPath, ClientStorageAccount sm, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
        this.clientStorageAccount = sm;
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        this.load();
    }

    public ClientStorageAccount getClientStorageAccount() {
        return clientStorageAccount;
    }

    protected void fillChildren(@NotNull EventStateHandle eventState) {
        BlobModule blobsNode = new BlobModule(this, clientStorageAccount);
        blobsNode.load();

        if (eventState.isEventTriggered()) {
            return;
        }

        addChildNode(blobsNode);

        QueueModule queueNode = new QueueModule(this, clientStorageAccount);
        queueNode.load();

        if (eventState.isEventTriggered()) {
            return;
        }

        addChildNode(queueNode);

        TableModule tableNode = new TableModule(this, clientStorageAccount);
        tableNode.load();

        if (eventState.isEventTriggered()) {
            return;
        }

        addChildNode(tableNode);
    }
}