/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.ui;

import com.microsoftopentechnologies.intellij.util.MethodUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoftopentechnologies.intellij.AzureSettings;
import com.microsoftopentechnologies.preference.StorageAccPrefPageTableElement;
import com.microsoftopentechnologies.preference.StorageAccPrefPageTableElements;
import com.microsoftopentechnologies.storageregistry.StorageAccount;
import com.microsoftopentechnologies.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.storageregistry.StorageRegistryUtilMethods;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class StorageAccountPanel implements AzureAbstractPanel {
    private static final String DISPLAY_NAME = "Storage Accounts";
    private JPanel contentPane;

    private JTable accountsTable;
    private JButton importButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private Project myProject;

    public StorageAccountPanel(Project project) {
        this.myProject = project;
        init();
    }

    protected void init() {
        accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountsTable.setModel(new StorageAccountTableModel(getTableContent()));
        for (int i = 0; i < accountsTable.getColumnModel().getColumnCount(); i++) {
            TableColumn each = accountsTable.getColumnModel().getColumn(i);
            each.setPreferredWidth(StorageAccountTableModel.getColumnWidth(i, 450));
        }
        importButton.addActionListener(createImportSubscriptionAction());
        addButton.addActionListener(createAddButtonListener());
        editButton.addActionListener(createEditButtonListener());
        removeButton.addActionListener(createRemoveButtonListener());
        editButton.setEnabled(false);
        removeButton.setEnabled(false);
        accountsTable.getSelectionModel().addListSelectionListener(createAccountsTableListener());
        if (!AzureSettings.getSafeInstance(myProject).isSubscriptionLoaded()) {
            MethodUtils.loadSubInfoFirstTime(myProject);
            ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
            ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
        }
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    private ActionListener createImportSubscriptionAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ImportSubscriptionDialog importSubscriptionDialog = new ImportSubscriptionDialog();
                importSubscriptionDialog.show();
                if (importSubscriptionDialog.isOK()) {
                    String fileName = importSubscriptionDialog.getPublishSettingsPath();
                    MethodUtils.handleFile(fileName, myProject);
                    ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
                    ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    private ActionListener createAddButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditStorageAccountDialog editStorageAccountDialog = new EditStorageAccountDialog(null, myProject);
                editStorageAccountDialog.show();
                if (editStorageAccountDialog.isOK()) {
                    AzureSettings.getSafeInstance(myProject).saveStorage();
                    ((StorageAccountTableModel)accountsTable.getModel()).setAccounts(getTableContent());
                    ((StorageAccountTableModel)accountsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    private ActionListener createEditButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = accountsTable.getSelectedRow();
                StorageAccount accToEdit = StorageAccountRegistry.getStrgList().get(index);
                EditStorageAccountDialog dlg = new EditStorageAccountDialog(accToEdit, myProject);
                dlg.show();
                if (dlg.isOK()) {
                    AzureSettings.getSafeInstance(myProject).saveStorage();
                }
            }
        };
    }

    private ActionListener createRemoveButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int curSelIndex = accountsTable.getSelectedRow();
                if (curSelIndex > -1) {
                    int choice = Messages.showOkCancelDialog(message("accRmvMsg"), message("accRmvTtl"), Messages.getQuestionIcon());
                    if (choice == Messages.OK) {
                        StorageAccountRegistry.getStrgList().remove(curSelIndex);
                        AzureSettings.getSafeInstance(myProject).saveStorage();
                        ((StorageAccountTableModel) accountsTable.getModel()).setAccounts(getTableContent());
                        ((StorageAccountTableModel) accountsTable.getModel()).fireTableDataChanged();
                    }
                }
            }
        };
    }

    private ListSelectionListener createAccountsTableListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean buttonsEnabled = accountsTable.getSelectedRow() > -1;
                editButton.setEnabled(buttonsEnabled);
                removeButton.setEnabled(buttonsEnabled);
            }
        };
    }

    /**
     * Method prepares storage account list to show in table.
     */
    private List<StorageAccPrefPageTableElement> getTableContent() {
        // loads data from preference file.
        AzureSettings.getSafeInstance(myProject).loadStorage();
        List<StorageAccount> strgList = StorageAccountRegistry.getStrgList();
        List<StorageAccPrefPageTableElement> tableRowElements = new ArrayList<StorageAccPrefPageTableElement>();
        for (StorageAccount storageAcc : strgList) {
            if (storageAcc != null) {
                StorageAccPrefPageTableElement ele = new StorageAccPrefPageTableElement();
                ele.setStorageName(storageAcc.getStrgName());
                ele.setStorageUrl(storageAcc.getStrgUrl());
                tableRowElements.add(ele);
            }
        }
        StorageAccPrefPageTableElements elements = new StorageAccPrefPageTableElements();
        elements.setElements(tableRowElements);
        return elements.getElements();
    }

    public String getSelectedValue() {
        int selectedIndex = accountsTable.getSelectedRow();
        if (selectedIndex >= 0) {
            return ((StorageAccountTableModel) accountsTable.getModel()).getAccountNameAtIndex(accountsTable.getSelectedRow());
        }
        return null;
    }

    private static class StorageAccountTableModel extends AbstractTableModel {
        public static final String[] COLUMNS = new String[]{"Name", "Service Endpoint"};
        private java.util.List<StorageAccPrefPageTableElement> accounts;

        public StorageAccountTableModel(List<StorageAccPrefPageTableElement> accounts) {
            this.accounts = accounts;
        }

        public void setAccounts(List<StorageAccPrefPageTableElement> accounts) {
            this.accounts = accounts;
        }

        public String getAccountNameAtIndex(int index) {
            return accounts.get(index).getStorageName();
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public static int getColumnWidth(int column, int totalWidth) {
            switch (column) {
                case 0:
                    return (int) (totalWidth * 0.4);
                default:
                    return (int) (totalWidth * 0.6);
            }
        }

        public int getRowCount() {
            return accounts.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            StorageAccPrefPageTableElement account = accounts.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return account.getStorageName();
                case 1:
                    return StorageRegistryUtilMethods.getServiceEndpoint(account.getStorageUrl());
            }
            return null;
        }
    }
}
