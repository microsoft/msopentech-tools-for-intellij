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
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CreateCloudServiceForm;
import com.microsoftopentechnologies.intellij.forms.CreateStorageAccountForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.vm.CloudService;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.vm.VirtualMachineImage;
import com.microsoftopentechnologies.tooling.msservices.model.vm.VirtualNetwork;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CloudServiceStep extends WizardStep<CreateVMWizardModel> {
    private static final String PRODUCTION = "Production";

    private Project project;
    private CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JEditorPane imageDescriptionTextPane;
    private JComboBox cloudServiceComboBox;
    private JComboBox storageComboBox;
    private JCheckBox availabilitySetCheckBox;
    private JComboBox availabilityComboBox;
    private JComboBox networkComboBox;
    private JComboBox subnetComboBox;

    private Map<String, CloudService> cloudServices;
    private final Lock csLock = new ReentrantLock();
    private final Condition csInitialized = csLock.newCondition();

    private Map<String, VirtualNetwork> virtualNetworks;
    private final Lock vnLock = new ReentrantLock();
    private final Condition vnInitialized = vnLock.newCondition();

    private Map<String, StorageAccount> storageAccounts;
    private final Lock saLock = new ReentrantLock();
    private final Condition saInitialized = saLock.newCondition();

    public CloudServiceStep(CreateVMWizardModel mModel, final Project project) {
        super("Cloud Service Settings", null, null);

        this.project = project;
        this.model = mModel;

        model.configStepList(createVmStepsList, 3);

        cloudServiceComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof CloudService) {
                    CloudService cs = (CloudService) o;

                    if (cs.getProductionDeployment().getVirtualNetwork().isEmpty()) {
                        setText(String.format("%s (%s)", cs.getName(),
                                !cs.getLocation().isEmpty() ? cs.getLocation() : cs.getAffinityGroup()));
                    } else {
                        setText(String.format("%s (%s - %s)", cs.getName(),
                                cs.getProductionDeployment().getVirtualNetwork(),
                                !cs.getLocation().isEmpty() ? cs.getLocation() : cs.getAffinityGroup()));
                    }
                }
            }
        });

        storageComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof StorageAccount) {
                    StorageAccount sa = (StorageAccount) o;
                    setText(String.format("%s (%s)", sa.getName(),
                            !sa.getLocation().isEmpty() ? sa.getLocation() : sa.getAffinityGroup()));
                }
            }
        });

        storageComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateNext();
            }
        });

        networkComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualNetwork) {
                    VirtualNetwork vn = (VirtualNetwork) o;
                    setText(String.format("%s (%s)", vn.getName(), vn.getLocation()));
                }
            }
        });

        subnetComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                model.setSubnet((String) subnetComboBox.getSelectedItem());
                validateNext();
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
                            DefaultLoader.getUIHelper().showException("An error occurred while trying to open the specified Link",
                                    e, "Error Opening Link", false, true);
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

        retrieveCloudServices(model.getVirtualNetwork(), model.isFilterByCloudService());
        retrieveVirtualNetworks();
        retrieveStorageAccounts(model.getCloudService());

        if (model.isFilterByCloudService()) {
            fillCloudServices(null, true);
        } else {
            fillVirtualNetworks(null, true);
        }

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
        model.setVirtualNetwork((VirtualNetwork) networkComboBox.getSelectedItem());
        model.setSubnet(subnetComboBox.isEnabled() && subnetComboBox.getSelectedItem() != null ?
                subnetComboBox.getSelectedItem().toString() :
                "");
        model.setAvailabilitySet(availabilitySetCheckBox.isSelected() ?
                (availabilityComboBox.getSelectedItem() == null
                        ? availabilityComboBox.getEditor().getItem().toString()
                        : availabilityComboBox.getSelectedItem().toString())
                : "");

        return super.onNext(model);
    }

    private void retrieveCloudServices(final VirtualNetwork selectedVN, final boolean cascade) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading cloud services...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                csLock.lock();

                try {
                    if (cloudServices == null) {
                        try {
                            List<CloudService> services = AzureSDKManagerImpl.getManager()
                                    .getCloudServices(model.getSubscription().getId().toString());
                            cloudServices = new TreeMap<String, CloudService>();

                            for (CloudService cloudService : services) {
                                if (cloudService.getProductionDeployment().getComputeRoles().size() == 0) {
                                    cloudServices.put(cloudService.getName(), cloudService);
                                }
                            }

                            csInitialized.signalAll();
                        } catch (AzureCmdException e) {
                            cloudServices = null;
                            DefaultLoader.getUIHelper().showException("An error occurred while trying to retrieve the cloud services list",
                                    e, "Error Retrieving Cloud Services", false, true);
                        }
                    }
                } finally {
                    csLock.unlock();
                }
            }
        });

        if (cloudServices == null) {
            final String createCS = "<< Create new cloud service >>";

            final DefaultComboBoxModel loadingCSModel = new DefaultComboBoxModel(
                    new String[]{createCS, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (createCS.equals(o)) {
                        showNewCloudServiceForm(selectedVN, cascade);
                    } else {
                        super.setSelectedItem(o);
                    }
                }
            };

            loadingCSModel.setSelectedItem(null);
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    cloudServiceComboBox.setModel(loadingCSModel);
                }
            }, ModalityState.any());
        }
    }

    private void fillCloudServices(final VirtualNetwork selectedVN,
                                   final boolean cascade) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                CloudService selectedCS = model.getCloudService();
                model.setFilterByCloudService(cascade);

                csLock.lock();

                try {
                    while (cloudServices == null) {
                        csInitialized.await();
                    }

                    if (selectedCS != null && !cloudServices.containsKey(selectedCS.getName())) {
                        cloudServices.put(selectedCS.getName(), selectedCS);
                    }
                } catch (InterruptedException e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying to load the cloud services list", e,
                            "Error Loading Cloud Services", false, true);
                } finally {
                    csLock.unlock();
                }

                refreshCloudServices(selectedCS, selectedVN, cascade);
            }
        });
    }

    private void refreshCloudServices(CloudService selectedCS,
                                      VirtualNetwork selectedVN,
                                      boolean cascade) {
        final DefaultComboBoxModel refreshedCSModel = getCloudServiceModel(selectedCS, selectedVN, cascade);

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                cloudServiceComboBox.setModel(refreshedCSModel);
            }
        }, ModalityState.any());
    }

    private DefaultComboBoxModel getCloudServiceModel(CloudService selectedCS,
                                                      final VirtualNetwork selectedVN,
                                                      final boolean cascade) {
        Collection<CloudService> services = filterCS(selectedVN);

        final String createCS = "<< Create new cloud service >>";

        DefaultComboBoxModel refreshedCSModel = new DefaultComboBoxModel(services.toArray()) {
            private final String clear = "(Clear selection...)";
            private boolean doCascade = cascade;

            @Override
            public void setSelectedItem(Object o) {
                if (clear.equals(o)) {
                    removeElement(o);
                    setSelectedItem(null);
                } else {
                    if (createCS.equals(o)) {
                        showNewCloudServiceForm(selectedVN, doCascade);
                    } else if (o instanceof CloudService) {
                        super.setSelectedItem(o);
                        model.setCloudService((CloudService) o);

                        if (getIndexOf(clear) == -1) {
                            addElement(clear);
                        }

                        if (doCascade) {
                            fillVirtualNetworks((CloudService) o, false);
                        }

                        fillStorage((CloudService) o);
                        fillAvailabilitySets((CloudService) o);
                    } else {
                        super.setSelectedItem(o);
                        model.setCloudService(null);

                        if (doCascade) {
                            fillVirtualNetworks(null, false);
                        }

                        fillStorage(null);
                        fillAvailabilitySets(null);
                    }

                    doCascade = doCascade || selectedVN == null;
                }
            }
        };

        refreshedCSModel.insertElementAt(createCS, 0);

        if (selectedCS != null && services.contains(selectedCS) && (cascade || selectedVN != null)) {
            refreshedCSModel.setSelectedItem(selectedCS);
        } else {
            model.setCloudService(null);
            refreshedCSModel.setSelectedItem(null);
        }

        return refreshedCSModel;
    }

    private Collection<CloudService> filterCS(VirtualNetwork selectedVN) {
        Collection<CloudService> services = selectedVN == null ? cloudServices.values() : new Vector<CloudService>();

        if (selectedVN != null) {
            for (CloudService cloudService : cloudServices.values()) {
                if ((isDeploymentEmpty(cloudService, PRODUCTION) && areSameRegion(cloudService, selectedVN)) ||
                        areSameNetwork(cloudService, selectedVN)) {
                    services.add(cloudService);
                }
            }
        }

        return services;
    }

    private void retrieveVirtualNetworks() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading virtual networks...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                vnLock.lock();

                try {
                    if (virtualNetworks == null) {
                        try {
                            List<VirtualNetwork> networks = AzureSDKManagerImpl.getManager()
                                    .getVirtualNetworks(model.getSubscription().getId().toString());
                            virtualNetworks = new TreeMap<String, VirtualNetwork>();

                            for (VirtualNetwork virtualNetwork : networks) {
                                virtualNetworks.put(virtualNetwork.getName(), virtualNetwork);
                            }

                            vnInitialized.signalAll();
                        } catch (AzureCmdException e) {
                            virtualNetworks = null;
                            DefaultLoader.getUIHelper().showException("An error occurred while trying to retrieve the virtual networks list",
                                    e, "Error Retrieving Virtual Networks", false, true);
                        }
                    }
                } finally {
                    vnLock.unlock();
                }
            }
        });

        if (virtualNetworks == null) {
            final DefaultComboBoxModel loadingVNModel = new DefaultComboBoxModel(
                    new String[]{"<Loading...>"});

            loadingVNModel.setSelectedItem(null);

            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    networkComboBox.setModel(loadingVNModel);

                    subnetComboBox.removeAllItems();
                    subnetComboBox.setEnabled(false);
                }
            }, ModalityState.any());
        }
    }

    private void fillVirtualNetworks(final CloudService selectedCS,
                                     final boolean cascade) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                VirtualNetwork selectedVN = model.getVirtualNetwork();
                String selectedSN = model.getSubnet();

                vnLock.lock();

                try {
                    while (virtualNetworks == null) {
                        vnInitialized.await();
                    }
                } catch (InterruptedException e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying load the virtual networks list", e,
                            "Error Loading Virtual Networks", false, true);
                } finally {
                    vnLock.unlock();
                }

                refreshVirtualNetworks(selectedCS, selectedVN, selectedSN, cascade);
            }
        });
    }

    private void refreshVirtualNetworks(final CloudService selectedCS,
                                        VirtualNetwork selectedVN,
                                        String selectedSN,
                                        boolean cascade) {
        final DefaultComboBoxModel refreshedVNModel = getVirtualNetworkModel(selectedCS, selectedVN, selectedSN, cascade);

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                networkComboBox.setModel(refreshedVNModel);
                networkComboBox.setEnabled(selectedCS == null || isDeploymentEmpty(selectedCS, PRODUCTION));
            }
        }, ModalityState.any());
    }

    private DefaultComboBoxModel getVirtualNetworkModel(final CloudService selectedCS,
                                                        VirtualNetwork selectedVN,
                                                        final String selectedSN,
                                                        final boolean cascade) {
        Vector<VirtualNetwork> networks = filterVN(selectedCS);

        if (selectedCS != null && !selectedCS.getProductionDeployment().getVirtualNetwork().isEmpty()) {
            selectedVN = networks.size() == 1 ? networks.get(0) : null;
        }

        DefaultComboBoxModel refreshedVNModel = new DefaultComboBoxModel(networks) {
            private final String none = "(None)";
            private boolean doCascade = cascade;

            @Override
            public void setSelectedItem(final Object o) {
                if (none.equals(o)) {
                    removeElement(o);
                    setSelectedItem(null);
                } else {
                    super.setSelectedItem(o);

                    if (o instanceof VirtualNetwork) {
                        model.setVirtualNetwork((VirtualNetwork) o);

                        if (getIndexOf(none) == -1) {
                            insertElementAt(none, 0);
                        }

                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                boolean validSubnet = false;

                                subnetComboBox.removeAllItems();

                                for (String subnet : ((VirtualNetwork) o).getSubnets()) {
                                    subnetComboBox.addItem(subnet);

                                    if (subnet.equals(selectedSN)) {
                                        validSubnet = true;
                                    }
                                }

                                if (validSubnet) {
                                    subnetComboBox.setSelectedItem(selectedSN);
                                } else {
                                    model.setSubnet(null);
                                    subnetComboBox.setSelectedItem(null);
                                }

                                subnetComboBox.setEnabled(true);
                            }
                        }, ModalityState.any());

                        if (doCascade) {
                            fillCloudServices((VirtualNetwork) o, false);
                        }
                    } else {
                        model.setVirtualNetwork(null);

                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                subnetComboBox.removeAllItems();
                                subnetComboBox.setEnabled(false);
                            }
                        }, ModalityState.any());

                        if (doCascade) {
                            fillCloudServices(null, false);
                        }
                    }

                    doCascade = doCascade || selectedCS == null;
                }
            }
        };

        if (selectedVN != null && networks.contains(selectedVN) && (cascade || selectedCS != null)) {
            refreshedVNModel.setSelectedItem(selectedVN);
        } else {
            model.setVirtualNetwork(null);
            refreshedVNModel.setSelectedItem(null);
        }

        return refreshedVNModel;
    }

    private Vector<VirtualNetwork> filterVN(CloudService selectedCS) {
        Vector<VirtualNetwork> networks = selectedCS == null ?
                new Vector<VirtualNetwork>(virtualNetworks.values()) :
                new Vector<VirtualNetwork>();

        if (selectedCS != null) {
            if (isDeploymentEmpty(selectedCS, PRODUCTION)) {
                for (VirtualNetwork virtualNetwork : virtualNetworks.values()) {
                    if (areSameRegion(selectedCS, virtualNetwork)) {
                        networks.add(virtualNetwork);
                    }
                }
            } else if (!selectedCS.getProductionDeployment().getVirtualNetwork().isEmpty()) {
                for (VirtualNetwork virtualNetwork : virtualNetworks.values()) {
                    if (areSameNetwork(selectedCS, virtualNetwork)) {
                        networks.add(virtualNetwork);
                        break;
                    }
                }
            }
        }

        return networks;
    }

    private void retrieveStorageAccounts(final CloudService selectedCS) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage accounts...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                saLock.lock();

                try {
                    if (storageAccounts == null) {
                        try {
                            List<StorageAccount> accounts = AzureSDKManagerImpl.getManager()
                                    .getStorageAccounts(model.getSubscription().getId().toString());
                            storageAccounts = new TreeMap<String, StorageAccount>();

                            for (StorageAccount storageAccount : accounts) {
                                storageAccounts.put(storageAccount.getName(), storageAccount);
                            }

                            saInitialized.signalAll();
                        } catch (AzureCmdException e) {
                            storageAccounts = null;
                            DefaultLoader.getUIHelper().showException("An error occurred while trying to retrieve the storage accounts list",
                                    e, "Error Retrieving Storage Accounts", false, true);
                        }
                    }
                } finally {
                    saLock.unlock();
                }
            }
        });

        if (storageAccounts == null) {
            final String createSA = "<< Create new storage account >>";

            final DefaultComboBoxModel loadingSAModel = new DefaultComboBoxModel(
                    new String[]{createSA, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (createSA.equals(o)) {
                        showNewStorageForm(selectedCS);
                    } else {
                        super.setSelectedItem(o);
                    }
                }
            };

            loadingSAModel.setSelectedItem(null);

            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    storageComboBox.setModel(loadingSAModel);
                }
            }, ModalityState.any());
        }
    }

    private void fillStorage(final CloudService selectedCS) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                StorageAccount selectedSA = model.getStorageAccount();

                saLock.lock();

                try {
                    while (storageAccounts == null) {
                        saInitialized.await();
                    }

                    if (selectedSA != null && !storageAccounts.containsKey(selectedSA.getName())) {
                        storageAccounts.put(selectedSA.getName(), selectedSA);
                    }
                } catch (InterruptedException e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying load the storage accounts list", e,
                            "Error Loading Storage Accounts", false, true);
                } finally {
                    saLock.unlock();
                }

                refreshStorageAccounts(selectedCS, selectedSA);
            }
        });
    }

    private void refreshStorageAccounts(final CloudService selectedCS, final StorageAccount selectedSA) {
        final DefaultComboBoxModel refreshedSAModel = getStorageAccountModel(selectedCS, selectedSA);

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                storageComboBox.setModel(refreshedSAModel);
                model.getCurrentNavigationState().NEXT.setEnabled(selectedCS != null &&
                        selectedSA != null &&
                        selectedSA.getLocation().equals(selectedCS.getLocation()));
            }
        }, ModalityState.any());
    }

    private DefaultComboBoxModel getStorageAccountModel(final CloudService selectedCS, StorageAccount selectedSA) {
        Vector<StorageAccount> accounts = filterSA(selectedCS);

        final String createSA = "<< Create new storage account >>";

        final DefaultComboBoxModel refreshedSAModel = new DefaultComboBoxModel(accounts) {
            @Override
            public void setSelectedItem(Object o) {
                if (createSA.equals(o)) {
                    showNewStorageForm(selectedCS);
                } else {
                    super.setSelectedItem(o);
                    model.setStorageAccount((StorageAccount) o);
                }
            }
        };

        refreshedSAModel.insertElementAt(createSA, 0);

        if (accounts.contains(selectedSA)) {
            refreshedSAModel.setSelectedItem(selectedSA);
        } else {
            refreshedSAModel.setSelectedItem(null);
            model.setStorageAccount(null);
        }

        return refreshedSAModel;
    }

    private Vector<StorageAccount> filterSA(CloudService selectedCS) {
        Vector<StorageAccount> accounts = new Vector<StorageAccount>();

        if (selectedCS != null) {
            for (StorageAccount storageAccount : storageAccounts.values()) {
                if ((!storageAccount.getLocation().isEmpty() &&
                        storageAccount.getLocation().equals(selectedCS.getLocation())) ||
                        (!storageAccount.getAffinityGroup().isEmpty() &&
                                storageAccount.getAffinityGroup().equals(selectedCS.getAffinityGroup()))) {
                    accounts.add(storageAccount);
                }
            }
        }

        return accounts;
    }

    private void fillAvailabilitySets(final CloudService selectedCS) {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (selectedCS != null) {
                    availabilityComboBox.setModel(new DefaultComboBoxModel(selectedCS.getProductionDeployment().getAvailabilitySets().toArray()));
                } else {
                    availabilityComboBox.setModel(new DefaultComboBoxModel(new String[]{}));
                }
            }
        }, ModalityState.any());
    }

    private void showNewCloudServiceForm(final VirtualNetwork selectedVN, final boolean cascade) {
        final CreateCloudServiceForm form = new CreateCloudServiceForm();
        form.fillFields(model.getSubscription(), project);
        DefaultLoader.getUIHelper().packAndCenterJDialog(form);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        CloudService newCloudService = form.getCloudService();
                        if (newCloudService != null) {
                            model.setCloudService(newCloudService);
                            fillCloudServices(selectedVN, cascade);
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
        DefaultLoader.getUIHelper().packAndCenterJDialog(form);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        StorageAccount newStorageAccount = form.getStorageAccount();
                        if (newStorageAccount != null) {
                            model.setStorageAccount(newStorageAccount);
                            fillStorage(selectedCS);
                        }
                    }
                });
            }
        });

        form.setVisible(true);
    }

    private static boolean isDeploymentEmpty(CloudService cloudService, String deploymentSlot) {
        if (deploymentSlot.equals(PRODUCTION)) {
            return cloudService.getProductionDeployment().getName().isEmpty();
        } else {
            return cloudService.getStagingDeployment().getName().isEmpty();
        }

    }

    private static boolean areSameRegion(CloudService cloudService, VirtualNetwork virtualNetwork) {
        return (!virtualNetwork.getLocation().isEmpty() &&
                virtualNetwork.getLocation().equals(cloudService.getLocation())) ||
                (!virtualNetwork.getAffinityGroup().isEmpty() &&
                        virtualNetwork.getAffinityGroup().equals(cloudService.getAffinityGroup()));
    }

    private static boolean areSameNetwork(CloudService cloudService, VirtualNetwork virtualNetwork) {
        return virtualNetwork.getName().equals(cloudService.getProductionDeployment().getVirtualNetwork());
    }

    private void validateNext() {
        model.getCurrentNavigationState().NEXT.setEnabled(storageComboBox.getSelectedItem() instanceof StorageAccount &&
                (!subnetComboBox.isEnabled() || subnetComboBox.getSelectedItem() instanceof String));
    }
}