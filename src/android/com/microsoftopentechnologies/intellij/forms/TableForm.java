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
package com.microsoftopentechnologies.intellij.forms;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.PermissionItem;
import com.microsoftopentechnologies.tooling.msservices.model.ms.PermissionType;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Table;
import com.microsoftopentechnologies.tooling.msservices.model.ms.TablePermissions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;


public class TableForm extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton createButton;
    private JTextField tableNameTextField;
    private JComboBox insertPermisssionComboBox;
    private JComboBox updatePermissionComboBox;
    private JComboBox deletePermissionComboBox;
    private JComboBox readPermissionComboBox;
    private String subscriptionId;
    private String serviceName;
    private Project project;
    private Table editingTable;
    private Runnable afterSave;


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setAfterSave(Runnable afterSave) {
        this.afterSave = afterSave;
    }

    private ArrayList<String> existingTableNames;

    public void setEditingTable(Table editingTable) {

        this.editingTable = editingTable;

        this.setTitle(editingTable == null ? "Create new table" : "Edit table");
        this.tableNameTextField.setText(editingTable == null ? "" : editingTable.getName());
        this.tableNameTextField.setEnabled(editingTable == null);
        this.createButton.setEnabled(editingTable != null);
        this.createButton.setText(editingTable == null ? "Create" : "Save");

        PermissionItem[] tablePermissions = PermissionItem.getTablePermissions();

        if (editingTable != null) {
            deletePermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingTable.getTablePermissions().getDelete()));
            insertPermisssionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingTable.getTablePermissions().getInsert()));
            readPermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingTable.getTablePermissions().getRead()));
            updatePermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingTable.getTablePermissions().getUpdate()));
        }
    }

    public TableForm() {
        final TableForm form = this;
        this.setResizable(false);
        this.setModal(true);
        this.setTitle("Create new table");
        this.setContentPane(mainPanel);

        createButton.setEnabled(false);

        tableNameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                super.keyTyped(keyEvent);

                createButton.setEnabled(!tableNameTextField.getText().isEmpty());
            }
        });

        final PermissionItem[] tablePermissions = PermissionItem.getTablePermissions();

        insertPermisssionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        deletePermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        updatePermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        readPermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                form.setVisible(false);
                form.dispose();
            }
        });

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TablePermissions tablePermissions = new TablePermissions();

                            tablePermissions.setDelete(((PermissionItem) deletePermissionComboBox.getSelectedItem()).getType());
                            tablePermissions.setUpdate(((PermissionItem) updatePermissionComboBox.getSelectedItem()).getType());
                            tablePermissions.setRead(((PermissionItem) readPermissionComboBox.getSelectedItem()).getType());
                            tablePermissions.setInsert(((PermissionItem) insertPermisssionComboBox.getSelectedItem()).getType());

                            final String tableName = tableNameTextField.getText().trim();

                            if (!tableName.matches("^[A-Za-z][A-Za-z0-9_]+")) {
                                JOptionPane.showMessageDialog(form, "Invalid table name. Table name must start with a letter, \n" +
                                        "contain only letters, numbers, and underscores.", "Service Explorer", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            int tableNameIndex = -1;
                            if (existingTableNames != null) {
                                tableNameIndex = Iterables.indexOf(existingTableNames, new Predicate<String>() {
                                    @Override
                                    public boolean apply(String name) {
                                        return tableName.equalsIgnoreCase(name);
                                    }
                                });
                            }

                            if (tableNameIndex != -1) {
                                JOptionPane.showMessageDialog(form, "Invalid table name. A table with that name already exists in this service.",
                                        "Error creating the table", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            if (editingTable == null) {
                                AzureManagerImpl.getManager().createTable(subscriptionId, serviceName, tableName, tablePermissions);
                            } else {
                                AzureManagerImpl.getManager().updateTable(subscriptionId, serviceName, tableName, tablePermissions);
                            }
                            if (afterSave != null)
                                afterSave.run();

                            form.setVisible(false);
                            form.dispose();
                            form.setCursor(Cursor.getDefaultCursor());

                        } catch (Throwable e) {
                            form.setCursor(Cursor.getDefaultCursor());
                            DefaultLoader.getUIHelper().showException("Error creating table", e);
                        }

                    }
                });
            }
        });
    }

    private int permissionIndex(PermissionItem[] p, PermissionType pt) {
        for (int i = 0; i < p.length; i++) {
            if (p[i].getType() == pt)
                return i;
        }
        return 0;
    }

    public void setExistingTableNames(ArrayList<String> existingTableNames) {
        this.existingTableNames = existingTableNames;
    }
}
