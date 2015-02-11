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

package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.interopbridges.tools.windowsazure.WindowsAzureEndpoint;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

/**
 * This Class provides the debug option to the user
 * to enable/disable project debugging.
 */
public class DebugConfigurationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JCheckBox emulatorCheckBox;
    private JCheckBox cloudCheckBox;
    private JTextField hostText;
    private TextFieldWithBrowseButton projText;
    private JLabel hostLabel;

    private Project project;
    private WindowsAzureRole debugRole;
    private WindowsAzureEndpoint debugEndpoint;
    private Map<String,String> paramMap;
    private List<String> configList = new ArrayList<String>();

    public DebugConfigurationDialog(Project project, WindowsAzureRole role, WindowsAzureEndpoint endPoint, Map<String,String> map) {
        super(true);
        this.project = project;
        this.debugRole = role;
        this.debugEndpoint = endPoint;
        this.paramMap = map;
        init();
    }

    protected void init() {
        setTitle(message("dbgTitle"));
        projText.addActionListener(createProjTextListener());
        cloudCheckBox.addItemListener(createCloudCheckBoxListener());
        super.init();
    }

    private ActionListener createProjTextListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectFromListDialog dialog = new SelectFromListDialog(project, ModuleManager.getInstance(project).getModules(), new SelectFromListDialog.ToStringAspect() {
                    @Override
                    public String getToStirng(Object obj) {
                        return ((Module) obj).getName();
                    }
                }, message("dbgProjSelTitle"), ListSelectionModel.SINGLE_SELECTION);
                dialog.show();
                if (dialog.getSelection() != null && dialog.getSelection().length > 0)
                    projText.setText(((Module) dialog.getSelection()[0]).getName());
            }
        };
    }

    private ItemListener createCloudCheckBoxListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                hostLabel.setEnabled(cloudCheckBox.isSelected());
                hostText.setEnabled(cloudCheckBox.isSelected());
            }
        };
    }

    /**
     * This method prepares and pass the required parameters for
     * creating a new debug launch configuration.
     */
    protected void createLaunchConfigParams() {
        if (emulatorCheckBox.isSelected()) {
            String configName = String.format("%s%s)", message("dlgDbgConfEmul"), debugRole.getName());
//            ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
            int iterate = 1;
            String tempConfigName = configName;
//            while (manager.isExistingLaunchConfigurationName(tempConfigName)) {
//                tempConfigName = String.format("%s (%s)", configName,
//                        String.valueOf(iterate));
//                iterate++;
//            }
            paramMap.put(message("dlgDbgEmuChkd") , "true");
            paramMap.put(message("dlgDbgEmuConf"), tempConfigName);
            paramMap.put(message("dlgDbgEmuProj"), projText.getText());
            paramMap.put(message("dlgDbgEmuHost"), WAEclipseHelperMethods.getHostName());
            paramMap.put(message("dlgDbgEmuPort"), debugEndpoint.getPrivatePort());
            configList.add(tempConfigName);
        }
        if (cloudCheckBox.isSelected()) {
            String configName = String.format("%s%s)", message("dlgDbgConfCloud"), debugRole.getName());
//            ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
            int iterate = 1;
            String tempConfigName = configName;
//            while (manager.isExistingLaunchConfigurationName(tempConfigName)) {
//                tempConfigName = String.format("%s (%s)", configName, String.valueOf(iterate));
//                iterate++;
//            }
            paramMap.put(message("dlgDbgCldChkd") , "true");
            paramMap.put(message("dlgDbgCldConf"), tempConfigName);
            paramMap.put(message("dlgDbgCldProj"), projText.getText());
            paramMap.put(message("dlgDbgCldHost"), hostText.getText());
            paramMap.put(message("dlgDbgCldPort"), debugEndpoint.getPort());
            configList.add(tempConfigName);
        }
    }

    @Override
    protected void doOKAction() {
        if (!projText.getText().isEmpty()) {
            Module module = ModuleManager.getInstance(project).findModuleByName(projText.getText());
            try {
                //check for project exists/open/JAVA nature
                if (module == null) {
                    PluginUtil.displayErrorDialog(message("dbgInvdProjTitle"), message("dbgInvdProjMsg"));
                    return;
                }
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("dbgProjErrTitle"), message("dbgProjErr"), e);
                return;
            }
        }
        try {
            //method which pass the required parameters for creating new
            //debug launch configuration.
            createLaunchConfigParams();
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("dlgDbgConfErrTtl"), message("dlgDbgConfErr"), e);
            return;
        }
        StringBuilder message = new StringBuilder(message("dlgConfDbgDlgMsg"));
        for (String str : configList) {
            message.append(String.format("\n-   %s", str));
        }
        Messages.showInfoMessage(message.toString(), message("dlgConfDbgTitle"));
        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected boolean postponeValidation() {
        return true;
    }

    @Override
    protected ValidationInfo doValidate() {
        if (projText.getText().isEmpty()) {
            return new ValidationInfo("Project cannot be empty", projText);
        }
        if (cloudCheckBox.isSelected() && hostText.getText().isEmpty()) {
            return new ValidationInfo("Empty host name", hostText);
        }
        if (!cloudCheckBox.isSelected() && !emulatorCheckBox.isSelected()) {
            return new ValidationInfo("Select debug configuration");
        }
        return null;
    }

    @Override
    public String getHelpId() {
        return "windows_azure_debug_config";
    }
}
