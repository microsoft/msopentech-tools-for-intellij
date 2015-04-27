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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.directoryservices.Application;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.LinkListener;
import com.microsoftopentechnologies.intellij.helpers.StringHelper;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.intellij.helpers.o365.Office365RestAPIManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;

public class CreateOffice365ApplicationForm extends JDialog {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JButton btnCloseButton;
    private JLabel lblPrivacy;
    private JButton btnCreate;
    private JCheckBox multiTenantCheckBox;
    private JTextField redirectURITextField;
    private DialogResult dialogResult;
    private Application application;

    public DialogResult getDialogResult() {
        return dialogResult;
    }

    public Application getApplication() {
        return application;
    }

    private void setApplication(Application application) {
        this.application = application;
    }

    public enum DialogResult {
        OK,
        CANCEL
    }

    public CreateOffice365ApplicationForm() {
        final JDialog form = this;

        this.setContentPane(mainPanel);
        this.setResizable(false);
        this.setModal(true);
        this.setTitle("Create Office 365 Application");

        // we disable the multi-tenant checkbox for now since VS
        // does it
        // TODO: Find out why VS does it. It's probably because mobile apps *have* to be multi-tenant.
        // In which case why have this option on the UI at all?
        multiTenantCheckBox.setSelected(true);
        multiTenantCheckBox.setEnabled(false);

        lblPrivacy.addMouseListener(new LinkListener("http://msdn.microsoft.com/en-us/vstudio/dn425032.aspx"));

        btnCloseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dialogResult = DialogResult.CANCEL;
                form.setVisible(false);
                form.dispose();
            }
        });

        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String name = nameTextField.getText();
                            final String replyURL = redirectURITextField.getText();

                            String error = "";

                            if (StringHelper.isNullOrWhiteSpace(name)) {
                                error += "The application name must not be empty.\n";
                            } else if (name.length() > 64) {
                                error += "The application name cannot be more than 64 characters long.\n";
                            }

                            if (StringHelper.isNullOrWhiteSpace(replyURL)) {
                                error += "The redirect URI must not be empty.\n";
                            } else {
                                try {
                                    new URI(replyURL);
                                } catch (URISyntaxException e) {
                                    error += "The redirect URI must be a valid URI.\n";
                                }
                            }

                            if (!error.isEmpty()) {
                                JOptionPane.showMessageDialog(form, error, "Error creating the application",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            Application application = new Application();
                            application.setdisplayName(name);
                            application.setreplyUrls(Lists.newArrayList(replyURL));
                            application.sethomepage(replyURL);
                            application.setavailableToOtherTenants(multiTenantCheckBox.isSelected());
                            application.setpublicClient(true);

                            Futures.addCallback(Office365RestAPIManager.getManager().registerApplication(application),
                                    new FutureCallback<Application>() {
                                        @Override
                                        public void onSuccess(final Application application) {
                                            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setApplication(application);
                                                    dialogResult = DialogResult.OK;
                                                    form.setCursor(Cursor.getDefaultCursor());
                                                    form.dispose();
                                                }
                                            }, ModalityState.any());
                                        }
                                    }, ModalityState.any());
                                }

                                @Override
                                public void onFailure(final Throwable throwable) {
                                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                        @Override
                                        public void onFailure(final Throwable throwable) {
                                            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                                @Override
                                                public void run() {
                                                    form.setCursor(Cursor.getDefaultCursor());
                                                    UIHelper.showException("An error occurred while trying to register the Office 365 application.",
                                                            throwable,
                                                            "Error Registering Office 365 Application",
                                                            false,
                                                            true);
                                                }
                                            }, ModalityState.any());
                                        }
                                    });
                        } catch (Throwable e) {
                            form.setCursor(Cursor.getDefaultCursor());
                            UIHelper.showException("An error occurred while trying to register the Office 365 application.",
                                    e,
                                    "Error Registering Office 365 Application",
                                    false,
                                    true);
                        }
                    }
                });
            }
        });
    }
}