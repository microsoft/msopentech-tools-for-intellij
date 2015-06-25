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

package com.microsoftopentechnologies.intellij.helpers.activityConfiguration.azureCustomWizardParameter;

import com.google.gson.Gson;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import org.jetbrains.android.facet.AndroidFacet;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class AzureParameterPane extends JPanel {
    private JCheckBox mobileServicesCheckBox;
    private JPanel mainPanel;
    private JCheckBox notificationHubCheckBox;
    private JButton notificationHubConfigureButton;
    private JButton mobileServicesConfigureButton;
    private PlainDocument document;
    private MobileService selectedMobileService;
    private String connectionString;
    private String hubName;
    private String senderID;

    public AzureParameterPane() {
        super(new BorderLayout());

        this.add(mainPanel, BorderLayout.CENTER);

        document = new PlainDocument();

        mobileServicesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mobileServicesConfigureButton.setEnabled(mobileServicesCheckBox.isSelected());
                updateDocument();
            }
        });
        notificationHubCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                notificationHubConfigureButton.setEnabled(notificationHubCheckBox.isSelected());
                updateDocument();
            }
        });

        mobileServicesConfigureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DataContext dataContext = DataManager.getInstance().getDataContext(mainPanel);
                final Project project = DataKeys.PROJECT.getData(dataContext);

                final MobileServiceConfigForm form = new MobileServiceConfigForm(project);

                if(selectedMobileService != null) {
                    form.setSelectedMobileService(selectedMobileService);
                }

                form.show();

                if(form.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                    selectedMobileService = form.getSelectedMobileService();

                }

                updateDocument();

            }
        });


        notificationHubConfigureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    DataContext dataContext = DataManager.getInstance().getDataContext(mainPanel);
                    Project project = DataKeys.PROJECT.getData(dataContext);

                    Module module = null;
                    Object selectedElement = ProjectView.getInstance(project).getCurrentProjectViewPane().getSelectedElement();

                    if(selectedElement instanceof PsiElement) {
                        PsiElement psiSelectedElement = (PsiElement) selectedElement;
                        module = ModuleUtil.findModuleForPsiElement(psiSelectedElement);
                    } else if(selectedElement instanceof AndroidFacet) {
                        module = ((AndroidFacet) selectedElement).getModule();
                    } else if(selectedElement instanceof Module) {
                        module = (Module) selectedElement;
                    }

                    if(module != null) {
                        final NotificationHubConfigForm form = new NotificationHubConfigForm(module);

                        if (connectionString != null) {
                            form.setConnectionString(connectionString);
                        }
                        if (senderID != null) {
                            form.setSenderID(senderID);
                        }
                        if (hubName != null) {
                            form.setHubName(hubName);
                        }

                        form.show();

                        if (form.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                            connectionString = form.getConnectionString();
                            hubName = form.getHubName();
                            senderID = form.getSenderID();
                        }

                        updateDocument();
                    }
                } catch (Throwable e) {
                    DefaultLoader.getUIHelper().showException("Error loading notification hubs configuration", e);
                }
            }
        });
    }

    public String getValue() {
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException ignored) {
            return null;
        }
    }

    public void setValue(String newValue) {
        try {
            document.replace(0, document.getLength(), newValue, null);
        } catch (BadLocationException ignored) {}
    }

    public PlainDocument getDocument() {
        return document;
    }


    private void updateDocument() {
        if((mobileServicesCheckBox.isSelected()
                && !notificationHubCheckBox.isSelected()
                && selectedMobileService != null)
            || (!mobileServicesCheckBox.isSelected()
                && notificationHubCheckBox.isSelected()
                && senderID != null
                && connectionString != null
                && hubName != null)
            || (mobileServicesCheckBox.isSelected()
                && notificationHubCheckBox.isSelected()
                && selectedMobileService != null
                && senderID != null
                && connectionString != null
                && hubName != null)) {

            Gson gson = new Gson();

            AzureParameters azureParameters = new AzureParameters(
                    mobileServicesCheckBox.isSelected(),
                    notificationHubCheckBox.isSelected(),
                    mobileServicesCheckBox.isSelected() ? selectedMobileService.getAppUrl() : null,
                    mobileServicesCheckBox.isSelected() ? selectedMobileService.getAppKey() : null,
                    notificationHubCheckBox.isSelected() ? senderID : null,
                    notificationHubCheckBox.isSelected() ? connectionString : null,
                    notificationHubCheckBox.isSelected() ? hubName : null);

            String stringVal = gson.toJson(azureParameters);

            setValue(stringVal);
        } else {
            setValue("");
        }
    }

    private class AzureParameters {
        public AzureParameters(boolean hasMobileService, boolean hasNotificationHub, String appUrl, String appKey, String sender, String connStr, String hub) {
            this.hasMobileService = hasMobileService;
            this.hasNotificationHub = hasNotificationHub;
            this.appUrl = appUrl;
            this.appKey = appKey;
            this.sender = sender;
            this.connStr = connStr;
            this.hub = hub;
        }

        private boolean hasMobileService;
        private boolean hasNotificationHub;
        private String appUrl;
        private String appKey;
        private String sender;
        private String connStr;
        private String hub;
    }
}
