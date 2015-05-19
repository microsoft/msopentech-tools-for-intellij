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

import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.ExternalStorageHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.Map;

public class ExternalStorageNode extends Node {
    private static final String WAIT_ICON_PATH = "storageaccount.png";
    private final ClientStorageAccount storageAccount;

    public ExternalStorageNode(Node parent, ClientStorageAccount sm) {
        super(sm.getName(), sm.getName(), parent, WAIT_ICON_PATH, true);
        this.storageAccount = sm;
    }

    public ClientStorageAccount getStorageAccount() {
        return storageAccount;
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        this.load();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        removeAllChildNodes();

        if (storageAccount.getPrimaryKey().isEmpty()) {
            try {
                NodeActionListener listener = DefaultLoader.getActions(this.getClass()).get(0).getConstructor().newInstance();
                listener.actionPerformed(new NodeActionEvent(new NodeAction(this, this.getName())));
            } catch (Exception e) {
                throw new AzureCmdException("Error opening external storage", e);
            }
        } else {

            fillChildren();
        }
    }

    public void fillChildren() {
        Node blobsNode = new BlobModule(this, storageAccount);
        blobsNode.load();
        addChildNode(blobsNode);

        Node queueNode = new QueueModule(this, storageAccount);
        queueNode.load();
        addChildNode(queueNode);

        Node tableNode = new TableModule(this, storageAccount);
        tableNode.load();
        addChildNode(tableNode);
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Detach", new DetachAction());

        return super.initActions();
    }

    private class DetachAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            Node node = e.getAction().getNode();
            node.getParent().removeDirectChildNode(node);

            ExternalStorageHelper.detach(storageAccount);
        }
    }
}