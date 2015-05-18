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
import com.microsoftopentechnologies.tooling.msservices.model.storage.BlobContainer;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;

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
                final Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, blobContainer);
                if (openedFile == null) {
                    DefaultLoader.getIdeHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", "container.png");
                } else {
                    DefaultLoader.getIdeHelper().openItem(getProject(), openedFile);
                }
            }
        });

    }


    @Override
    public void addAction(NodeAction action) {
        super.addAction(action);
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {

        return ImmutableMap.of(
                "Delete", DeleteBlobContainer.class,
                "View Blob Container", ViewBlobContainer.class);
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            if (DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, blobContainer) == null) {
                DefaultLoader.getIdeHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", "container.png");
            }
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
                Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, blobContainer);
                if(openedFile != null) {
                    DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
                }
                DefaultLoader.getIdeHelper().runInBackground(getProject(), "Creating blob container...", false, false, null, new Runnable(){
                    @Override
                    public void run() {
                        try {
                            AzureSDKManagerImpl.getManager().deleteBlobContainer(storageAccount, blobContainer);

                            parent.removeAllChildNodes();
                            parent.load();
                        } catch (AzureCmdException ex) {
                            DefaultLoader.getUIHelper().showException("Error deleting blob storage", ex, "Service explorer", false, true);
                        }
                    }
                });
            }
        }
    }
}
