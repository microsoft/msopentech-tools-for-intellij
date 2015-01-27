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
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.vm.CloudService;
import com.microsoftopentechnologies.intellij.model.vm.StorageAccount;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

public class CloudServiceStep extends WizardStep<CreateVMWizardModel> {

    private Project project;
    private CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JEditorPane imageDescriptionTextPane;
    private JComboBox cloudServiceComboBox;
    private JComboBox storageComboBox;
    private JCheckBox availabilitySetCheckBox;
    private JComboBox availabilityComboBox;
    private List<StorageAccount> storageAccounts;
    private List<CloudService> cloudServices;


    public CloudServiceStep(CreateVMWizardModel mModel, final Project project) {

        super("Cloud Service Settings");

        this.project = project;
        this.model = mModel;

        model.configStepList(createVmStepsList, 3);

        cloudServiceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                fillStorage(null);
                fillAvailabilitySets();
            }
        });

        cloudServiceComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
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
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof StorageAccount) {
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

        imageDescriptionTextPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if(hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if(Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                        } catch (Exception e) {
                            UIHelper.showException("Error opening link", e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        model.getCurrentNavigationState().NEXT.setEnabled(false);

        if(storageAccounts == null) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage account...", false) {
                @Override
                public void run(ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);

                        storageAccounts = AzureSDKManagerImpl.getManager().getStorageAccounts(model.getSubscription().getId().toString());

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fillStorage(null);
                            }
                        });

                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error trying to get storage account list", e);
                    }
                }
            });
        }

        final VirtualMachineImage virtualMachineImage = model.getVirtualMachineImage();
        imageDescriptionTextPane.setText(model.getHtmlFromVMImage(virtualMachineImage));
        imageDescriptionTextPane.setCaretPosition(0);

        fillCloudServices(null);
        fillStorage(null);

        return rootPanel;

    }

    @Override
    public WizardStep onNext(CreateVMWizardModel model) {
        if(!(storageComboBox.getSelectedItem() instanceof StorageAccount)) {
            JOptionPane.showMessageDialog(null, "Must select a storage account", "Error creating the virtual machine", JOptionPane.ERROR_MESSAGE);
            return this;
        }

        model.setCloudService((CloudService) cloudServiceComboBox.getSelectedItem());
        model.setStorageAccount((StorageAccount) storageComboBox.getSelectedItem());
        model.setAvailabilitySet(availabilitySetCheckBox.isSelected() ?
               (availabilityComboBox.getSelectedItem() == null
                ? availabilityComboBox.getEditor().getItem().toString()
                : availabilityComboBox.getSelectedItem().toString())
            : "");

        return super.onNext(model);
    }

    private void fillCloudServices(final CloudService selected) {
        cloudServiceComboBox.setModel(new DefaultComboBoxModel(new String[] { "<Loading...>" }));

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading cloud services...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);

                    if (cloudServices == null) {
                        cloudServices = AzureSDKManagerImpl.getManager().getCloudServices(model.getSubscription().getId().toString());
                    }

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fillStorage(null);

                            cloudServiceComboBox.setModel(new DefaultComboBoxModel(cloudServices.toArray()) {
                                @Override
                                public void setSelectedItem(Object o) {
                                    if (o instanceof String) {
                                        if (o.toString().trim().length() > 0) {
                                            showNewCloudServiceForm();
                                        }
                                    } else {
                                        super.setSelectedItem(o);
                                        fillStorage(null);
                                    }
                                }

                            });

                            cloudServiceComboBox.insertItemAt("<< Create new cloud service >>", 0);

                            if(selected != null) {
                                cloudServiceComboBox.setSelectedItem(selected);
                            }
                        }
                    });

                } catch (AzureCmdException e) {
                    UIHelper.showException("Error trying to get cloud services list", e);
                }
            }
        });
    }

    private void fillStorage(StorageAccount selected) {

        Object item = cloudServiceComboBox.getSelectedItem();

        if(!(item instanceof CloudService) && cloudServices != null && cloudServices.size() > 0) {
            item = cloudServices.get(0);
        }

        Vector<StorageAccount> accounts = new Vector<StorageAccount>();

        if(item != null && storageAccounts != null && item instanceof CloudService) {
            CloudService selectedItem = (CloudService) item;

            for (StorageAccount storageAccount : storageAccounts) {
                if (storageAccount.getLocation().equals(selectedItem.getLocation())) {
                    accounts.add(storageAccount);
                }
            }

        }


        storageComboBox.setModel(new DefaultComboBoxModel(accounts) {
            @Override
            public void setSelectedItem(Object o) {
                if (o instanceof String) {
                    if(o.toString().trim().length() > 0) {
                        showNewStorageForm();
                    }
                } else {
                    super.setSelectedItem(o);
                }
            }
        });

        storageComboBox.insertItemAt("<< Create new storage account >>", 0);

        if(selected != null) {
            storageComboBox.setSelectedItem(selected);
        }

        model.getCurrentNavigationState().NEXT.setEnabled(accounts.size() > 0);
    }

    private void fillAvailabilitySets() {
        if(cloudServiceComboBox.getSelectedItem() instanceof CloudService) {
            CloudService selectedItem = (CloudService) cloudServiceComboBox.getSelectedItem();
            availabilityComboBox.setModel(new DefaultComboBoxModel(selectedItem.getAvailabilitySets().toArray()));
        } else {
            availabilityComboBox.setModel(new DefaultComboBoxModel(new String[] {}));
        }
    }

    private void showNewCloudServiceForm() {
        final CreateCloudServiceForm form = new CreateCloudServiceForm();
        form.fillFields(model.getSubscription(), project);
        UIHelper.packAndCenterJDialog(form);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        CloudService newCloudService = form.getCloudService();
                        cloudServices.add(newCloudService);
                        fillCloudServices(newCloudService);
                    }
                });
            }
        });
        form.setVisible(true);
    }

    private void showNewStorageForm() {
        final CreateStorageAccountForm form = new CreateStorageAccountForm();
        form.fillFields(model.getSubscription(), project);
        UIHelper.packAndCenterJDialog(form);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        StorageAccount newStorageAccount = form.getStorageAccount();
                        storageAccounts.add(newStorageAccount);
                        fillStorage(newStorageAccount);
                    }
                });
            }
        });

        form.setVisible(true);
    }
}
