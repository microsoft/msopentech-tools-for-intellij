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

package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.CustomAPI;
import com.microsoftopentechnologies.intellij.model.ms.CustomAPIPermissions;
import com.microsoftopentechnologies.intellij.model.ms.PermissionItem;
import com.microsoftopentechnologies.intellij.model.ms.PermissionType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomAPIForm extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton createButton;
    private JTextField tableNameTextField;
    private JComboBox getPermissionComboBox;
    private JComboBox postPermissionComboBox;
    private JComboBox putPermissionComboBox;
    private JComboBox patchPermissionComboBox;
    private JComboBox deletePermissionComboBox;
    private UUID subscriptionId;
    private String serviceName;
    private Project project;
    private CustomAPI editingCustomAPI;
    private Runnable afterSave;
    private List<String> existingApiNames;

    public Project getProject() {
        return project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public CustomAPIForm() {
        final CustomAPIForm form = this;

        this.setResizable(false);
        this.setModal(true);
        this.setTitle("Create new custom API");
        this.setContentPane(mainPanel);
        createButton.setEnabled(false);


        final PermissionItem[] tablePermissions = PermissionItem.getTablePermissions();

        getPermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        postPermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        putPermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        patchPermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));
        deletePermissionComboBox.setModel(new DefaultComboBoxModel(tablePermissions));

        tableNameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                super.keyTyped(keyEvent);

                createButton.setEnabled(!tableNameTextField.getText().isEmpty());
            }
        });

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

                        String apiName = tableNameTextField.getText().trim();

                        CustomAPIPermissions permissions = new CustomAPIPermissions();
                        permissions.setPatchPermission(((PermissionItem) patchPermissionComboBox.getSelectedItem()).getType());
                        permissions.setDeletePermission(((PermissionItem) deletePermissionComboBox.getSelectedItem()).getType());
                        permissions.setGetPermission(((PermissionItem) getPermissionComboBox.getSelectedItem()).getType());
                        permissions.setPostPermission(((PermissionItem) postPermissionComboBox.getSelectedItem()).getType());
                        permissions.setPutPermission(((PermissionItem) putPermissionComboBox.getSelectedItem()).getType());

                        try {
                            form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            if(!apiName.matches("^[A-Za-z][A-Za-z0-9_]+")) {
                                form.setCursor(Cursor.getDefaultCursor());
                                JOptionPane.showMessageDialog(form, "Invalid api name. Api name must start with a letter, \n" +
                                        "contain only letters, numbers, and undercores.", "Error creating the api", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            if(existingApiNames == null) {
                                existingApiNames = new ArrayList<String>();

                                for (CustomAPI api : AzureRestAPIManagerImpl.getManager().getAPIList(subscriptionId, serviceName)) {
                                    existingApiNames.add(api.getName().toLowerCase());
                                }
                            }

                            if(editingCustomAPI == null && existingApiNames.contains(apiName.toLowerCase())) {
                                form.setCursor(Cursor.getDefaultCursor());
                                JOptionPane.showMessageDialog(form, "Invalid API name. An API with that name already exists in this service.",
                                        "Error creating the API", JOptionPane.ERROR_MESSAGE);
                                return;
                            }


                            if(editingCustomAPI == null) {
                                AzureRestAPIManagerImpl.getManager().createCustomAPI(subscriptionId, serviceName, apiName, permissions);
                            }
                            else {
                                AzureRestAPIManagerImpl.getManager().updateCustomAPI(subscriptionId, serviceName, apiName, permissions);
                                editingCustomAPI.setCustomAPIPermissions(permissions);
                            }

                            afterSave.run();

                            form.setCursor(Cursor.getDefaultCursor());

                            form.setVisible(false);
                            form.dispose();

                        } catch (Throwable e) {
                            form.setCursor(Cursor.getDefaultCursor());
                            UIHelper.showException("Error creating table", e);
                        }

                    }
                });
            }
        });

    }

    private int permissionIndex(PermissionItem[] p, PermissionType pt) {
        for(int i = 0;i < p.length;i++) {
            if(p[i].getType() == pt)
                return i;
        }
        return 0;
    }

    public void setEditingCustomAPI(CustomAPI editingCustomAPI) {
        this.editingCustomAPI = editingCustomAPI;


        this.setTitle(editingCustomAPI == null ? "Create new custom API" : "Edit custom API");

        this.createButton.setText(editingCustomAPI == null ? "Create" : "Save");
        this.tableNameTextField.setText(editingCustomAPI == null ? "" : editingCustomAPI.getName());
        this.tableNameTextField.setEnabled(editingCustomAPI == null);
        this.createButton.setEnabled(editingCustomAPI != null);

        PermissionItem[] tablePermissions = PermissionItem.getTablePermissions();

        if(editingCustomAPI != null) {
            getPermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingCustomAPI.getCustomAPIPermissions().getGetPermission()));
            deletePermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingCustomAPI.getCustomAPIPermissions().getDeletePermission()));
            patchPermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingCustomAPI.getCustomAPIPermissions().getPatchPermission()));
            postPermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingCustomAPI.getCustomAPIPermissions().getPostPermission()));
            putPermissionComboBox.setSelectedIndex(permissionIndex(tablePermissions, editingCustomAPI.getCustomAPIPermissions().getPutPermission()));
        }

    }

    public CustomAPI getEditingCustomAPI() {
        return editingCustomAPI;
    }

    public void setAfterSave(Runnable editSaved) {
        this.afterSave = editSaved;
    }
}
