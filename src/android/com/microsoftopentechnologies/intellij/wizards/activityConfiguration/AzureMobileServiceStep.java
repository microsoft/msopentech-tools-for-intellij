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

package com.microsoftopentechnologies.intellij.wizards.activityConfiguration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.forms.CreateNewServiceForm;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.ReadOnlyCellTableModel;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.Service;
import com.microsoftopentechnologies.intellij.model.Subscription;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AzureMobileServiceStep extends WizardStep<AddServiceWizardModel> {
    private static final ServiceType serviceType = ServiceType.AzureMobileServices;

    private final AddServiceWizardModel model;
    private JPanel rootPanel;
    private JList listServices;
    private JTable mobileServices;
    private JButton buttonAddService;
    private JButton buttonEdit;
    private List<Service> serviceList;
    private ModalityState modalityState;

    public AzureMobileServiceStep(final String title, final AddServiceWizardModel model) {
        super(title, null, null);
        this.model = model;
        this.modalityState = ModalityState.any();

        ReadOnlyCellTableModel tableModel = new ReadOnlyCellTableModel();
        mobileServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel selectionModel = mobileServices.getSelectionModel();

        tableModel.addColumn("Name");
        tableModel.addColumn("Region");
        tableModel.addColumn("Type");
        tableModel.addColumn("Subscription");

        mobileServices.setModel(tableModel);

        List<String> listServicesData = new ArrayList<String>();

        int boldIndex = -1;
        int index = 0;
        for (ServiceType serviceType : this.model.getServiceTypes()) {
            listServicesData.add(serviceType.getDisplayName());

            if (serviceType.equals(AzureMobileServiceStep.serviceType)) {
                boldIndex = index;
            }

            index++;
        }

        final String boldValue = listServicesData.get(boldIndex);
        DefaultListCellRenderer customListCellRenderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (boldValue.equals(value)) {// <= put your logic here
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        };
        this.listServices.setCellRenderer(customListCellRenderer);

        DefaultListSelectionModel customListSelectionModel = new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {

            }

            @Override
            public void addSelectionInterval(int index0, int index1) {

            }
        };
        this.listServices.setSelectionModel(customListSelectionModel);

        this.listServices.setListData(listServicesData.toArray(new String[1]));

        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                Object sourceObj = listSelectionEvent.getSource();

                if (sourceObj instanceof ListSelectionModel
                        && !((ListSelectionModel) sourceObj).isSelectionEmpty()
                        && listSelectionEvent.getValueIsAdjusting()
                        && serviceList.size() > 0) {
                    Service s = serviceList.get(mobileServices.getSelectionModel().getLeadSelectionIndex());
                    model.setService(s);
                    model.getCurrentNavigationState().NEXT.setEnabled(true);
                }
            }
        });

        buttonEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ManageSubscriptionForm form = new ManageSubscriptionForm(null);
                UIHelper.packAndCenterJDialog(form);
                form.setVisible(true);

                try {
                    List<Subscription> subscriptionList = AzureRestAPIManager.getManager().getSubscriptionList();

                    if (subscriptionList == null || subscriptionList.size() == 0) {
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
        });

        buttonAddService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CreateNewServiceForm form = new CreateNewServiceForm();
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
                UIHelper.packAndCenterJDialog(form);
                form.setVisible(true);
            }
        });

        refreshServices();
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        rootPanel.revalidate();
        return rootPanel;
    }

    @Override
    public WizardStep onNext(AddServiceWizardModel model) {
        return super.onNext(model);
    }

    private void refreshServices() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                fillList();
            }
        }, this.modalityState);
    }

    private void fillList() {
        model.setService(null);
        model.getCurrentNavigationState().NEXT.setEnabled(false);
        serviceList = new ArrayList<Service>();
        mobileServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
        messageTableModel.addColumn("Message");
        Vector<String> vector = new Vector<String>();
        vector.add("(loading... )");
        messageTableModel.addRow(vector);

        mobileServices.setModel(messageTableModel);

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                int selectedIndex = -1;

                try {
                    List<Subscription> subscriptionList = AzureRestAPIManager.getManager().getSubscriptionList();

                    if (subscriptionList != null && subscriptionList.size() > 0) {
                        buttonAddService.setEnabled(true);

                        ReadOnlyCellTableModel serviceTableModel = new ReadOnlyCellTableModel();
                        serviceTableModel.addColumn("Name");
                        serviceTableModel.addColumn("Region");
                        serviceTableModel.addColumn("Type");
                        serviceTableModel.addColumn("Subscription");

                        int rowIndex = 0;

                        for (Subscription s : subscriptionList) {
                            List<Service> currentSubServices = AzureRestAPIManager.getManager().getServiceList(s.getId());

                            for (Service service : currentSubServices) {
                                Vector<String> row = new Vector<String>();
                                row.add(service.getName());
                                row.add(service.getRegion());
                                row.add("Mobile service");
                                row.add(s.getName());

                                serviceTableModel.addRow(row);
                                serviceList.add(service);

                                if (model.getService() != null
                                        && model.getService().getName().equals(service.getName())
                                        && model.getService().getSubcriptionId().toString().equals(service.getSubcriptionId().toString())) {
                                    selectedIndex = rowIndex;
                                }

                                rowIndex++;
                            }
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
                    } else {
                        buttonAddService.setEnabled(false);

                        while (messageTableModel.getRowCount() > 0) {
                            messageTableModel.removeRow(0);
                        }

                        Vector<String> vector = new Vector<String>();
                        vector.add("Please sign in/import your Azure subscriptions.");
                        messageTableModel.addRow(vector);
                        mobileServices.setModel(messageTableModel);

                        buttonEdit.doClick();
                    }
                } catch (Throwable ex) {
                    UIHelper.showException("Error retrieving service list", ex);
                    serviceList.clear();

                    while (messageTableModel.getRowCount() > 0) {
                        messageTableModel.removeRow(0);
                    }

                    Vector<String> vector = new Vector<String>();
                    vector.add("There has been an error while loading the list of Azure Mobile Services");
                    messageTableModel.addRow(vector);
                    mobileServices.setModel(messageTableModel);
                }

                mobileServices.getSelectionModel().clearSelection();

                if (selectedIndex >= 0) {
                    mobileServices.getSelectionModel().setLeadSelectionIndex(selectedIndex);
                }
            }
        }, this.modalityState);
    }
}