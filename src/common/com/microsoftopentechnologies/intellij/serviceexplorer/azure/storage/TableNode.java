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
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.Table;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import javax.swing.*;
import java.util.Map;

public class TableNode extends Node {
    private static final String TABLE_MODULE_ID = TableNode.class.getName();
    private static final String ICON_PATH = "container.png";
    private final Table table;
    private final StorageAccount storageAccount;

    public TableNode(TableModule parent, StorageAccount storageAccount, Table table) {
        super(TABLE_MODULE_ID, table.getName(), parent, ICON_PATH, true);

        this.storageAccount = storageAccount;
        this.table = table;
    }

    @Override
    protected void onNodeClick(NodeActionEvent ex) {
        if(DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, table) == null) {
            DefaultLoader.getIdeHelper().openItem(getProject(), storageAccount, table, " [Table]", "Table", "container.png");
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "View Table", ViewTable.class,
                "Delete", DeleteTable.class
        );
    }

    public class ViewTable extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(null);
        }
    }

    public class DeleteTable extends NodeActionListener {

        @Override
        public void actionPerformed(final NodeActionEvent e) {
            int optionDialog = JOptionPane.showOptionDialog(null,
                    "Are you sure you want to delete the table \"" + table.getName() + "\"?",
                    "Service explorer",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Yes", "No"},
                    null);

            if (optionDialog == JOptionPane.YES_OPTION) {

                Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, table);
                if(openedFile != null) {
                    DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
                }

                DefaultLoader.getIdeHelper().runInBackground(getProject(), "Deleting table...", false, false, null, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AzureSDKManagerImpl.getManager().deleteTable(storageAccount, table);

                            parent.removeAllChildNodes();
                            parent.load();
                        } catch (AzureCmdException ex) {
                            DefaultLoader.getUIHelper().showException("Error deleting table", ex, "Service explorer", false, true);
                        }
                    }
                });
            }
        }
    }

}
