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
import com.microsoftopentechnologies.intellij.model.vm.CloudService;
import com.microsoftopentechnologies.intellij.model.vm.StorageAccount;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineSize;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.util.List;

public class CloudServiceStep extends WizardStep<CreateVMWizardModel> {

    private Project project;
    private CreateVMWizardModel model;
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
    private JComboBox cloudServiceComboBox;
    private JComboBox storageComboBox;
    private JCheckBox availabilitySetCheckBox;
    private JComboBox availabilityComboBox;

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



    public CloudServiceStep(CreateVMWizardModel mModel, Project project) {

        this.project = project;
        this.model = mModel;


        model.configStepList(createVmStepsList, 3);

        scrollPanel.remove(imageInfoPanel);
        final JBScrollPane jbScrollPane = new JBScrollPane(imageInfoPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.add(jbScrollPane);


    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        final VirtualMachineImage virtualMachineImage = model.getVirtualMachineImage();

        imageTitleTextPane.setText(virtualMachineImage.getLabel());
        imageDescriptionTextPane.setText(virtualMachineImage.getDescription());
        imagePublishedDateTextPane.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(virtualMachineImage.getPublishedDate().getTime()));
        imagePublisherTextPane.setText(virtualMachineImage.getPublisherName());
        imageOSFamilyTextPane.setText(virtualMachineImage.getOperatingSystemType());
        imageLocationTextPane.setText(virtualMachineImage.getLocation());

        imageTitleTextPane.setCaretPosition(0);




        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading cloud services...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {

                    final List<CloudService> cloudServices = AzureSDKManagerImpl.getManager().getCloudServices(model.getSubscription().getId().toString());

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {

                            cloudServiceComboBox.setModel(new DefaultComboBoxModel(cloudServices.toArray()));

                            cloudServiceComboBox.insertItemAt("<< Create new cloud service >>", 0);
                        }
                    });

                } catch (AzureCmdException e) {
                    UIHelper.showException("Error trying to get VM sizes", e);
                }
            }
        });

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage account...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    final List<StorageAccount> storageAccounts = AzureSDKManagerImpl.getManager().getStorageAccounts(model.getSubscription().getId().toString());

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            storageComboBox.setModel(new DefaultComboBoxModel(storageAccounts.toArray()));

                            storageComboBox.insertItemAt("<< Create new storage account >>", 0);
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
