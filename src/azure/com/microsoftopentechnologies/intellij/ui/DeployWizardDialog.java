/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.ui;

import com.microsoftopentechnologies.intellij.runnable.AccountActionRunnable;
import com.microsoftopentechnologies.intellij.runnable.CacheAccountWithProgressBar;
import com.microsoftopentechnologies.intellij.runnable.LoadAccountWithProgressBar;
import com.microsoftopentechnologies.intellij.util.MethodUtils;
import com.microsoftopentechnologies.intellij.ui.components.WindowsAzurePage;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.interopbridges.tools.windowsazure.OSFamilyType;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse;
import com.microsoftopentechnologies.deploy.wizard.ConfigurationEventArgs;
import com.microsoftopentechnologies.model.KeyName;
import com.microsoftopentechnologies.model.StorageService;
import com.microsoftopentechnologies.model.StorageServices;
import com.microsoftopentechnologies.model.Subscription;
import com.microsoftopentechnologies.deploy.util.PublishData;
import com.microsoftopentechnologies.deploy.util.WizardCache;
import com.microsoftopentechnologies.intellij.AzureSettings;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils.ElementWrapper;
import org.jetbrains.annotations.Nullable;

public class DeployWizardDialog extends WindowsAzurePage {
    private JPanel contentPane;
    private JButton importButton;
    private JComboBox subscriptionCombo;
    private JXHyperlink subLink;
    private JComboBox storageAccountCmb;
    private JButton newStorageAccountBtn;
    private JComboBox hostedServiceCombo;
    private JButton newHostedServiceBtn;
    private JComboBox targetOS;
    private JComboBox deployStateCmb;
    private JCheckBox unpublishChBox;
    private JTextField userName;
    private JPasswordField userPassword;
    private JPasswordField confirmPassword;
    private JCheckBox conToDplyChkBtn;
    private JLabel userPasswordLbl;
    private JLabel confirmPasswordLbl;
    private JXHyperlink encLink;

    private final Module myModule;
    private PublishData publishData;
    private HostedService currentHostedService;
    private StorageService currentStorageAccount;
    private WindowsAzurePackageType deployMode = WindowsAzurePackageType.CLOUD;
    private String defaultLocation;
    public ArrayList<String> newServices = new ArrayList<String>();
    private String deployFileName;
    private String deployConfigFileName;
    private WindowsAzureProjectManager waProjManager;

    public DeployWizardDialog(Module module) {
        super(module.getProject());
        this.myModule = module;
        loadProject();
        init();
    }

    @Override
    protected void init() {
        super.init();
        myOKAction.putValue(Action.NAME, "Publish");
        importButton.addActionListener(createImportSubscriptionAction());
        subscriptionCombo.addItemListener(createSubscriptionComboListener());
        subLink.setAction(createSubLinkAction());
        UIUtils.populateSubscriptionCombo(subscriptionCombo);
        AzureSettings azureSettings = AzureSettings.getSafeInstance(myModule.getProject());
        if (!azureSettings.isSubscriptionLoaded()) {
            doLoadPreferences();
            // reload information if its new session.
            AzureSettings.getSafeInstance(myModule.getProject()).loadStorage();
            MethodUtils.prepareListFromPublishData(myModule.getProject());
            azureSettings.setSubscriptionLoaded(true);
            UIUtils.populateSubscriptionCombo(subscriptionCombo);
            if ((subscriptionCombo.getSelectedItem() != null)) {
                loadDefaultWizardValues();
                unpublishChBox.setSelected(true);
            }
        }

        populateStorageAccounts();
        storageAccountCmb.addItemListener(createStorageAccountListener());
        newStorageAccountBtn.addActionListener(createNewStorageAccountListener());

        hostedServiceCombo.addItemListener(createHostedServiceComboListener());
        populateHostedServices();
        newHostedServiceBtn.addActionListener(createNewHostedServiceListener());

        populateTargetOs();

        deployStateCmb.setModel(new DefaultComboBoxModel(new String[]{message("deplStaging"), message("deplProd")}));
        deployStateCmb.addItemListener(createDeployStateCmbListener());

        userName.getDocument().addDocumentListener(createUserNameListener());
        encLink.setAction(createEncLinkAction());

        boolean isSubPresent = subscriptionCombo.getSelectedItem() != null;
        setComponentState(isSubPresent);
        if (isSubPresent) {
            // load cached subscription, cloud service & storage account
            loadDefaultWizardValues();
        }
        loadDefaultRDPValues();
    }

