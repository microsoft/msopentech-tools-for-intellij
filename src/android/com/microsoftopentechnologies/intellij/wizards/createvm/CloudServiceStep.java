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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.util.List;
import java.util.Vector;

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
    private List<StorageAccount> storageAccounts;
    private List<CloudService> cloudServices;

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



    public CloudServiceStep(CreateVMWizardModel mModel, final Project project) {

        super("Cloud Service Settings");

        this.project = project;
        this.model = mModel;

        model.configStepList(createVmStepsList, 3);

        scrollPanel.remove(imageInfoPanel);
        final JBScrollPane jbScrollPane = new JBScrollPane(imageInfoPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.add(jbScrollPane);

        cloudServiceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                fillStorage();
                fillAvailabilitySets();
            }
        });

        cloudServiceComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof CloudService) {
                    CloudService sa = (CloudService) o;
                    return super.getListCellRendererComponent(jList, String.format("%s (%s)", sa.getName(), sa.getLocation()), i, b, b1);
                } else {
                    return super.getListCellRendererComponent(jList, o, i, b, b1);
                }
            }
        });

        storageComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                if(o instanceof StorageAccount) {
                    StorageAccount sa = (StorageAccount) o;
                    return super.getListCellRendererComponent(jList, String.format("%s (%s)", sa.getName(), sa.getLocation()), i, b, b1);
                } else {
                    return super.getListCellRendererComponent(jList, o, i, b, b1);
                }
            }
        });

        storageComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                model.getCurrentNavigationState().NEXT.setEnabled(storageComboBox.getSelectedItem() instanceof StorageAccount);
            }
        });

        availabilitySetCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                availabilityComboBox.setEnabled(availabilitySetCheckBox.isSelected());
            }
        });


    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage account...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    storageAccounts = AzureSDKManagerImpl.getManager().getStorageAccounts(model.getSubscription().getId().toString());

                } catch (AzureCmdException e) {
                    UIHelper.showException("Error trying to get storage account list", e);
                }
            }
        });

        final VirtualMachineImage virtualMachineImage = model.getVirtualMachineImage();

        imageTitleTextPane.setText(virtualMachineImage.getLabel());
        imageDescriptionTextPane.setText(virtualMachineImage.getDescription());
        imagePublishedDateTextPane.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(virtualMachineImage.getPublishedDate().getTime()));
        imagePublisherTextPane.setText(virtualMachineImage.getPublisherName());
        imageOSFamilyTextPane.setText(virtualMachineImage.getOperatingSystemType());
        imageLocationTextPane.setText(virtualMachineImage.getLocation());

        imageTitleTextPane.setCaretPosition(0);

        fillCloudServices();

        return rootPanel;

    }

    private void fillCloudServices() {
        cloudServiceComboBox.setModel(new DefaultComboBoxModel(new String[] { "<Loading...>" }));

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading cloud services...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {

                    if(cloudServices == null) {
                        cloudServices = AzureSDKManagerImpl.getManager().getCloudServices(model.getSubscription().getId().toString());
                    }

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {

                            cloudServiceComboBox.setModel(new DefaultComboBoxModel(cloudServices.toArray()){
                                @Override
                                public void setSelectedItem(Object o) {
                                    if(o instanceof String) {
                                        final CreateCloudServiceForm form = new CreateCloudServiceForm();
                                        form.fillFields(model.getSubscription(), project);
                                        UIHelper.packAndCenterJDialog(form);

                                        form.setOnCreate(new Runnable() {
                                            @Override
                                            public void run() {
                                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        cloudServices.add(form.getCloudService());
                                                        fillCloudServices();
                                                    }
                                                });
                                            }
                                        });

                                        form.setVisible(true);

                                    } else {
                                        super.setSelectedItem(o);
                                    }
                                }

                            });

                            cloudServiceComboBox.insertItemAt("<< Create new cloud service >>", 0);
                        }
                    });

                } catch (AzureCmdException e) {
                    UIHelper.showException("Error trying to get cloud services list", e);
                }
            }
        });
    }

    private void fillStorage() {
        if(cloudServiceComboBox.getSelectedItem() instanceof CloudService) {

            Vector<StorageAccount> accounts = new Vector<StorageAccount>();
            CloudService selectedItem = (CloudService) cloudServiceComboBox.getSelectedItem();

            for (StorageAccount storageAccount : storageAccounts) {
                if (storageAccount.getLocation().equals(selectedItem.getLocation())) {
                    accounts.add(storageAccount);
                }
            }

            storageComboBox.setModel(new DefaultComboBoxModel(accounts) {
                @Override
                public void setSelectedItem(Object o) {
                    if (o instanceof String) {

                        final CreateStorageAccountForm form = new CreateStorageAccountForm();
                        form.fillFields(model.getSubscription(), project);
                        UIHelper.packAndCenterJDialog(form);

                        form.setOnCreate(new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        storageAccounts.add(form.getStorageAccount());
                                        fillStorage();
                                    }
                                });

                            }
                        });

                        form.setVisible(true);

                    } else {
                        super.setSelectedItem(o);
                    }
                }
            });
        }

        storageComboBox.insertItemAt("<< Create new storage account >>", 0);
    }

    private void fillAvailabilitySets() {
        if(cloudServiceComboBox.getSelectedItem() instanceof CloudService) {
            CloudService selectedItem = (CloudService) cloudServiceComboBox.getSelectedItem();
            availabilityComboBox.setModel(new DefaultComboBoxModel(selectedItem.getAvailabilitySets().toArray()));
        } else {
            availabilityComboBox.setModel(new DefaultComboBoxModel(new String[] {}));
        }
    }
}
