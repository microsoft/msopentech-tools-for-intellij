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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.helpers.storage.BlobExplorerFileEditorProvider;
import com.microsoftopentechnologies.intellij.model.storage.BlobContainer;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class ContainerNode extends Node {

    private static final String CONTAINER_MODULE_ID = ContainerNode.class.getName();
    private static final String ICON_PATH = "container.png";

    private final BlobContainer blobContainer;
    private final StorageAccount storageAccount;

    public ContainerNode(final Node parent, StorageAccount sa, BlobContainer bc) {
        super(CONTAINER_MODULE_ID, bc.getName(), parent, ICON_PATH, true);

        blobContainer = bc;
        storageAccount = sa;

        addClickActionListener(new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                openContainer();
            }
        });

    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {

        return ImmutableMap.of(
                "Delete", DeleteBlobContainer.class,
                "View Blob Container", ViewBlobContainer.class);
    }

    private void openContainer() {

        if(getOpenedFile() == null) {

            LightVirtualFile containerVirtualFile = new LightVirtualFile(blobContainer.getName() + " [Container]");
            containerVirtualFile.putUserData(BlobExplorerFileEditorProvider.CONTAINER_KEY, blobContainer);
            containerVirtualFile.putUserData(BlobExplorerFileEditorProvider.STORAGE_KEY, storageAccount);

            containerVirtualFile.setFileType(new FileType() {
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

            FileEditorManager.getInstance(getProject()).openFile(containerVirtualFile, true, true);
        }


    }

    private VirtualFile getOpenedFile() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            BlobContainer editedBlobContainer = editedFile.getUserData(BlobExplorerFileEditorProvider.CONTAINER_KEY);
            StorageAccount editedStorageAccount = editedFile.getUserData(BlobExplorerFileEditorProvider.STORAGE_KEY);

            if(editedStorageAccount != null
                    && editedBlobContainer != null
                    && editedStorageAccount.getName().equals(storageAccount.getName())
                    && editedBlobContainer.getName().equals(blobContainer.getName())) {
                return editedFile;
            }
        }

        return null;
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            openContainer();
        }
    }

    public class DeleteBlobContainer extends NodeActionListener {

        @Override
        public void actionPerformed(final NodeActionEvent e) {
            int optionDialog = JOptionPane.showOptionDialog(null,
                "Are you sure you want to delete the blob container \"" + blobContainer.getName() + "\"?",
                "Service explorer",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Yes", "No"},
                null);

            if (optionDialog == JOptionPane.YES_OPTION) {

                VirtualFile openedFile = getOpenedFile();
                if(openedFile != null) {
                    FileEditorManager.getInstance(getProject()).closeFile(openedFile);
                }

                ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Creating blob container...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            AzureSDKManagerImpl.getManager().deleteBlobContainer(storageAccount, blobContainer);

                            parent.removeAllChildNodes();
                            parent.load();
                        } catch (AzureCmdException ex) {
                            UIHelper.showException("Error deleting blob storage", ex, "Service explorer", false, true);
                        }
                    }
                });

            }
        }
    }
}
