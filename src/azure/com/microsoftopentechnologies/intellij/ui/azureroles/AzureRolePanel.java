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
package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureNamedCache;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AzureRolePanel extends BaseConfigurable {
    private String[] arrVMSize = {"A9", "A8", "A7", "A6", "A5", "ExtraLarge", "Large", "Medium", "Small", "ExtraSmall"};

    private JPanel contentPane;
    private JTextField txtRoleName;
    private JComboBox comboVMSize;
    private JTextField txtNoOfInstances;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole windowsAzureRole;
    private boolean isNew;

    public AzureRolePanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole windowsAzureRole, boolean isNew) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.windowsAzureRole = windowsAzureRole;
        setModified(isNew);
        this.isNew = isNew;
        init();
    }

    private void init() {
        txtRoleName.setText(windowsAzureRole.getName());
        comboVMSize.setModel(new DefaultComboBoxModel(arrVMSize));
        comboVMSize.setSelectedItem(arrVMSize[getVMSizeIndex()]);
        comboVMSize.addItemListener(createComboVMSizeListener());
        txtRoleName.getDocument().addDocumentListener(createModifyListener());
        try {
            txtNoOfInstances.setText(windowsAzureRole.getInstances());
            txtNoOfInstances.getDocument().addDocumentListener(createModifyListener());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
    }

    private ItemListener createComboVMSizeListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                handleSmallVMCacheConf();
                setModified(true);
//                // Set VM Size in role
//                try {
//                    windowsAzureRole.setVMSize((String) comboVMSize.getSelectedItem());
//                } catch (WindowsAzureInvalidProjectOperationException ex) {
//                    PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
//                }
            }
        };
    }

    private DocumentListener createModifyListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
            }
        };
    }

    private int getVMSizeIndex() {
        String vmSize = "";
        vmSize = windowsAzureRole.getVMSize();
        int index = 8;
        for (int i = 0; i < arrVMSize.length; i++) {
            if (vmSize.equalsIgnoreCase(arrVMSize[i])) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void handleSmallVMCacheConf() {
        try {
            if (message("txtExtraSmallVM").equals((String) comboVMSize.getSelectedItem()) && windowsAzureRole.getCacheMemoryPercent() > 0) {
                // If extra small VM and cache is enabled
                int choice = Messages.showYesNoDialog((Project) null, message("cacheConfMsg"), message("cacheConfTitle"), Messages.getWarningIcon());
                if (choice == Messages.YES) {
                    // Yes - Disable cache
                    windowsAzureRole.setCacheMemoryPercent(0);
                } else {
                    // No or if dialog is closed directly then reset VM size back to original
                    comboVMSize.setSelectedItem(arrVMSize[getVMSizeIndex()]);
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachGetErMsg"), e);
        }
    }

    /**
     * Method checks if number of instances are equal to 1
     * and caching is enabled as well as high availability
     * feature is on then ask input from user,
     * whether to turn off high availability feature
     * or he wants to edit instances.
     *
     * @param val
     * @return boolean
     */
    private boolean handleHighAvailabilityFeature(boolean val) {
        boolean isBackupSet = false;
        boolean okToProceed = val;
        try {
            /*
    		 * checks if number of instances are equal to 1
    		 * and caching is enabled
    		 */
            if (txtNoOfInstances.getText().trim().equalsIgnoreCase("1") && windowsAzureRole.getCacheMemoryPercent() > 0) {
    			/*
    			 * Check high availability feature of any of the cache is on
    			 */
                Map<String, WindowsAzureNamedCache> mapCache = windowsAzureRole.getNamedCaches();
                for (Iterator<WindowsAzureNamedCache> iterator = mapCache.values().iterator(); iterator.hasNext(); ) {
                    WindowsAzureNamedCache cache = (WindowsAzureNamedCache) iterator.next();
                    if (cache.getBackups()) {
                        isBackupSet = true;
                    }
                }
    			/*
    			 * High availability feature of any of the cache is on.
    			 */
                if (isBackupSet) {
                    int choice = Messages.showOkCancelDialog(message("highAvailMsg"), message("highAvailTtl"), Messages.getQuestionIcon());
                    /*
    				 * Set High availability feature to No.
    				 */
                    if (choice == Messages.OK) {
                        for (Iterator<WindowsAzureNamedCache> iterator = mapCache.values().iterator(); iterator.hasNext(); ) {
                            WindowsAzureNamedCache cache = iterator.next();
                            if (cache.getBackups()) {
                                cache.setBackups(false);
                            }
                        }
                        okToProceed = true;
                        waProjManager.save();
                    } else {
    					/*
    					 * Stay on Role properties page.
    					 */
                        okToProceed = false;
                        txtNoOfInstances.requestFocus();
                    }
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachGetErMsg"), e);
            okToProceed = false;
        }
        return okToProceed;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            windowsAzureRole.setVMSize((String) comboVMSize.getSelectedItem());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
        roleNameModifyListener();
        instancesModifyListener();
        boolean okToProceed = true;
        try {
            okToProceed = handleHighAvailabilityFeature(okToProceed);
            waProjManager.save();
            setModified(false);
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
    }

    /**
     * Modify listener for role name textbox.
     */
    private void roleNameModifyListener() throws ConfigurationException {
//        if (isValidinstances) {
//            setValid(true);
//        } else {
//            setValid(false);
//        }
        String roleName = txtRoleName.getText();
        try {
            boolean isValidRoleName;
            if (roleName.equalsIgnoreCase(windowsAzureRole.getName())) {
                isValidRoleName = true;
            } else {
                isValidRoleName = waProjManager.isAvailableRoleName(roleName);
            }
            /*
        	 * If text box is empty then do not show error
        	 * as user may be giving input.
        	 * Just disable OK button.
        	 */
            /*if (txtRoleName.getText().isEmpty()) {
                setValid(false);
            } else */
            if (isValidRoleName) {
                windowsAzureRole.setName(txtRoleName.getText().trim());
            } else {
                throw new ConfigurationException(message("dlgInvldRoleName2"), message("dlgInvldRoleName1"));
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
    }

    /**
     * Modify listener for number of instances textbox.
     * Validate number of instances field.
     */
    protected void instancesModifyListener() {
//        if (isValidRoleName) {
//            setValid(true);
//        } else  {
//            setValid(false);
//        }
        String noOfInstances = txtNoOfInstances.getText();
        /*
         * If text box is not empty
         * then only get integer value using casting.
         */
        if (!noOfInstances.isEmpty()) {
            try {
                int instances = Integer.parseInt(noOfInstances);
//                if (instances < 1) {
//                    isValidinstances = false;
//                } else {
//                    isValidinstances = true;
//                }
            } catch (NumberFormatException ex) {
//                isValidinstances = false;
            }
        }
        try {
        	/*
        	 * If text box is empty then do not show error
        	 * as user may be giving input.
        	 * Just disable OK button.
        	 */
         /*   if (noOfInstances.isEmpty()) {
                setValid(false);
            } else if (!isValidinstances) {
                setValid(false);
                PluginUtil.displayErrorDialog(message("dlgInvldInst1"), message("dlgInvldInst2"));
            } else {*/
                windowsAzureRole.setInstances(txtNoOfInstances.getText());
//            }
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
        }
    }


    @Override
    public void reset() {
        if (!isNew) {
            setModified(false);
        }
    }

    @Override
    public void disposeUIResources() {

    }

    @Override
    public String getDisplayName() {
        return message("cmhLblGeneral");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_role";
    }
}