    private DocumentListener createUserNameListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setEnableRemAccess(e.getDocument().getLength() > 0);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setEnableRemAccess(e.getDocument().getLength() > 0);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setEnableRemAccess(e.getDocument().getLength() > 0);
            }
        };
    }

    private ActionListener createNewStorageAccountListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ElementWrapper<PublishData> subscription = (ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem();
                if (subscription != null) {
                    PublishData publishData = ((ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem()).getValue();
                    int maxStorageAccounts = publishData.getCurrentSubscription().getMaxStorageAccounts();

                    String currentSubscriptionId = publishData.getCurrentSubscription().getId();
                    if (maxStorageAccounts > publishData.getStoragesPerSubscription().get(currentSubscriptionId).size()) {
                        NewStorageAccountDialog storageAccountDialog = new NewStorageAccountDialog(subscription.getKey(), myModule.getProject());
                        if (defaultLocation != null) { // user has created a hosted service before a storage account
                            storageAccountDialog.setDefaultLocation(defaultLocation);
                        }
                        storageAccountDialog.show();
                        if (storageAccountDialog.isOK()) {
                            populateStorageAccounts();
                            UIUtils.selectByText(storageAccountCmb, storageAccountDialog.getStorageAccountName());
                            defaultLocation = WizardCacheManager.getStorageAccountFromCurrentPublishData(storageAccountDialog.getStorageAccountName()).getStorageAccountProperties().getLocation();
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("storageAccountsLimitTitle"), message("storageAccountsLimitErr"));
                    }
                }
            }
        };
    }

    private Action createSubLinkAction() {
        return new AbstractAction(message("linkLblSub")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DefaultDialogWrapper subscriptionsDialog = new DefaultDialogWrapper(myModule.getProject(), new SubscriptionsPanel(myModule.getProject()));
                subscriptionsDialog.show();

                /*
				 * Update data in every case.
				 * No need to check which button (OK/Cancel)
				 * has been pressed as change is permanent
				 * even though user presses cancel
				 * according to functionality.
				 */
//                doLoadPreferences();
                UIUtils.populateSubscriptionCombo(subscriptionCombo);
                // update cache of publish data object
                if (subscriptionCombo.getSelectedItem() != null) {
                    publishData = ((ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem()).getValue();
                }
                // enable and disable components.
                setComponentState((subscriptionCombo.getSelectedItem() != null));
            }
        };
    }

    private Action createEncLinkAction() {
        return new AbstractAction(message("linkLblEnc")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open remote access dialog
                DefaultDialogWrapper remoteAccess = new DefaultDialogWrapper(myModule.getProject(),
                        new WARemoteAccessPanel(myModule, true, userName.getText(), String.valueOf(userPassword.getPassword()),
                                String.valueOf(confirmPassword.getPassword())));
                remoteAccess.show();
                if (remoteAccess.isOK()) {
                    loadDefaultRDPValues();
                /*
			     * To handle the case, if you typed
			     * password on Publish wizard --> Encryption link
			     * Remote access --> OK --> Toggle password text boxes
			     */
//                    isPwdChanged = false;
                }
            }
        };
    }

    protected void doOKAction() {
        handlePageComplete();
        super.doOKAction();
    }

    /**
     * Initialize {@link WindowsAzureProjectManager} object
     * according to selected project.
     */
    private void loadProject() {
        try {
            String modulePath = PluginUtil.getModulePath(myModule);
            File projectDir = new File(modulePath);
            waProjManager = WindowsAzureProjectManager.load(projectDir);
        } catch (Exception e) {
            log(message("projLoadEr"), e);
        }
    }

    private void doLoadPreferences() {
        LoadAccountWithProgressBar task = new LoadAccountWithProgressBar(myModule.getProject());
        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, "Loading Account Settings...", true, myModule.getProject());
    }

    private ActionListener createNewHostedServiceListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtils.ElementWrapper<PublishData> subscriptionItem = (UIUtils.ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem();
                if (subscriptionItem != null) {
                    PublishData publishData = subscriptionItem.getValue();
                    int maxHostedServices = publishData.getCurrentSubscription().getMaxHostedServices();
                    String currentSubscriptionId = publishData.getCurrentSubscription().getId();
                    if (maxHostedServices > publishData.getServicesPerSubscription().get(currentSubscriptionId).size()) {

                        NewHostedServiceDialog hostedServiceDialog = new NewHostedServiceDialog();
                        if (defaultLocation != null) { // user has created a storage account before creating the hosted service
                            hostedServiceDialog.setDefaultLocation(defaultLocation);
                        }
                        hostedServiceDialog.show();
                        if (hostedServiceDialog.isOK()) {
                            populateHostedServices();
                            newServices.add(hostedServiceDialog.getHostedServiceName());
                            UIUtils.selectByText(hostedServiceCombo, hostedServiceDialog.getHostedServiceName());
                            defaultLocation = WizardCacheManager.getHostedServiceFromCurrentPublishData(hostedServiceDialog.getHostedServiceName()).
                                    getProperties().getLocation();
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("hostServLimitTitle"), message("hostServLimitErr"));
                    }
                }
            }
        };
    }

    private ItemListener createStorageAccountListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (storageAccountCmb.getSelectedItem() != null) {
                    currentStorageAccount = ((ElementWrapper<StorageService>) storageAccountCmb.getSelectedItem()).getValue();
                }
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private ItemListener createHostedServiceComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ElementWrapper<HostedService> selectedItem = (ElementWrapper<HostedService>) hostedServiceCombo.getSelectedItem();
                    currentHostedService = selectedItem == null ? null : selectedItem.getValue();
