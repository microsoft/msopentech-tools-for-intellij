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

package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage;

import com.google.common.collect.ImmutableMap;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Queue;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;

import javax.swing.*;
import java.util.Map;

public class QueueNode extends Node {
    private static final String QUEUE_MODULE_ID = QueueNode.class.getName();
    private static final String ICON_PATH = "container.png";
    private final Queue queue;
    private final StorageAccount storageAccount;

    public QueueNode(QueueModule parent, StorageAccount storageAccount, Queue queue) {
        super(QUEUE_MODULE_ID, queue.getName(), parent, ICON_PATH, true);

        this.storageAccount = storageAccount;
        this.queue = queue;
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
                "View Queue", ViewQueue.class,
                "Delete", DeleteQueue.class,
                "Clear Queue", ClearQueue.class
        );
    }

    public class ViewQueue extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(null);
        }
    }

    public class DeleteQueue extends NodeActionListener {

        @Override
        public void actionPerformed(final NodeActionEvent e) {
            int optionDialog = JOptionPane.showOptionDialog(null,
                    "Are you sure you want to delete the queue \"" + queue.getName() + "\"?",
                    "Service explorer",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Yes", "No"},
                    null);

            if (optionDialog == JOptionPane.YES_OPTION) {

                Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, queue);
                if(openedFile != null) {
                    DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
                }

                DefaultLoader.getIdeHelper().runInBackground(getProject(), "Deleting queue...", false, false, null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AzureSDKManagerImpl.getManager().deleteQueue(storageAccount, queue);

                            parent.removeAllChildNodes();
                            parent.load();
                        } catch (AzureCmdException ex) {
                            DefaultLoader.getUIHelper().showException("Error deleting queue", ex, "Service explorer", false, true);
                        }
                    }
                });
            }
        }
    }

    public class ClearQueue extends NodeActionListener {

        @Override
        public void actionPerformed(final NodeActionEvent e) {
            int optionDialog = JOptionPane.showOptionDialog(null,
                    "Are you sure you want to clear the queue \"" + queue.getName() + "\"?",
                    "Service explorer",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Yes", "No"},
                    null);

            if (optionDialog == JOptionPane.YES_OPTION) {
                DefaultLoader.getIdeHelper().runInBackground(getProject(), "Clearing queue...", false, false, null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AzureSDKManagerImpl.getManager().clearQueue(storageAccount, queue);

                            DefaultLoader.getIdeHelper().refreshQueue(getProject(), storageAccount, queue);
                        } catch (AzureCmdException ex) {
                            DefaultLoader.getUIHelper().showException("Error clearing queue", ex, "Service explorer", false, true);
                        }
                    }
                });
            }
        }
    }

}
