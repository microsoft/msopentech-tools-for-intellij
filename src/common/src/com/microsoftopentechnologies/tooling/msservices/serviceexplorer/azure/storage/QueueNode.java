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

import com.google.common.collect.ImmutableMap;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Queue;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.Map;

public class QueueNode extends Node {
    public class RefreshAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getIdeHelper().refreshQueue(getProject(), storageAccount, queue);
        }
    }

    public class ViewQueue extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    public class DeleteQueue extends AzureNodeActionPromptListener {
        public DeleteQueue() {
            super(QueueNode.this,
                    String.format("Are you sure you want to delete the queue \"%s\"?", queue.getName()),
                    "Deleting Queue");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, queue);

            if (openedFile != null) {
                DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
            }

            try {
                StorageClientSDKManagerImpl.getManager().deleteQueue(storageAccount, queue);

                parent.removeAllChildNodes();
                ((QueueModule) parent).load();
            } catch (AzureCmdException ex) {
                DefaultLoader.getUIHelper().showException("Error deleting queue", ex, "Service explorer", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    public class ClearQueue extends AzureNodeActionPromptListener {
        public ClearQueue() {
            super(QueueNode.this,
                    String.format("Are you sure you want to clear the queue \"%s\"?", queue.getName()),
                    "Clearing Queue");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            try {
                StorageClientSDKManagerImpl.getManager().clearQueue(storageAccount, queue);

                DefaultLoader.getIdeHelper().refreshQueue(getProject(), storageAccount, queue);
            } catch (AzureCmdException ex) {
                DefaultLoader.getUIHelper().showException("Error clearing queue", ex, "Service explorer", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    private static final String QUEUE_MODULE_ID = QueueNode.class.getName();
    private static final String ICON_PATH = "container.png";
    private final Queue queue;
    private final ClientStorageAccount storageAccount;

    public QueueNode(QueueModule parent, ClientStorageAccount storageAccount, Queue queue) {
        super(QUEUE_MODULE_ID, queue.getName(), parent, ICON_PATH, true);

        this.storageAccount = storageAccount;
        this.queue = queue;

        loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent ex) {
        final Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, queue);

        if (openedFile == null) {
            DefaultLoader.getIdeHelper().openItem(getProject(), storageAccount, queue, " [Queue]", "Queue", "container.png");
        } else {
            DefaultLoader.getIdeHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Refresh", RefreshAction.class,
                "View Queue", ViewQueue.class,
                "Delete", DeleteQueue.class,
                "Clear Queue", ClearQueue.class
        );
    }
}