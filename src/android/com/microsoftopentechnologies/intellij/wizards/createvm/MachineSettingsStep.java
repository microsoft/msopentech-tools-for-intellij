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
import java.awt.*;
import java.text.DateFormat;
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


    public MachineSettingsStep(CreateVMWizardModel model, Project project) {
        super("Virtual Machine Basic Settings", null);

        this.project = project;
        this.model = model;

        model.configStepList(createVmStepsList, 2);

        scrollPanel.remove(imageInfoPanel);
        final JBScrollPane jbScrollPane = new JBScrollPane(imageInfoPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.add(jbScrollPane);
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        model.getCurrentNavigationState().NEXT.setEnabled(false);

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
                try {
                    final List<VirtualMachineSize> virtualMachineSizes = AzureSDKManagerImpl.getManager().getVirtualMachineSizes(model.getSubscription().getId().toString());

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
