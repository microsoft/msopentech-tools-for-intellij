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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardModel;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.intellij.ui.components.AzureWizardStep;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.util.AppCmpntParam;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

public class AzureWizardModel extends WizardModel {
    private SelectLocationStep selectLocationStep;
    private PublishSettingsStep publishSettingsStep;
    private KeyFeaturesStep keyFeaturesStep;
    private Project myProject;
    private WindowsAzureProjectManager waProjMgr;

    public AzureWizardModel(final Project project, WindowsAzureProjectManager waProjMgr) {
        super(AzureBundle.message("wizPageTitle"));
        myProject = project;
        this.waProjMgr = waProjMgr;
        selectLocationStep = new SelectLocationStep(this.getTitle(), this);
        publishSettingsStep = new PublishSettingsStep(this.getTitle(), this);
        keyFeaturesStep = new KeyFeaturesStep(this.getTitle());
        add(selectLocationStep);
        add(publishSettingsStep);
        add(keyFeaturesStep);
    }

    public Map<String, String> getDeployPageValues() {
        return publishSettingsStep.getJdkServerPanel().getDeployPageValues();
    }

    public Map<String, Boolean> getKeyFeaturesValues() {
        return keyFeaturesStep.getValues();
    }

    public String getProjectName() {
        return selectLocationStep.getProjectName();
    }

    public String getProjectLocation() {
        return selectLocationStep.getProjectLocation();
    }

    public boolean isUseDefaultLocation() {
        return selectLocationStep.isUseDefaultLocation();
    }

    public ArrayList<String> getAppsAsNames() {
        return publishSettingsStep.getJdkServerPanel().getApplicationsTab().getAppsAsNames();
    }

    public boolean isLicenseAccepted() {
        return publishSettingsStep.getJdkServerPanel().createAccLicenseAggDlg();
    }

    /**
     * @return applist
     */
    public ArrayList<AppCmpntParam> getAppsList() {
        return publishSettingsStep.getJdkServerPanel().getApplicationsTab().getAppsList();
    }
    /**
     * Method returns access key from storage registry
     * according to account name selected in combo box.
     * @param combo
     * @return
     */
    public static String getAccessKey(JComboBox combo) {
        String key = "";
        // get access key.
        int strgAccIndex = combo.getSelectedIndex();
        if (strgAccIndex > 0 && !combo.getSelectedItem().toString().isEmpty()) {
            key = StorageAccountRegistry.getStrgList().get(strgAccIndex - 1).getStrgKey();
        }
        return key;
    }

    public Project getMyProject() {
        return myProject;
    }

    public WindowsAzureProjectManager getWaProjMgr() {
        return waProjMgr;
    }

    public WindowsAzureRole getWaRole() {
        try {
            return waProjMgr.getRoles().get(0);
        } catch (WindowsAzureInvalidProjectOperationException e) {

            return null;
        }
    }

    public ValidationInfo doValidate() {
        return ((AzureWizardStep) getCurrentStep()).doValidate();
    }
}