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

import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.BlobContainer;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.*;

public class StorageNode extends Node {
    private static final String ACTION_CREATE = "Create blob container";
    private static final String WAIT_ICON_PATH = "storageaccount.png";
    private static final String BLOBS = "Blobs";
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

        Node blobsNode = new Node(BLOBS + storageAccount.getName(), BLOBS) {

            @Override
            protected Map<String, Class<? extends NodeActionListener>> initActions() {
                HashMap<String, Class<? extends NodeActionListener>> hashMap = new HashMap<String, Class<? extends NodeActionListener>>();
                hashMap.put(ACTION_CREATE, CreateBlobContainer.class);
                return hashMap;
            }

        };

        for (BlobContainer blobContainer : AzureSDKManagerImpl.getManager().getBlobContainers(storageAccount)) {
            blobsNode.addChildNode(new ContainerNode(this, storageAccount, blobContainer));
        }

        addChildNode(blobsNode);
    }

    public class CreateBlobContainer extends  NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {

        }
    }

}
