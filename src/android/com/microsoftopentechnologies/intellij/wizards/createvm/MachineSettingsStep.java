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

package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineSize;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MachineSettingsStep extends WizardStep<CreateVMWizardModel> {
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JPanel scrollPanel;
    private JPanel imageInfoPanel;
    private JTextPane imageDescriptionTextPane;
    private JTextPane imageLocationTextPane;
    private JTextPane imagePublisherTextPane;
    private JTextPane imagePublishedDateTextPane;
    private JTextPane imageOSFamilyTextPane;
    private JTextPane imageTitleTextPane;
    private JTextField vmNameTextField;
    private JComboBox vmSizeComboBox;
    private JTextField vmUserTextField;
    private JPasswordField vmPasswordField;
    private JPasswordField confirmPasswordField;

    Project project;
    CreateVMWizardModel model;

    private void createUIComponents() {
        imageInfoPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {

                double height = 0;
                for (Component component : this.getComponents()) {
                    height += component.getHeight();
                }

                Dimension preferredSize = super.getPreferredSize();
                preferredSize.setSize(preferredSize.getWidth(), height);
                return preferredSize;
            }
        };
    }


    public MachineSettingsStep(CreateVMWizardModel mModel, Project project) {
        super("Virtual Machine Basic Settings", null);

        this.project = project;
        this.model = mModel;

        model.configStepList(createVmStepsList, 2);

        scrollPanel.remove(imageInfoPanel);
        final JBScrollPane jbScrollPane = new JBScrollPane(imageInfoPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.add(jbScrollPane);

        DocumentListener documentListener = new DocumentListener() {
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
        };

        vmNameTextField.getDocument().addDocumentListener(documentListener);
        vmUserTextField.getDocument().addDocumentListener(documentListener);
        vmPasswordField.getDocument().addDocumentListener(documentListener);
        confirmPasswordField.getDocument().addDocumentListener(documentListener);
    }

    private void validateEmptyFields() {

        boolean allFieldsCompleted = !(
                vmNameTextField.getText().isEmpty()
                        || vmUserTextField.getText().isEmpty()
                        || vmPasswordField.getPassword().length == 0
                        || confirmPasswordField.getPassword().length == 0);

        model.getCurrentNavigationState().NEXT.setEnabled(allFieldsCompleted);
    }

    @Override
    public WizardStep onNext(CreateVMWizardModel model) {
        WizardStep wizardStep = super.onNext(model);

        String name = vmNameTextField.getText();
        String pass = new String(vmPasswordField.getPassword());
        String conf = new String(confirmPasswordField.getPassword());

        if (name.length() > 15 || name.length() < 3) {
            JOptionPane.showMessageDialog(null, "Invalid virtual machine name. The name must be between 3 and 15 character long.", "Error creating the virtual machine", JOptionPane.ERROR_MESSAGE);
            return this;
        }

        if (!name.matches("^[A-Za-z][A-Za-z0-9-]+[A-Za-z0-9]$")) {
            JOptionPane.showMessageDialog(null, "Invalid virtual machine name. The name must start with a letter, \n" +
                    "contain only letters, numbers, and hyphens, " +
                    "and end with a letter or number.", "Error creating the virtual machine", JOptionPane.ERROR_MESSAGE);
            return this;
        }

        if (!pass.equals(conf)) {
            JOptionPane.showMessageDialog(null, "Password confirmation should match password", "Error creating the service", JOptionPane.ERROR_MESSAGE);
            return this;
        }

        if (!pass.matches("(?=^.{8,255}$)((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])(?=.*[a-z])|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*")) {
            JOptionPane.showMessageDialog(null, "The password does not conform to complexity requirements.\n" +
                    "It should be at least eight characters long and contain a mixture of upper case, lower case, digits and symbols.", "Error creating the virtual machine", JOptionPane.ERROR_MESSAGE);
            return this;
        }


        model.setName(name);
        model.setSize((VirtualMachineSize) vmSizeComboBox.getSelectedItem());
        model.setUserName(vmUserTextField.getText());
        model.setPassword(confirmPasswordField.getPassword());

        return wizardStep;
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        validateEmptyFields();

        final VirtualMachineImage virtualMachineImage = model.getVirtualMachineImage();

        imageTitleTextPane.setText(virtualMachineImage.getLabel());
        imageDescriptionTextPane.setText(virtualMachineImage.getDescription());
        imagePublishedDateTextPane.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(virtualMachineImage.getPublishedDate().getTime()));
        imagePublisherTextPane.setText(virtualMachineImage.getPublisherName());
        imageOSFamilyTextPane.setText(virtualMachineImage.getOperatingSystemType());
        imageLocationTextPane.setText(virtualMachineImage.getLocation());

        imageTitleTextPane.setCaretPosition(0);


        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading VM sizes...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    final List<VirtualMachineSize> virtualMachineSizes = AzureSDKManagerImpl.getManager().getVirtualMachineSizes(model.getSubscription().getId().toString());

                    Collections.sort(virtualMachineSizes, new Comparator<VirtualMachineSize>() {
                        @Override
                        public int compare(VirtualMachineSize t0, VirtualMachineSize t1) {

                            if(t0.getName().contains("Basic") && t1.getName().contains("Basic")) {
                                return t0.getName().compareTo(t1.getName());
                            } else if(t0.getName().contains("Basic") ) {
                                return -1;
                            } else if(t1.getName().contains("Basic") ) {
                                return 1;
                            }

                            int coreCompare = Integer.valueOf(t0.getCores()).compareTo(Integer.valueOf(t1.getCores()));

                            if(coreCompare == 0){
                                return Integer.valueOf(t0.getMemoryInMB()).compareTo(Integer.valueOf(t1.getMemoryInMB()));
                            } else {
                                return coreCompare;
                            }
                        }
                    });



                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            vmSizeComboBox.setModel(new DefaultComboBoxModel(virtualMachineSizes.toArray()));
                        }
                    });


                } catch (AzureCmdException e) {
                    UIHelper.showException("Error trying to get VM sizes", e);
                }
            }
        });

        return rootPanel;
    }


}
