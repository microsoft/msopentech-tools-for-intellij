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

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.helpers.storage.QueueExplorerFileEditorProvider;
import com.microsoftopentechnologies.intellij.model.storage.Queue;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if(getOpenedFile() == null) {


            LightVirtualFile queueVirtualFile = new LightVirtualFile(queue.getName() + " [Queue]");
            queueVirtualFile.putUserData(QueueExplorerFileEditorProvider.QUEUE_KEY, queue);
            queueVirtualFile.putUserData(QueueExplorerFileEditorProvider.STORAGE_KEY, storageAccount);

            queueVirtualFile.setFileType(new FileType() {
                @NotNull
                @Override
                public String getName() {
                    return "Queue";
                }

                @NotNull
                @Override
                public String getDescription() {
                    return "Queue";
                }

                @NotNull
                @Override
                public String getDefaultExtension() {
                    return "";
                }

                @Nullable
                @Override
                public Icon getIcon() {
                    return DefaultLoader.getUIHelper().loadIcon("container.png");
                }

                @Override
                public boolean isBinary() {
                    return true;
                }

                @Override
                public boolean isReadOnly() {
                    return false;
                }

                @Override
                public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
                    return "UTF8";
                }
            });

            FileEditorManager.getInstance(getProject()).openFile(queueVirtualFile, true, true);
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

    private VirtualFile getOpenedFile() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            Queue editedQueue = editedFile.getUserData(QueueExplorerFileEditorProvider.QUEUE_KEY);
            StorageAccount editedStorageAccount = editedFile.getUserData(QueueExplorerFileEditorProvider.STORAGE_KEY);

            if(editedStorageAccount != null
                    && editedQueue != null
                    && editedStorageAccount.getName().equals(storageAccount.getName())
                    && editedQueue.getName().equals(queue.getName())) {
                return editedFile;
            }
        }

        return null;
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

                VirtualFile openedFile = getOpenedFile();
                if(openedFile != null) {
                    DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
                    FileEditorManager.getInstance((Project) getProject()).closeFile(openedFile);
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

                            parent.removeAllChildNodes();
                            parent.load();
                        } catch (AzureCmdException ex) {
                            DefaultLoader.getUIHelper().showException("Error clearing queue", ex, "Service explorer", false, true);
                        }
                    }
                });
            }
        }
    }

}
