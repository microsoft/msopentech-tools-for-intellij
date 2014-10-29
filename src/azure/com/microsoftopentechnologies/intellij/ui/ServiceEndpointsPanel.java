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

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.wacommonutil.PreferenceSetUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class ServiceEndpointsPanel implements AzureAbstractPanel {
    private static final String DISPLAY_NAME = "Service Endpoints";
    private JPanel contentPane;

    private JComboBox prefNameCmb;
    private JButton editBtn;
    private JTextField txtPortal;
    private JTextField txtMangmnt;
    private JTextField txtBlobUrl;
    private JTextField txtPubSet;

    private String valOkToLeave = "";

    public ServiceEndpointsPanel() {
        if (!AzurePlugin.IS_ANDROID_STUDIO && AzurePlugin.IS_WINDOWS) {
            init();
        }
    }

    protected void init() {
        prefNameCmb.addItemListener(createPrefNameCmbListener());
        setToDefaultName();
        populateValues();
        editBtn.addActionListener(createEditBtnListener());
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    private ItemListener createPrefNameCmbListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                populateValues();
            }
        };
    }

    private ActionListener createEditBtnListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                JdkSrvConfig.custLinkListener(
//                        Messages.edtPrefTtl,
//                        Messages.prefFileMsg,
//                        false,
//                        getShell(),
//                        null,
//                        preferenceFile);
            }
        };
    }

    /**
     * Method sets extracted <preferenceset> names to
     * active set combo box. By default, it is set to
     * the default setting of the parent <preferencesets> element.
     * But if user is visiting service endpoint page
     * after okToLeave then value modified by user is populated.
     */
    private void setToDefaultName() {
        try {
            prefNameCmb.setModel(new DefaultComboBoxModel(PreferenceSetUtil.getPrefSetNameArr(AzurePlugin.prefFilePath)));
            if (!valOkToLeave.isEmpty()) {
                prefNameCmb.setSelectedItem(valOkToLeave);
            } else {
                prefNameCmb.setSelectedItem(PreferenceSetUtil.getSelectedPreferenceSetName(AzurePlugin.prefFilePath));
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("errTtl"), message("getPrefErMsg"));
        }
    }

    /**
     * Blob, Management, Portal
     * and Publish Settings
     * values from preferencesets.xml
     * will be populated according to active set value.
     */
    private void populateValues() {
        String nameInCombo = (String) prefNameCmb.getSelectedItem();
        try {
            txtPortal.setText(PreferenceSetUtil.getSelectedPortalURL(nameInCombo, AzurePlugin.prefFilePath));
            txtMangmnt.setText(PreferenceSetUtil.getManagementURL(nameInCombo, AzurePlugin.prefFilePath));
            txtBlobUrl.setText(PreferenceSetUtil.getBlobServiceURL(nameInCombo, AzurePlugin.prefFilePath));
            txtPubSet.setText(PreferenceSetUtil.getSelectedPublishSettingsURL(nameInCombo, AzurePlugin.prefFilePath));
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("errTtl"), message("getPrefErMsg"));
        }
    }
}
