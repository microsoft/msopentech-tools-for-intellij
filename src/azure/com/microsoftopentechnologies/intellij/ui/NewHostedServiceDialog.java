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

import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.windowsazure.management.models.LocationsListResponse;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class NewHostedServiceDialog extends DialogWrapper {
    private final static String HOSTED_SERVICE_NAME_PATTERN = "^[a-z0-9]+$";

    private JPanel contentPane;
    private JTextField hostedServiceTxt;
    private JComboBox locationComb;
    private JTextField descriptionTxt;

    private String defaultLocation;

    public NewHostedServiceDialog() {
        super(true);
        setTitle(message("cldSrv"));
        init();
    }

    protected void init() {
        populateLocations();
        super.init();
    }

    private void populateLocations() {
        List<LocationsListResponse.Location> items = WizardCacheManager.getLocation();
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
        String hostedServiceNameToCreate = hostedServiceTxt.getText();
        String hostedServiceLocation = (String) locationComb.getSelectedItem();
        boolean isNameAvailable;
        try {
            isNameAvailable = WizardCacheManager.isHostedServiceNameAvailable(hostedServiceNameToCreate);
            if (isNameAvailable) {
                WizardCacheManager.createHostedServiceMock(hostedServiceNameToCreate, hostedServiceLocation, descriptionTxt.getText());
//                valid = true;
                super.doOKAction();
            } else {
                PluginUtil.displayErrorDialog(message("dnsCnf"), message("hostedServiceConflictError"));
                hostedServiceTxt.requestFocusInWindow();
                hostedServiceTxt.selectAll();
            }
        } catch (final Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("error"), ex.getMessage(), ex);
        }
    }

    @Override
    protected boolean postponeValidation() {
        return false;
    }

    @Nullable
    protected ValidationInfo doValidate() {

        String host = hostedServiceTxt.getText();
        String location = (String) locationComb.getSelectedItem();

        boolean legalName = validateHostedServiceName(host);
        if (host == null || host.isEmpty()) {
            return new ValidationInfo(message("hostedIsNullError"), hostedServiceTxt);
        }
        if (!legalName) {
            return new ValidationInfo("<html>Hosted Service name may consist only of<br>numbers and lower case letters, and be 3-24 characters long.</html>",
                    hostedServiceTxt);
        }
        if (location == null || location.isEmpty()) {
            return new ValidationInfo(message("hostedLocNotSelectedError"), locationComb);
        }
//        setMessage(message("hostedCreateNew"));
        return null;
    }

    private boolean validateHostedServiceName(String host) {
        if (host.length() < 3 || host.length() > 24) {
            return false;
        }
        return host.matches(HOSTED_SERVICE_NAME_PATTERN);
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public String getHostedServiceName() {
        return hostedServiceTxt.getText();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("hostedNew"), message("hostedCreateNew"));
    }
}
