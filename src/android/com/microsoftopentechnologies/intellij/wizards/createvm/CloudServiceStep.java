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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.TreeMap;
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
    private Map<String, StorageAccount> storageAccounts;
    private Map<String, CloudService> cloudServices;
    private final Object csMonitor = new Object();
    private final Object saMonitor = new Object();

    public CloudServiceStep(CreateVMWizardModel mModel, final Project project) {
        super("Cloud Service Settings");

        this.project = project;
        this.model = mModel;

        model.configStepList(createVmStepsList, 3);

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
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
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

        final VirtualMachineImage virtualMachineImage = model.getVirtualMachineImage();
        imageDescriptionTextPane.setText(model.getHtmlFromVMImage(virtualMachineImage));
        imageDescriptionTextPane.setCaretPosition(0);

        fillCloudServices(null);
        fillStorage(null, null);

        return rootPanel;
    }

    @Override
    public WizardStep onNext(CreateVMWizardModel model) {
        if (!(storageComboBox.getSelectedItem() instanceof StorageAccount)) {
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
        if (cloudServices == null) {
            final String createCS = "<< Create new cloud service >>";

            DefaultComboBoxModel loadingCSModel = new DefaultComboBoxModel(
                    new String[]{createCS, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (createCS.equals(o)) {
                        showNewCloudServiceForm();
                    } else {
                        super.setSelectedItem(o);
                    }
                }
            };

            loadingCSModel.setSelectedItem(null);

            cloudServiceComboBox.setModel(loadingCSModel);

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading cloud services...", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);

                        synchronized (csMonitor) {
                            if (cloudServices == null) {
                                cloudServices = new TreeMap<String, CloudService>();

                                for (CloudService cloudService : AzureSDKManagerImpl.getManager().getCloudServices(model.getSubscription().getId().toString())) {
                                    if (cloudService.isProductionDeploymentVM()) {
                                        cloudServices.put(cloudService.getName(), cloudService);
                                    }
                                }
                            }
                        }

                        refreshCloudServices(selected);
                    } catch (AzureCmdException e) {
                        cloudServices = null;
                        UIHelper.showException("Error trying to get cloud services list", e);
                    }
                }
            });
        } else {
            refreshCloudServices(selected);
        }
    }

    private void refreshCloudServices(final CloudService selected) {
        if (selected != null && !cloudServices.containsKey(selected.getName())) {
            cloudServices.put(selected.getName(), selected);
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final String createCS = "<< Create new cloud service >>";

                DefaultComboBoxModel refreshedCSModel = new DefaultComboBoxModel(cloudServices.values().toArray()) {
                    @Override
                    public void setSelectedItem(Object o) {
                        if (createCS.equals(o)) {
                            showNewCloudServiceForm();
                        } else if (o instanceof CloudService) {
                            super.setSelectedItem(o);
                            fillStorage((CloudService) o, null);
                            fillAvailabilitySets((CloudService) o);
                        } else {
                            super.setSelectedItem(o);
                            fillStorage(null, null);
                            fillAvailabilitySets(null);
                        }
                    }
                };

                refreshedCSModel.insertElementAt(createCS, 0);
                refreshedCSModel.setSelectedItem(selected);

                cloudServiceComboBox.setModel(refreshedCSModel);
            }
        });
    }

    private void fillStorage(final CloudService selectedCS, final StorageAccount selectedSA) {
        if (storageAccounts == null) {
            final String createSA = "<< Create new storage account >>";

            DefaultComboBoxModel loadingSAModel = new DefaultComboBoxModel(
                    new String[]{createSA, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (createSA.equals(o)) {
                        showNewCloudServiceForm();
                    } else {
                        super.setSelectedItem(o);
                    }
                }
            };

            loadingSAModel.setSelectedItem(null);

            storageComboBox.setModel(loadingSAModel);

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage account...", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);

                        synchronized (saMonitor) {
                            if (storageAccounts == null) {
                                storageAccounts = new TreeMap<String, StorageAccount>();

                                for (StorageAccount storageAccount : AzureSDKManagerImpl.getManager().getStorageAccounts(model.getSubscription().getId().toString())) {
                                    storageAccounts.put(storageAccount.getName(), storageAccount);
                                }
                            }
                        }

                        refreshStorageAccounts(selectedCS, selectedSA);
                    } catch (AzureCmdException e) {
                        storageAccounts = null;
                        UIHelper.showException("Error trying to get storage account list", e);
                    }
                }
            });
        } else {
            refreshStorageAccounts(selectedCS, selectedSA);
        }
    }

    private void refreshStorageAccounts(final CloudService selectedCS, final StorageAccount selectedSA) {
        if (selectedSA != null && !storageAccounts.containsKey(selectedSA.getName())) {
            storageAccounts.put(selectedSA.getName(), selectedSA);
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Vector<StorageAccount> accounts = new Vector<StorageAccount>();

                if (selectedCS != null) {
                    for (StorageAccount storageAccount : storageAccounts.values()) {
                        if (storageAccount.getLocation().equals(selectedCS.getLocation())) {
                            accounts.add(storageAccount);
                        }
                    }
                }

                final String createSA = "<< Create new storage account >>";

                DefaultComboBoxModel refreshedSAModel = new DefaultComboBoxModel(accounts) {
                    @Override
                    public void setSelectedItem(Object o) {
                        if (createSA.equals(o)) {
                            showNewStorageForm(selectedCS);
                        } else {
                            super.setSelectedItem(o);
                        }
                    }
                };

                refreshedSAModel.insertElementAt(createSA, 0);

                if (selectedCS != null && selectedSA != null && selectedSA.getLocation().equals(selectedCS.getLocation())) {
                    refreshedSAModel.setSelectedItem(selectedSA);
                    model.getCurrentNavigationState().NEXT.setEnabled(true);
                } else {
                    refreshedSAModel.setSelectedItem(null);
                    model.getCurrentNavigationState().NEXT.setEnabled(false);
                }

                storageComboBox.setModel(refreshedSAModel);
            }
        });
    }

    private void fillAvailabilitySets(CloudService selectedCS) {
        if (selectedCS != null) {
            availabilityComboBox.setModel(new DefaultComboBoxModel(selectedCS.getAvailabilitySets().toArray()));
        } else {
            availabilityComboBox.setModel(new DefaultComboBoxModel(new String[]{}));
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
                        if(newCloudService != null) {
                            fillCloudServices(newCloudService);
                        }
                    }
                });
            }
        });

        form.setVisible(true);
    }

    private void showNewStorageForm(final CloudService selectedCS) {
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
                        if(newStorageAccount != null) {
                            fillStorage(selectedCS, newStorageAccount);
                        }
                    }
                });
            }
        });

        form.setVisible(true);
    }
}