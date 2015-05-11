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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.AppSettingsNames;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.components.MSOpenTechToolsApplication;
import com.microsoftopentechnologies.tooling.msservices.components.PluginSettings;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.AuthenticationContext;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.PromptValue;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Subscription;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

public class ManageSubscriptionForm extends JDialog {
    private JPanel mainPanel;
    private JTable subscriptionTable;
    private JButton signInButton;
    private JButton removeButton;
    private JButton importSubscriptionButton;
    private JButton closeButton;
    private ArrayList<Subscription> subscriptionList;
    private Project project;

    public ManageSubscriptionForm(final Project project) {
        this.project = project;

        this.setTitle("Manage Subscriptions");
        this.setModal(true);
        this.setContentPane(mainPanel);

        this.setResizable(false);

        final DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return (col == 0);
            }

            public Class<?> getColumnClass(int colIndex) {
                return getValueAt(0, colIndex).getClass();
            }
        };

        model.addColumn("");
        model.addColumn("Name");
        model.addColumn("Id");

        subscriptionTable.setModel(model);

        TableColumn column = subscriptionTable.getColumnModel().getColumn(0);
        column.setMinWidth(23);
        column.setMaxWidth(23);

        signInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (AzureRestAPIManagerImpl.getManager().getAuthenticationToken() != null) {
                        clearSubscriptions(false);
                    } else {
                        PluginSettings settings = MSOpenTechToolsApplication.getCurrent().getSettings();
                        final AuthenticationContext context = new AuthenticationContext(settings.getAdAuthority());

                        Futures.addCallback(context.acquireTokenInteractiveAsync(
                                settings.getTenantName(),
                                settings.getAzureServiceManagementUri(),
                                settings.getClientId(),
                                settings.getRedirectUri(),
                                project,
                                PromptValue.login), new FutureCallback<AuthenticationResult>() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                context.dispose();

                                if (authenticationResult != null) {
                                    final AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();
                                    apiManager.setAuthenticationMode(AzureAuthenticationMode.ActiveDirectory);
                                    apiManager.setAuthenticationToken(authenticationResult);

                                    // load list of subscriptions
                                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                apiManager.clearSubscriptions();

                                                refreshSignInCaption();
                                            } catch (AzureCmdException e1) {
                                                DefaultLoader.getUIHelper().showException("An error occurred while attempting to " +
                                                        "clear your old subscriptions.", e1);
                                            }

                                            loadList();
                                        }
                                    }, ModalityState.any());
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                context.dispose();
                                DefaultLoader.getUIHelper().showException("An error occurred while attempting to sign in to your account.", throwable);
                            }
                        });
                    }
                } catch (IOException e1) {
                    DefaultLoader.getUIHelper().showException("An error occurred while attempting to sign in to your account.", e1);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                clearSubscriptions(true);
            }
        });

        removeButton.setEnabled(false);

        importSubscriptionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportSubscriptionForm isf = new ImportSubscriptionForm();
                isf.setOnSubscriptionLoaded(new Runnable() {
                    @Override
                    public void run() {
                        loadList();
                    }
                });
                UIHelperImpl.packAndCenterJDialog(isf);
                isf.setVisible(true);
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                loadList();
            }
        });
    }

    private void refreshSignInCaption() {
        boolean isNotSigned = (AzureRestAPIManagerImpl.getManager().getAuthenticationToken() == null);

        signInButton.setText(isNotSigned ? "Sign In ..." : "Sign Out");
    }

    private void clearSubscriptions(boolean isSigningOut) {
        int res = JOptionPane.showConfirmDialog(this, (isSigningOut
                        ? "Are you sure you would like to clear all subscriptions?"
                        : "Are you sure you would like to sign out?"),
                (isSigningOut
                        ? "Clear Subscriptions"
                        : "Sign out"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (res == JOptionPane.YES_OPTION) {
            try {
                AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();
                apiManager.clearAuthenticationTokens();
                apiManager.clearSubscriptions();
                apiManager.setAuthenticationMode(AzureAuthenticationMode.Unknown);
            } catch (AzureCmdException t) {
                DefaultLoader.getUIHelper().showException("Error clearing user subscriptions", t);
            }

            DefaultTableModel model = (DefaultTableModel) subscriptionTable.getModel();

            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }

            PropertiesComponent.getInstance().unsetValue(AppSettingsNames.SELECTED_SUBSCRIPTIONS);
            ApplicationManager.getApplication().saveSettings();

            removeButton.setEnabled(false);

            refreshSignInCaption();
        }
    }

    private void onCancel() {
        try {
            ArrayList<UUID> selectedList = new ArrayList<UUID>();

            TableModel model = subscriptionTable.getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean selected = (Boolean) model.getValueAt(i, 0);

                if (selected) {
                    selectedList.add(UUID.fromString(model.getValueAt(i, 2).toString()));
                }
            }

            AzureRestAPIManagerImpl.getManager().setSelectedSubscriptions(selectedList);

            //Saving the project is necessary to save the changes on the PropertiesComponent
            if (project != null) {
                project.save();
            }

            dispose();
        } catch (AzureCmdException e) {
            DefaultLoader.getUIHelper().showException("Error setting selected subscriptions", e);
        }
    }

    private void loadList() {
        final JDialog form = this;
        final DefaultTableModel model = (DefaultTableModel) subscriptionTable.getModel();

        refreshSignInCaption();

        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Vector<Object> vector = new Vector<Object>();
        vector.add("");
        vector.add("(loading... )");
        vector.add("");
        model.addRow(vector);

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    while (model.getRowCount() > 0) {
                        model.removeRow(0);
                    }

                    subscriptionList = AzureRestAPIManagerImpl.getManager().getFullSubscriptionList();

                    if (subscriptionList != null && subscriptionList.size() > 0) {
                        for (Subscription subs : subscriptionList) {
                            Vector<Object> row = new Vector<Object>();
                            row.add(subs.isSelected());
                            row.add(subs.getName());
                            row.add(subs.getId().toString());
                            model.addRow(row);
                        }

                        removeButton.setEnabled(true);
                    } else {
                        removeButton.setEnabled(false);
                    }

                    form.setCursor(Cursor.getDefaultCursor());
                } catch (AzureCmdException e) {
                    form.setCursor(Cursor.getDefaultCursor());
                    DefaultLoader.getUIHelper().showException("Error getting subscription list", e);
                }
            }
        });
    }
}