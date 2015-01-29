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
package com.microsoftopentechnologies.intellij.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.interopbridges.tools.windowsazure.OSFamilyType;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoftopentechnologies.azurecommons.propertypage.Azure;
import com.microsoftopentechnologies.intellij.ui.AzureAbstractPanel;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AzureModulePanel implements AzureAbstractPanel {
    private JPanel contentPane;
    private JTextField txtServiceName;
    private JComboBox comboType;
    private JComboBox targetOSComboType;

    private Module myModule;
    private WindowsAzureProjectManager waProjManager;
    private static String[] arrType = new String[]{message("proPageBFEmul"), message("proPageBFCloud")};

    public AzureModulePanel(Module module) {
        this.myModule = module;
        init();
    }

    private void init() {
        loadProject();
        try {
            txtServiceName.setText(waProjManager.getServiceName());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("proPageErrTitle"), message("proPageErrMsgBox1") + message("proPageErrMsgBox2"), e);
        }
        comboType.setModel(new DefaultComboBoxModel(arrType));
        List<String> osNames = new ArrayList<String>();
        for (OSFamilyType osType : OSFamilyType.values()) {
            osNames.add(osType.getName());
        }
        targetOSComboType.setModel(new DefaultComboBoxModel(osNames.toArray(new String[osNames.size()])));
        WindowsAzurePackageType type;
        try {
            type = waProjManager.getPackageType();
            comboType.setSelectedItem(type.equals(WindowsAzurePackageType.LOCAL) ? arrType[0] : arrType[1]);
            //Set current value for target OS
            targetOSComboType.setSelectedItem(waProjManager.getOSFamily().getName());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("proPageErrTitle"), message("proPageErrMsgBox1") + message("proPageErrMsgBox2"), e);
        }
    }

    /**
     * This method loads the projects available in workspace.
     * selProject variable will contain value of current selected project.
     */
    private void loadProject() {
        try {
            waProjManager = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(myModule)));
        } catch (Exception e) {
            PluginUtil.displayErrorDialog( message("remAccSyntaxErr"), message("proPageErrMsgBox1") + message("proPageErrMsgBox2"));
            log(message("remAccErProjLoad"), e);
        }
    }

    @Override
    public JComponent getPanel() {
        return contentPane;
    }

    @Override
    public String getDisplayName() {
        return message("cmhLblWinAz");
    }

    @Override
    public boolean doOKAction() {
        try {
            loadProject();
            waProjManager = Azure.performOK(waProjManager, txtServiceName.getText(), (String) comboType.getSelectedItem(),
                    (String) targetOSComboType.getSelectedItem());
            waProjManager.save();
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(myModule)).refresh(true, true);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("proPageErrTitle"), message("proPageErrMsgBox1") + message("proPageErrMsgBox2"), e);
            return false;
        }
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }
}
