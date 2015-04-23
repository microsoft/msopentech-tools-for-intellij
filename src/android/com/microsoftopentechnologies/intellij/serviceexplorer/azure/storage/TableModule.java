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


import com.microsoftopentechnologies.intellij.forms.CreateTableForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.Table;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.Map;

public class TableModule extends Node {

    private static final String TABLES = "Tables";
    private static final String ACTION_CREATE = "Create new table";
    private Node parent;
    private final StorageAccount storageAccount;

    public TableModule(StorageNode parent, StorageAccount storageAccount) {
        super(TABLES + storageAccount.getName(), TABLES, parent, null, true);

        this.storageAccount = storageAccount;
        this.parent = parent;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        removeAllChildNodes();

        for (Table table : AzureSDKManagerImpl.getManager().getTables(storageAccount)) {
            addChildNode(new TableNode(this, storageAccount, table));
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {

        addAction(ACTION_CREATE, new CreateTableAction());

        return null;
    }


    public class CreateTableAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            CreateTableForm form = new CreateTableForm();

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