//                setPageComplete(validatePageComplete());
                }
            }
        };
    }

    private void populateTargetOs() {
        List<String> osNames = new ArrayList<String>();
        for (OSFamilyType osType : OSFamilyType.values()) {
            osNames.add(osType.getName());
        }
        targetOS.setModel(new DefaultComboBoxModel(osNames.toArray(new String[osNames.size()])));
    }

    private ItemListener createSubscriptionComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                publishData = UIUtils.changeCurrentSubAsPerCombo((JComboBox) e.getSource());
                if (storageAccountCmb != null && publishData != null) {
                    populateStorageAccounts();
                    populateHostedServices();
                    setComponentState((subscriptionCombo.getSelectedItem() != null));
                }
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private ActionListener createImportSubscriptionAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ImportSubscriptionDialog importSubscriptionDialog = new ImportSubscriptionDialog();
                importSubscriptionDialog.show();
                if (importSubscriptionDialog.isOK()) {
                    importBtn(importSubscriptionDialog.getPublishSettingsPath());

                }
            }
        };
    }

    private ItemListener createDeployStateCmbListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String deployState = (String)((JComboBox) e.getSource()).getSelectedItem();
                if (deployState.equalsIgnoreCase(message("deplProd"))) {
                    unpublishChBox.setSelected(false);
                }
//                setPageComplete(validatePageComplete());
            }
        };
    }

    private void importBtn(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            File file = new File(fileName);
            PublishData publishDataToCache = null;
            if (file.getName().endsWith(message("publishSettExt"))) {
                publishDataToCache = handlePublishSettings(file);
            }/* else {
                publishDataToCache = handlePfx(file);
            }*/
            if (publishDataToCache == null) {
                return;
            }
            /*
			 * logic to set un-pubilsh check box to true
			 * when ever importing publish settings
			 * file for the first time.
			 */
            if (subscriptionCombo.getItemCount() == 0) {
                unpublishChBox.setSelected(true);
            }
            UIUtils.populateSubscriptionCombo(subscriptionCombo);
//
//            int selection = 0;
//            selection = findSelectionIndex(publishDataToCache);
//
//            subscriptionCombo.select(selection);
//            WizardCacheManager.setCurrentPublishData(publishDataToCache);
//
            setComponentState((subscriptionCombo.getSelectedItem() != null));
//            // Make centralized storage registry.
//            MethodUtils.prepareListFromPublishData();
        }
    }

    private PublishData handlePublishSettings(File file) {
        PublishData data = UIUtils.createPublishDataObj(file);
		/*
		 * If data is equal to null,
		 * then publish settings file already exists.
		 * So don't load information again.
		 */
        if (data != null) {
            AccountActionRunnable settings = new CacheAccountWithProgressBar(file, data, null);
            doLoad(file, settings);
        }
        return data;
    }

    private void doLoad(File file, AccountActionRunnable settings) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(settings, "Loading Account Settings...", true, myModule.getProject());
