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

import com.microsoftopentechnologies.intellij.runnable.NewStorageAccountWithProgressWindow;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.windowsazure.management.models.LocationsListResponse;
import com.microsoft.windowsazure.management.storage.models.StorageAccountCreateParameters;
import com.microsoftopentechnologies.model.StorageService;
import com.microsoftopentechnologies.model.Subscription;
import com.microsoftopentechnologies.deploy.util.PublishData;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils.ElementWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class NewStorageAccountDialog extends DialogWrapper {
    private final static String STORAGE_ACCOUNT_NAME_PATTERN = "^[a-z0-9]+$";

    private JPanel contentPane;
    private JTextField storageAccountTxt;
    private JComboBox locationComb;
    private JTextField descriptionTxt;
    private JComboBox subscriptionCombo;

    private String defaultLocation;
    private String subscription;
    private StorageService storageService;

    private Project myProject;

    public NewStorageAccountDialog(String subscription, Project project) {
        super(true);
        setTitle(message("strgAcc"));
        this.subscription = subscription;
        this.myProject = project;
        init();
    }

    protected void init() {
        UIUtils.populateSubscriptionCombo(subscriptionCombo);
        /*
		 * If subscription name is there,
		 * dialog invoked from publish wizard,
		 * hence disable subscription combo.
		 */
        if (subscription != null) {
            subscriptionCombo.setEnabled(false);
            UIUtils.selectByText(subscriptionCombo, subscription);
        }
        subscriptionCombo.addItemListener(createSubscriptionComboListener());
        populateLocations();
        super.init();
    }

    private ItemListener createSubscriptionComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                populateLocations();
            }
        };
    }

    private void populateLocations() {
        List<LocationsListResponse.Location> items;
        ElementWrapper<PublishData> pd = (ElementWrapper<PublishData>) subscriptionCombo.getSelectedItem();
        if (pd != null) {
            Subscription sub = WizardCacheManager.findSubscriptionByName(pd.getKey());
            items = pd.getValue().getLocationsPerSubscription().get(sub.getId());
        } else {
            items = WizardCacheManager.getLocation();
        }
        locationComb.removeAllItems();

        for (LocationsListResponse.Location location : items) {
            locationComb.addItem(location.getName());
        }
        /*
         * default location will exist if the user has
		 * created a storage account before creating the hosted service
		 */
        if (defaultLocation != null) {
            locationComb.setSelectedItem(defaultLocation);
        }
    }

    @Override
    protected void doOKAction() {
        String storageAccountNameToCreate = storageAccountTxt.getText();
        String storageAccountLocation = (String) locationComb.getSelectedItem();

        final StorageAccountCreateParameters body = new StorageAccountCreateParameters();
        body.setName(storageAccountTxt.getText());
        body.setLabel(storageAccountTxt.getText());
        body.setLocation((String) locationComb.getSelectedItem());
        body.setDescription(descriptionTxt.getText());

        try {
            boolean isNameAvailable = WizardCacheManager.isStorageAccountNameAvailable(storageAccountNameToCreate);
            if (isNameAvailable) {
				/*
				 * case 1 : Invoked through publish wizard
				 * create mock and add account through publish process
				 */
                if (subscription != null) {
                    WizardCacheManager.createStorageServiceMock(storageAccountNameToCreate, storageAccountLocation, descriptionTxt.getText());
                } else {
					/*
                     * case 2 : Invoked through preference page
					 * Add account immediately.
					*/
                    PublishData pubData = UIUtils.changeCurrentSubAsPerCombo(subscriptionCombo);
                    PublishData publishData = WizardCacheManager.getCurrentPublishData();
                    Subscription curSub = publishData.getCurrentSubscription();
                    int maxStorageAccounts = curSub.getMaxStorageAccounts();

                    if (maxStorageAccounts > publishData.getStoragesPerSubscription().get(curSub.getId()).size()) {
                        NewStorageAccountWithProgressWindow task = new NewStorageAccountWithProgressWindow(pubData, body);
                        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, "Progress Information", true, myProject);
                        storageService = task.getStorageService();
                    } else {
                        PluginUtil.displayErrorDialog(message("storageAccountsLimitTitle"), message("storageAccountsLimitErr"));
                        return;
                    }
                }
//                valid = true;
                super.doOKAction();
            } else {
                PluginUtil.displayErrorDialog(message("dnsCnf"), message("storageAccountConflictError"));
                storageAccountTxt.requestFocus();
                storageAccountTxt.selectAll();
            }
        } catch (final Exception e1) {
            PluginUtil.displayErrorDialogAndLog(message("error"), e1.getMessage(), e1);
        }
    }

    @Override
    protected boolean postponeValidation() {
        return false;
    }

    @Nullable
    protected ValidationInfo doValidate() {
        String host = storageAccountTxt.getText();
        String location = (String) locationComb.getSelectedItem();

        boolean legalName = validateStorageAccountName(host);
        if (host == null || host.isEmpty()) {
            return new ValidationInfo(message("storageAccountIsNullError"), storageAccountTxt);
        }
        if (!legalName) {
            return new ValidationInfo(message("wrongStorageName"), storageAccountTxt);
        }
        if (location == null || location.isEmpty()) {
            return new ValidationInfo(message("hostedLocNotSelectedError"), locationComb);
        }
//        setMessage(message("storageCreateNew"));
        return null;
    }

    private boolean validateStorageAccountName(String host) {
        if (host.length() < 3 || host.length() > 24) {
            return false;
        }
        return host.matches(STORAGE_ACCOUNT_NAME_PATTERN);
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public String getStorageAccountName() {
        return storageAccountTxt.getText();
    }

    public StorageService getStorageService() {
        return storageService;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("storageNew"), message("storageCreateNew"));
    }
}
