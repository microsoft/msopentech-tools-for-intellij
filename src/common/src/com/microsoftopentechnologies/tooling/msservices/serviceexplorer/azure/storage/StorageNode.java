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
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListenerAsync;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.Callable;

public class StorageNode extends Node {
    private static final String WAIT_ICON_PATH = "storageaccount.png";
    private final StorageAccount storageAccount;

    public StorageNode(Node parent, StorageAccount sm) {
        super(sm.getName(), sm.getName(), parent, WAIT_ICON_PATH, true);
        this.storageAccount = sm;
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        this.load();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {

        removeAllChildNodes();

        Node blobsNode = new BlobModule(this, storageAccount);
        blobsNode.load();

        Node queueNode = new QueueModule(this, storageAccount);
        queueNode.load();

        Node tableNode = new TableModule(this, storageAccount);
        tableNode.load();

        addChildNode(blobsNode);
        addChildNode(queueNode);
        addChildNode(tableNode);
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Delete", new DeleteStorageAccountAction());
        return super.initActions();
    }

    public class DeleteStorageAccountAction extends NodeActionListenerAsync {
        int optionDialog;

        public DeleteStorageAccountAction() {
            super("Deleting Storage Account");
        }

        @NotNull
        @Override
        protected Callable<Boolean> beforeAsyncActionPerfomed() {
            return new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    optionDialog = JOptionPane.showOptionDialog(null,
                            "This operation will delete storage account " + storageAccount.getName() +
                                    ".\nAre you sure you want to continue?",
                            "Service explorer",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new String[]{"Yes", "No"},
                            null);

                    return (optionDialog == JOptionPane.YES_OPTION);
                }
            };
        }

        @Override
        protected void runInBackground(NodeActionEvent e) {
            try {
                final Node node = e.getAction().getNode();
                node.setLoading(true);

                AzureManagerImpl.getManager().deleteStorageAccount(storageAccount);

                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {

                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(node);
                    }
                });
            } catch (AzureCmdException ex) {
                DefaultLoader.getUIHelper().showException("Error deleting storage account", ex);
            }
        }
    }
}