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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoftopentechnologies.intellij.forms.CreateMobileServiceForm;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.ReadOnlyCellTableModel;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


public class MobileServiceConfigForm extends DialogWrapper {
    private JPanel rootPanel;
    private JTable mobileServices;
    private JButton buttonAddService;
    private JButton buttonEdit;
    private JTextPane summaryTextPane;
    private List<MobileService> mobileServiceList;
    private Project project;

    private MobileService selectedMobileService;

    public MobileServiceConfigForm(Project project) {
        super(project, true);
        setTitle("Select an Azure Mobile Service");

        this.project = project;

        getOKAction().setEnabled(false);

        summaryTextPane.setContentType("text/html");

        ReadOnlyCellTableModel tableModel = new ReadOnlyCellTableModel();
        mobileServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel selectionModel = mobileServices.getSelectionModel();

        tableModel.addColumn("Name");
        tableModel.addColumn("Region");
        tableModel.addColumn("Type");
        tableModel.addColumn("Subscription");

        mobileServices.setModel(tableModel);

        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                Object sourceObj = listSelectionEvent.getSource();

                if (sourceObj instanceof ListSelectionModel
                        && !((ListSelectionModel) sourceObj).isSelectionEmpty()
                        && listSelectionEvent.getValueIsAdjusting()
                        && mobileServiceList.size() > 0) {
                    selectedMobileService =
                            mobileServiceList.get(mobileServices.getSelectionModel().getLeadSelectionIndex());
                    getOKAction().setEnabled(true);

                    updateSummary();
                }
            }
        });

        buttonEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                editSubscriptions();
            }
        });

        buttonAddService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CreateMobileServiceForm form = new CreateMobileServiceForm();
                form.setServiceCreated(new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fillList();
                            }
                        });
                    }
                });

                form.setModal(true);
                UIHelperImpl.packAndCenterJDialog(form);
                form.setVisible(true);
            }
        });

        init();

        fillList();
    }


    private void editSubscriptions() {
        ManageSubscriptionForm form = new ManageSubscriptionForm(project);
        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);

        try {
            List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();

            if (subscriptionList.size() == 0) {
                buttonAddService.setEnabled(false);

                // clear the mobile services list
                final ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
                messageTableModel.addColumn("Message");
                Vector<String> vector = new Vector<String>();
                vector.add("Please sign in/import your Azure subscriptions.");
                messageTableModel.addRow(vector);


                mobileServices.setModel(messageTableModel);
            } else {
                fillList();
            }
        } catch (AzureCmdException e) {
            final ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
            messageTableModel.addColumn("Message");
            Vector<String> vector = new Vector<String>();
            vector.add("There has been an error while retrieving the configured Azure subscriptions.");
            messageTableModel.addRow(vector);
            vector = new Vector<String>();
            vector.add("Please retry signing in/importing your Azure subscriptions.");
            messageTableModel.addRow(vector);

            mobileServices.setModel(messageTableModel);
        }
    }

    private void updateSummary() {
        summaryTextPane.setText("<html> <head> </head> <body style=\"font-family: sans serif;\"> <p style=\"margin-top: 0\">"
                + "<b>Summary:</b></p> <ol> "
                + "<li>Added a reference to the Azure Mobile Services library in project <b>"
                + project.getName()
                + "</b>.</li> "
                + "<li>Added a static method to instantiate MobileServiceClient, connecting to <b>"
                + selectedMobileService.getName()
                + "</b>.</li> "
                + "</ol> <p style=\"margin-top: 0\">After clicking Finish, it might take a few seconds to "
                + "complete set up.</p> </body> </html>");
    }

    private void fillList() {

        getOKAction().setEnabled(false);
        mobileServiceList = new ArrayList<MobileService>();
        mobileServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
        messageTableModel.addColumn("Message");
        Vector<String> vector = new Vector<String>();
        vector.add("(loading... )");
        messageTableModel.addRow(vector);

        mobileServices.setModel(messageTableModel);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading mobile services", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {

                    final List<MobileService> currentSubServices = new ArrayList<MobileService>();
                    final List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
                    final HashMap<String, String> subscriptionData = new HashMap<String, String>();

                    if (subscriptionList.size() > 0) {

                        for (Subscription s : subscriptionList) {
                            subscriptionData.put(s.getId(), s.getName());
                            currentSubServices.addAll(AzureManagerImpl.getManager().getMobileServiceList(s.getId()));
                        }


                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                buttonAddService.setEnabled(true);

                                ReadOnlyCellTableModel serviceTableModel = new ReadOnlyCellTableModel();
                                serviceTableModel.addColumn("Name");
                                serviceTableModel.addColumn("Region");
                                serviceTableModel.addColumn("Type");
                                serviceTableModel.addColumn("Subscription");

                                int rowIndex = 0;
                                mobileServices.getSelectionModel().clearSelection();


                                for (MobileService mobileService : currentSubServices) {
                                    Vector<String> row = new Vector<String>();
                                    row.add(mobileService.getName());
                                    row.add(mobileService.getRegion());
                                    row.add("Mobile service");
                                    row.add(subscriptionData.get(mobileService.getSubcriptionId()));

                                    serviceTableModel.addRow(row);
                                    mobileServiceList.add(mobileService);

                                    if (selectedMobileService != null
                                            && selectedMobileService.getName().equals(mobileService.getName())
                                            && selectedMobileService.getSubcriptionId().equals(mobileService.getSubcriptionId())) {
                                        mobileServices.getSelectionModel().setLeadSelectionIndex(rowIndex);
                                    }

                                    rowIndex++;
                                }

                                if (rowIndex == 0) {
                                    while (messageTableModel.getRowCount() > 0) {
                                        messageTableModel.removeRow(0);
                                    }

                                    Vector<String> vector = new Vector<String>();
                                    vector.add("There are no Azure Mobile Services on the imported subscriptions");
                                    messageTableModel.addRow(vector);

                                    mobileServices.setModel(messageTableModel);
                                } else {
                                    mobileServices.setModel(serviceTableModel);
                                }
                            }
                        }, ModalityState.any());
                    } else {
                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                buttonAddService.setEnabled(false);

                                while (messageTableModel.getRowCount() > 0) {
                                    messageTableModel.removeRow(0);
                                }

                                Vector<String> vector = new Vector<String>();
                                vector.add("Please sign in/import your Azure subscriptions.");
                                messageTableModel.addRow(vector);
                                mobileServices.setModel(messageTableModel);

                                editSubscriptions();

                            }
                        }, ModalityState.any());
                    }

                } catch (Throwable ex) {
                    DefaultLoader.getUIHelper().showException("Error retrieving service list", ex);
                    mobileServiceList.clear();

                    while (messageTableModel.getRowCount() > 0) {
                        messageTableModel.removeRow(0);
                    }

                    Vector<String> vector = new Vector<String>();
                    vector.add("There has been an error while loading the list of Azure Mobile Services");
                    messageTableModel.addRow(vector);
                    mobileServices.setModel(messageTableModel);
                }
            }
        });
    }

    public MobileService getSelectedMobileService() {
        return selectedMobileService;
    }

    public void setSelectedMobileService(MobileService selectedMobileService) {
        this.selectedMobileService = selectedMobileService;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return (mobileServices.getSelectedRows().length == 0)
                ? new ValidationInfo("Select a Mobile Service", mobileServices)
                : super.doValidate();
    }
}