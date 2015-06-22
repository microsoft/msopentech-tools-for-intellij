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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.microsoftopentechnologies.intellij.forms.ExternalStorageAccountForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;

public class ConfirmDialogAction extends NodeActionListener {
    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ExternalStorageNode node = (ExternalStorageNode) e.getAction().getNode();

        final ExternalStorageAccountForm form = new ExternalStorageAccountForm();
        form.setTitle("Storage Account Key Required");
        form.setStorageAccount(node.getClientStorageAccount());

        UIHelperImpl.packAndCenterJDialog(form);

        form.setOnFinish(new Runnable() {
            @Override
            public void run() {
                node.getClientStorageAccount().setPrimaryKey(form.getPrimaryKey());
                ClientStorageAccount clientStorageAccount = StorageClientSDKManagerImpl.getManager().getStorageAccount(node.getClientStorageAccount().getConnectionString());

                node.getClientStorageAccount().setPrimaryKey(clientStorageAccount.getPrimaryKey());
                node.getClientStorageAccount().setBlobsUri(clientStorageAccount.getBlobsUri());
                node.getClientStorageAccount().setQueuesUri(clientStorageAccount.getQueuesUri());
                node.getClientStorageAccount().setTablesUri(clientStorageAccount.getTablesUri());

                node.load();
            }
        });

        form.setVisible(true);
    }
}