//        try {
//            getContainer().run(true, true, settings);
        AzureSettings.getSafeInstance(myModule.getProject()).savePublishDatas();
//        } catch (InvocationTargetException e) {
//            String message = null;
//            if (e.getMessage() == null) {
//                message = message("genericErrorWhileLoadingCred");
//            } else {
//                message = e.getMessage();
//            }
//            PluginUtil.displayErrorDialog(null, message("importDlgTitle"), String.format(message("importDlgMsg"), file.getName(), message));
//        } catch (InterruptedException e) {
//        }
    }

    /**
     * Method loads configured remote access values
     * on wizard page.
     */
    private void loadDefaultRDPValues() {
        try {
            // to update project manager object
            loadProject();
            String uname = waProjManager.getRemoteAccessUsername();
            if (uname != null && !uname.isEmpty()) {
                userName.setText(uname);
                try {
                    String pwd = waProjManager.getRemoteAccessEncryptedPassword();
					/*
					 * If its dummy password,
					 * then do not show it on UI
					 */
                    if (pwd.equals(message("remAccDummyPwd")) || pwd.isEmpty()) {
                        userPassword.setText("");
                        confirmPassword.setText("");
                    } else {
                        userPassword.setText(pwd);
                        confirmPassword.setText(pwd);
                    }
                    setEnableRemAccess(true);
                } catch (Exception e) {
                    userPassword.setText("");
                    confirmPassword.setText("");
                }
            } else {
                userName.setText("");
                setEnableRemAccess(false);
            }
        } catch (Exception e) {
            userName.setText("");
            setEnableRemAccess(false);
        }
    }

    private void loadDefaultWizardValues() {
        WizardCache cacheObj = AzureSettings.getSafeInstance(myModule.getProject()).loadWizardCache(myModule.getName());
        if (cacheObj != null
                && !cacheObj.getSubName().isEmpty()
                && !cacheObj.getServiceName().isEmpty()
                && !cacheObj.getStorageName().isEmpty()) {
            UIUtils.selectByText(subscriptionCombo, cacheObj.getSubName());
            publishData = UIUtils.changeCurrentSubAsPerCombo(subscriptionCombo);
            if (publishData != null) {
                populateStorageAccounts();
                populateHostedServices();
                setComponentState((subscriptionCombo.getSelectedItem() != null));
                UIUtils.selectByText(hostedServiceCombo, cacheObj.getServiceName());
                UIUtils.selectByText(storageAccountCmb, cacheObj.getStorageName());
            }
        }
    }

    /**
     * Enable or disable password fields.
     * @param status
     */
    private void setEnableRemAccess(boolean status) {
        userPassword.setEnabled(status);
        confirmPassword.setEnabled(status);
        userPasswordLbl.setEnabled(status);
        confirmPasswordLbl.setEnabled(status);
        conToDplyChkBtn.setEnabled(status);
        if (!status) {
            userPassword.setText("");
            confirmPassword.setText("");
            conToDplyChkBtn.setSelected(false);
        }
    }

    /**
     * Enable or disable components related to
     * publish settings.
     * @param enabled
     */
    private void setComponentState(boolean enabled) {
        subscriptionCombo.setEnabled(enabled);
        storageAccountCmb.setEnabled(enabled);
        newStorageAccountBtn.setEnabled(enabled);
        hostedServiceCombo.setEnabled(enabled);
        targetOS.setEnabled(enabled);
        if (!enabled) {
            hostedServiceCombo.removeAllItems();
            storageAccountCmb.removeAllItems();
        }
        deployStateCmb.setEnabled(enabled);
        newHostedServiceBtn.setEnabled(enabled);
        unpublishChBox.setEnabled(enabled);
    }

    protected void populateStorageAccounts() {
        if (publishData != null) {
            Object currentSelection = storageAccountCmb.getSelectedItem();
            Subscription currentSubscription = publishData.getCurrentSubscription();
            StorageServices storageServices = publishData.getStoragesPerSubscription().get(currentSubscription.getId());
            storageAccountCmb.removeAllItems();
            if (storageServices != null && !storageServices.isEmpty()) {
                for (StorageService storageService : storageServices) {
                    storageAccountCmb.addItem(new ElementWrapper<StorageService>(storageService.getServiceName(), storageService));
                }
            }
            if (currentSelection != null) {
                storageAccountCmb.setSelectedItem(currentSelection);
            }
        }
    }

    public void populateHostedServices() {
        if (publishData != null) {
            Object currentSelection = hostedServiceCombo.getSelectedItem();
            Subscription currentSubscription = publishData.getCurrentSubscription();
            java.util.List<HostedServiceListResponse.HostedService> hostedServices = publishData.getServicesPerSubscription().get(currentSubscription.getId());
            hostedServiceCombo.removeAllItems();
            if (hostedServices != null && !hostedServices.isEmpty()) {
                for (HostedServiceListResponse.HostedService hsd : hostedServices) {
                    hostedServiceCombo.addItem(new ElementWrapper<HostedServiceListResponse.HostedService>(hsd.getServiceName(), hsd));
                }
                if (currentSelection != null) {
                    hostedServiceCombo.setSelectedItem(currentSelection);
                }
            }
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("deplWizTitle"), "");
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 400);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (publishData == null) {
            return new ValidationInfo(message("deplFillSubsciptionId"), subscriptionCombo);
        }
        if (currentStorageAccount == null) {
            return new ValidationInfo(message("deplFillStorageAcc"), storageAccountCmb);
        }
        if (currentHostedService == null) {
            return new ValidationInfo(message("deplFillHostedServiceMsg"), hostedServiceCombo);
        }
		/*
		 * Validation for remote access settings.
		 */
        if (!userName.getText().isEmpty()) {
            char[] pwd = userPassword.getPassword();
            if (pwd == null || pwd.length == 0) {
                // password is empty
                return new ValidationInfo(message("rdpPasswordEmpty"), userPassword);
            } else {
                char[] confirm = confirmPassword.getPassword();
                if (confirm == null || confirm.length == 0) {
                    // confirm password is empty
                    return new ValidationInfo(message("rdpConfirmPasswordEmpty"), confirmPassword);
                } else {
                    if (!Arrays.equals(pwd, confirm)) {
                        // password and confirm password do not match.
                        return new ValidationInfo(message("rdpPasswordsDontMatch"), confirmPassword);
                    }
                }
            }
        }
        return null;
    }

    private void handlePageComplete() {
        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_MODE, deployMode));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.SUBSCRIPTION, publishData));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.CONFIG_HTTPS_LINK, waProjManager.getSSLInfoIfUnique() != null? "true":"false"));


        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.STORAGE_ACCOUNT,
                currentStorageAccount));
        // Always set key to primary
        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.STORAGE_ACCESS_KEY,
                KeyName.Primary.toString()));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_STATE,
                deployStateCmb.getSelectedItem()));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.UN_PUBLISH,
                unpublishChBox.isSelected()));

        deployFileName = constructDeployFilePath(message("cspckDefaultFileName"));

        deployConfigFileName = constructDeployFilePath(message("cscfgDefaultFileName"));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_FILE,
                deployFileName));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.DEPLOY_CONFIG_FILE,
                deployConfigFileName));

        fireConfigurationEvent(new ConfigurationEventArgs(this,
                ConfigurationEventArgs.HOSTED_SERVICE,
                currentHostedService));
    }

    private String constructDeployFilePath(String fileName) {
        String moduleLocation = PluginUtil.getModulePath(myModule);
        return moduleLocation + File.separator + message("deployDir") + File.separator + fileName;
    }

    /**
     * Method returns new services names, if created by user.
     * @return
     */
    public ArrayList<String> getNewServices() {
        return newServices;
    }

    public String getTargetOSName() {
        return (String) targetOS.getSelectedItem();
    }

    public String getRdpUname() {
        return userName.getText();
    }

    public String getRdpPwd() {
        return new String(userPassword.getPassword());
    }

    public boolean getConToDplyChkStatus() {
        return conToDplyChkBtn.isSelected();
    }
}

