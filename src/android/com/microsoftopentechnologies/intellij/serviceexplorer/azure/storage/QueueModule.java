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

import com.microsoftopentechnologies.intellij.forms.CreateBlobContainerForm;
import com.microsoftopentechnologies.intellij.forms.CreateQueueForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.Queue;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.Map;

public class QueueModule extends Node {
    private static final String QUEUES = "Queues";
    private static final String ACTION_CREATE = "Create new queue";
    private Node parent;
    private final StorageAccount storageAccount;

    public QueueModule(StorageNode parent, StorageAccount storageAccount) {
        super(QUEUES + storageAccount.getName(), QUEUES, parent, null, true);

        this.storageAccount = storageAccount;
        this.parent = parent;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        removeAllChildNodes();

        for (Queue queue : AzureSDKManagerImpl.getManager().getQueues(storageAccount)) {
            addChildNode(new QueueNode(this, storageAccount, queue));
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {

        addAction(ACTION_CREATE, new CreateQueueAction());

        return null;
    }

    public class CreateQueueAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            CreateQueueForm form = new CreateQueueForm();

            form.setProject(getProject());
            form.setStorageAccount(storageAccount);

            form.setOnCreate(new Runnable() {
                @Override
                public void run() {
                    parent.removeAllChildNodes();
                    parent.load();
                }
            });

            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);

        }
    }
}
