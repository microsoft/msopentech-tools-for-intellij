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
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.BlobContainer;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.HashMap;
import java.util.Map;

public class BlobModule extends Node {
    private static final String BLOBS = "Blobs";
    private static final String ACTION_CREATE = "Create blob container";
    private Node parent;
    private final StorageAccount storageAccount;

    public BlobModule(Node parent, StorageAccount storageAccount) {
        super(BLOBS + storageAccount.getName(), BLOBS, parent, null, true);
        this.parent = parent;
        this.storageAccount = storageAccount;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {

        removeAllChildNodes();

        for (BlobContainer blobContainer : AzureSDKManagerImpl.getManager().getBlobContainers(storageAccount)) {
            addChildNode(new ContainerNode(this, storageAccount, blobContainer));
        }

    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction(ACTION_CREATE, new CreateBlobContainer());
        return null;
    }

    public class CreateBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            CreateBlobContainerForm form = new CreateBlobContainerForm();

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
