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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.MSOpenTechTools;
import com.microsoftopentechnologies.intellij.components.PluginSettings;
import com.microsoftopentechnologies.intellij.helpers.*;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationContext;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureManager;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.Subscription;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public class ManageSubscriptionForm extends JDialog {
    private Project project;
    private JPanel mainPanel;
    private JTable subscriptionTable;
    private JButton addSubscriptionButton;
    private JButton removeButton;
    private JButton okButton;
    private JButton cancelButton;
    private ArrayList<Subscription> subscriptionList;
    private JPopupMenu addSubscriptionMenu;
    private final int MENU_ITEM_HEIGHT = 25;
    private DialogResult dialogResult;

    public DialogResult getDialogResult() {
        return dialogResult;
    }

    public enum DialogResult {
        OK,
        CANCEL
    }

    public ManageSubscriptionForm(Project project) {
        this.project = project;
        final ManageSubscriptionForm form = this;
        this.setTitle("Manage subscriptions");
        this.setModal(true);
        this.setContentPane(mainPanel);
        getRootPane().setDefaultButton(okButton);

        final ReadOnlyCellTableModel model = new ReadOnlyCellTableModel();
        model.addColumn("Name");
        model.addColumn("Id");

        subscriptionTable.setModel(model);

        // initialize the popup menu for the "Add Subscription" button
        initSubscriptionMenu();

        addSubscriptionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addSubscriptionMenu.pack();
                addSubscriptionMenu.setPopupSize(addSubscriptionButton.getWidth(),
                        MENU_ITEM_HEIGHT * addSubscriptionMenu.getComponentCount());
                addSubscriptionMenu.show(addSubscriptionButton, 0, addSubscriptionButton.getHeight());
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int res = JOptionPane.showConfirmDialog(form, "Are you sure you would like to clear all subscriptions?",
                        "Clear Subscriptions",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (res == JOptionPane.YES_OPTION) {
                    try {
                        AzureManager apiManager = AzureRestAPIManager.getManager();
                        apiManager.clearAuthenticationTokens();
                        apiManager.clearSubscriptions();
                        apiManager.setAuthenticationMode(AzureAuthenticationMode.Unknown);
                    } catch (AzureCmdException t) {
                        UIHelper.showException("Error clearing user subscriptions", t);
                    }

                    ReadOnlyCellTableModel model = (ReadOnlyCellTableModel)subscriptionTable.getModel();
                    while (model.getRowCount() > 0)
                        model.removeRow(0);
                }
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
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

    private void onOk() {
        // check if we have any subscriptions added
        if(subscriptionTable.getModel().getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please Sign In/Import your Azure subscription(s).",
                    "Manage Azure Subscriptions",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        dialogResult = DialogResult.OK;
        dispose();
    }

    private void onCancel() {
        dialogResult = DialogResult.CANCEL;
        dispose();
    }

    private void initSubscriptionMenu() {
        addSubscriptionMenu = new JPopupMenu();
        JMenuItem signInItem = new JMenuItem("Sign In...");
        signInItem.setMnemonic(KeyEvent.VK_S);
        JMenuItem importItem = new JMenuItem("Import...");
        importItem.setMnemonic(KeyEvent.VK_I);
        addSubscriptionMenu.add(signInItem);
        addSubscriptionMenu.add(importItem);

        signInItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PluginSettings settings = MSOpenTechTools.getCurrent().getSettings();
                    final AuthenticationContext context = new AuthenticationContext(settings.getAdAuthority());

                    Futures.addCallback(context.acquireTokenInteractiveAsync(
                            settings.getTenantName(),
                            settings.getAzureServiceManagementUri(),
                            settings.getClientId(),
                            settings.getRedirectUri(),
                            project), new FutureCallback<AuthenticationResult>() {
                        @Override
                        public void onSuccess(AuthenticationResult authenticationResult) {
                            context.dispose();

                            if (authenticationResult != null) {
                                final AzureManager apiManager = AzureRestAPIManager.
                                        getManager();
                                apiManager.setAuthenticationMode(AzureAuthenticationMode.ActiveDirectory);
                                apiManager.setAuthenticationToken(authenticationResult);

                                // load list of subscriptions
                                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            apiManager.clearSubscriptions();
                                        } catch (AzureCmdException e1) {
                                            UIHelper.showException("An error occurred while attempting to " +
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
                            UIHelper.showException("An error occurred while attempting to sign in to your account.", throwable);
                        }
                    });
                } catch (IOException e1) {
                    UIHelper.showException("An error occurred while attempting to sign in to your account.", e1);
                }
            }
        });

        importItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportSubscriptionForm isf = new ImportSubscriptionForm();
                isf.setOnSubscriptionLoaded(new Runnable() {
                    @Override
                    public void run() {
                        loadList();
                    }
                });
                UIHelper.packAndCenterJDialog(isf);
                isf.setVisible(true);
            }
        });
    }

    private void loadList() {
        final JDialog form = this;
        final ReadOnlyCellTableModel model = (ReadOnlyCellTableModel) subscriptionTable.getModel();

        while (model.getRowCount() > 0)
            model.removeRow(0);

        form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Vector<String> vector = new Vector<String>();
        vector.add("(loading... )");
        vector.add("");
        model.addRow(vector);

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {

                    while (model.getRowCount() > 0)
                        model.removeRow(0);

                    subscriptionList = AzureRestAPIManager.getManager().getSubscriptionList();

                    if (subscriptionList != null) {
                        for (Subscription subs : subscriptionList) {
                            Vector<String> row = new Vector<String>();
                            row.add(subs.getName());
                            row.add(subs.getId().toString());
                            model.addRow(row);
                        }
                    }

                    form.setCursor(Cursor.getDefaultCursor());
                } catch (AzureCmdException e) {
                    form.setCursor(Cursor.getDefaultCursor());
                    UIHelper.showException("Error getting subscription list", e);
                }

            }
        });

    }
}
