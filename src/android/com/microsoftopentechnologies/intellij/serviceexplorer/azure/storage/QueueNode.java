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

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.storage.BlobExplorerFileEditorProvider;
import com.microsoftopentechnologies.intellij.helpers.storage.QueueExplorerFileEditorProvider;
import com.microsoftopentechnologies.intellij.model.storage.BlobContainer;
import com.microsoftopentechnologies.intellij.model.storage.Queue;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
    protected void onNodeClick(NodeActionEvent e) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            Queue editedBlobContainer = editedFile.getUserData(QueueExplorerFileEditorProvider.QUEUE_KEY);
            StorageAccount editedStorageAccount = editedFile.getUserData(QueueExplorerFileEditorProvider.STORAGE_KEY);

            if(editedStorageAccount != null
                    && editedBlobContainer != null
                    && editedStorageAccount.getName().equals(storageAccount.getName())
                    && editedBlobContainer.getName().equals(queue.getName())) {
                return;
            }
        }


        LightVirtualFile queueVirtualFile = new LightVirtualFile(queue.getName() + " [Queue]");
        queueVirtualFile.putUserData(QueueExplorerFileEditorProvider.QUEUE_KEY, queue);
        queueVirtualFile.putUserData(QueueExplorerFileEditorProvider.STORAGE_KEY, storageAccount);

        queueVirtualFile.setFileType(new FileType() {
            @NotNull
            @Override
            public String getName() {
                return "BlobContainer";
            }

            @NotNull
            @Override
            public String getDescription() {
                return "BlobContainer";
            }

            @NotNull
            @Override
            public String getDefaultExtension() {
                return "";
            }

            @Nullable
            @Override
            public Icon getIcon() {
                return UIHelper.loadIcon("container.png");
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

        fileEditorManager.openFile(queueVirtualFile, true, true);

    }
}
