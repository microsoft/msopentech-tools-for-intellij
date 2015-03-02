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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Subscription;
import com.microsoftopentechnologies.intellij.model.vm.AffinityGroup;
import com.microsoftopentechnologies.intellij.model.vm.CloudService;
import com.microsoftopentechnologies.intellij.model.vm.Location;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Vector;

public class CreateCloudServiceForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox subscriptionComboBox;
    private JTextField nameTextField;
    private JComboBox regionOrAffinityGroupComboBox;
    private JProgressBar createProgressBar;

    private Subscription subscription;
    private CloudService cloudService;
    private Runnable onCreate;

    public CreateCloudServiceForm() {
        setContentPane(contentPane);
        setModal(true);

        setResizable(false);
        setPreferredSize(new Dimension(411, 300));

        getRootPane().setDefaultButton(buttonOK);

        setTitle("Create Cloud Service");

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
                if(!(o instanceof String)) {
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
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                nameTextField.getText().isEmpty() || regionOrAffinityGroupComboBox.getSelectedObjects().length == 0);

        buttonOK.setEnabled(allFieldsCompleted);
    }

    public void fillFields(final Subscription subscription, Project project) {
        this.subscription = subscription;

        subscriptionComboBox.addItem(subscription.getName());

        regionOrAffinityGroupComboBox.addItem("<Loading...>");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading regions...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {

                    final List<AffinityGroup> affinityGroups = AzureSDKManagerImpl.getManager().getAffinityGroups(subscription.getId().toString());
                    final List<Location> locations = AzureSDKManagerImpl.getManager().getLocations(subscription.getId().toString());

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
                    UIHelper.showException("Error getting regions", e);
                }
            }
        });

    }

    private void onOK() {
        if (!nameTextField.getText().matches("^[A-Za-z0-9][A-Za-z0-9-]+[A-Za-z0-9]$")) {
            JOptionPane.showMessageDialog(this, "Invalid cloud service name. Cloud service name must start with a letter or number, \n" +
                    "contain only letters, numbers, and hyphens, " +
                    "and end with a letter or number.", "Error creating the cloud service", JOptionPane.ERROR_MESSAGE);
            return;
        }

        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        createProgressBar.setVisible(true);

        try {
            String name = nameTextField.getText();
            Object regionOrAffinity = regionOrAffinityGroupComboBox.getSelectedItem();
            String region = (regionOrAffinity instanceof Location) ? regionOrAffinity.toString() : "";
            String affinityGroup = (regionOrAffinity instanceof AffinityGroup) ? regionOrAffinity.toString() : "";

            cloudService = new CloudService(name, region, affinityGroup, subscription.getId().toString());
            AzureSDKManagerImpl.getManager().createCloudService(cloudService);
        } catch (Exception e) {
            cloudService = null;
            UIHelper.showException("An error occurred while trying to create the specified cloud service", e, "Error Creating Storage Account", false, true);
        }

        onCreate.run();
        this.setCursor(Cursor.getDefaultCursor());

        this.setVisible(false);
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public CloudService getCloudService() {
        return cloudService;
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }
}