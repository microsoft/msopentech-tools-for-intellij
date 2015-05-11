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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.LinkListener;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.vm.AffinityGroup;
import com.microsoftopentechnologies.tooling.msservices.model.vm.Location;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Vector;

public class CreateStorageAccountForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox subscriptionComboBox;
    private JTextField nameTextField;
    private JComboBox regionOrAffinityGroupComboBox;
    private JComboBox replicationComboBox;
    private JProgressBar createProgressBar;
    private JLabel pricingLabel;
    private JLabel userInfoLabel;

    private Runnable onCreate;
    private Subscription subscription;
    private StorageAccount storageAccount;
    private Project project;

    private final String PRICING_LINK = "http://go.microsoft.com/fwlink/?LinkID=400838";

    private enum ReplicationTypes {
        Standard_LRS,
        Standard_GRS,
        Standard_RAGRS;

        public String getDescription() {
            switch (this) {
                case Standard_GRS:
                    return "Geo-Redundant";
                case Standard_LRS:
                    return "Locally Redundant";
                case Standard_RAGRS:
                    return "Read Access Geo-Redundant";
            }

            return super.toString();
        }
    }

    public CreateStorageAccountForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setResizable(false);
        setPreferredSize(new Dimension(411, 330));
        setTitle("Create Storage Account");

        pricingLabel.addMouseListener(new LinkListener(PRICING_LINK));
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        regionOrAffinityGroupComboBox.setRenderer(new ListCellRendererWrapper<Object>() {

            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (!(o instanceof String) && o != null) {
                    setText("  " + o.toString());
                }
            }
        });

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }
        });

        regionOrAffinityGroupComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateEmptyFields();
            }
        });

        if (AzureRestAPIManagerImpl.getManager().getAuthenticationMode().equals(AzureAuthenticationMode.ActiveDirectory)) {
            String upn = AzureRestAPIManagerImpl.getManager().getAuthenticationToken().getUserInfo().getUniqueName();
            userInfoLabel.setText("Signed in as: " + (upn.contains("#") ? upn.split("#")[1] : upn));
        } else {
            userInfoLabel.setText("");
        }

        replicationComboBox.setModel(new DefaultComboBoxModel(ReplicationTypes.values()));
        replicationComboBox.setRenderer(new ListCellRendererWrapper<ReplicationTypes>() {
            @Override
            public void customize(JList jList, ReplicationTypes replicationTypes, int i, boolean b, boolean b1) {
                setText(replicationTypes.getDescription());
            }
        });
    }


    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                nameTextField.getText().isEmpty() || regionOrAffinityGroupComboBox.getSelectedObjects().length == 0);

        buttonOK.setEnabled(allFieldsCompleted);
    }

    private void onOK() {
        if (nameTextField.getText().length() < 3
                || nameTextField.getText().length() > 24
                || !nameTextField.getText().matches("[a-z0-9]+")) {
            JOptionPane.showMessageDialog(this, "Invalid storage account name. The name should be between 3 and 24 characters long and \n" +
                    "can contain only lowercase letters and numbers.", "Service Explorer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        createProgressBar.setVisible(true);

        try {
            String name = nameTextField.getText();
            String region = (regionOrAffinityGroupComboBox.getSelectedItem() instanceof Location) ? regionOrAffinityGroupComboBox.getSelectedItem().toString() : "";
            String affinityGroup = (regionOrAffinityGroupComboBox.getSelectedItem() instanceof AffinityGroup) ? regionOrAffinityGroupComboBox.getSelectedItem().toString() : "";
            String replication = replicationComboBox.getSelectedItem().toString();

            storageAccount = new StorageAccount(name, replication, "", "", "", region, affinityGroup, "", "",
                    "", "", "", "", "", "", "", "", new GregorianCalendar(), subscription.getId().toString());
            AzureSDKManagerImpl.getManager().createStorageAccount(storageAccount);
            AzureSDKManagerImpl.getManager().refreshStorageAccountInformation(storageAccount);

            if (onCreate != null) {
                onCreate.run();
            }
        } catch (AzureCmdException e) {
            storageAccount = null;
            DefaultLoader.getUIHelper().showException("An error occurred while trying to create the specified storage account.", e, "Error Creating Storage Account", false, true);
        }

        setCursor(Cursor.getDefaultCursor());

        this.setVisible(false);
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public void fillFields(final Subscription subscription, Project project) {
        final CreateStorageAccountForm createStorageAccountForm = this;
        this.project = project;

        if (subscription == null) {
            try {
                subscriptionComboBox.setEnabled(true);

                ArrayList<Subscription> fullSubscriptionList = AzureRestAPIManagerImpl.getManager().getFullSubscriptionList();
                subscriptionComboBox.setModel(new DefaultComboBoxModel(new Vector<Subscription>(fullSubscriptionList)));
                subscriptionComboBox.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent itemEvent) {
                        createStorageAccountForm.subscription = (Subscription) itemEvent.getItem();
                        loadRegions();
                    }
                });

                if (fullSubscriptionList.size() > 0) {
                    createStorageAccountForm.subscription = fullSubscriptionList.get(0);
                    loadRegions();
                }

            } catch (AzureCmdException e) {
                DefaultLoader.getUIHelper().showException("Error getting subscriptions", e);
            }

        } else {
            this.subscription = subscription;
            subscriptionComboBox.addItem(subscription.getName());

            loadRegions();
        }
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void loadRegions() {
        regionOrAffinityGroupComboBox.addItem("<Loading...>");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading regions...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                progressIndicator.setIndeterminate(true);

                try {
                    final java.util.List<AffinityGroup> affinityGroups = AzureSDKManagerImpl.getManager().getAffinityGroups(subscription.getId().toString());
                    final java.util.List<Location> locations = AzureSDKManagerImpl.getManager().getLocations(subscription.getId().toString());

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final Vector<Object> vector = new Vector<Object>();
                            vector.add("Regions");
                            vector.addAll(locations);
                            if (affinityGroups.size() > 0) {
                                vector.add("Affinity Groups");
                                vector.addAll(affinityGroups);
                            }

                            regionOrAffinityGroupComboBox.removeAllItems();
                            regionOrAffinityGroupComboBox.setModel(new DefaultComboBoxModel(vector) {
                                public void setSelectedItem(Object o) {
                                    if (!(o instanceof String)) {
                                        super.setSelectedItem(o);
                                    }
                                }
                            });

                            regionOrAffinityGroupComboBox.setSelectedIndex(1);
                        }
                    });
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying to load the regions list",
                            e, "Error Loading Regions", false, true);
                }
            }
        });
    }
}
