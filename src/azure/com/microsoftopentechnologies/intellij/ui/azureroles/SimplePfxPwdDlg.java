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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoftopentechnologies.azurecommons.wacommonutil.CerPfxUtil;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class SimplePfxPwdDlg extends DialogWrapper {
    private JPanel contentPane;
    private JPasswordField txtPwd;

    private String pfxPath;

    public SimplePfxPwdDlg(String path) {
        super(true);
        this.pfxPath = path;
        init();
    }

    protected void init() {
        setTitle(message("certPwd"));
        super.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        if (CerPfxUtil.validatePfxPwd(pfxPath, new String(txtPwd.getPassword()).trim())) {
            super.doOKAction();
        } else {
            PluginUtil.displayErrorDialog(message("error"), message("invalidPfxPwdMsg"));
        }
    }

    protected ValidationInfo doValidate() {
        if (new String(txtPwd.getPassword()).trim().isEmpty()) {
            return new ValidationInfo("", txtPwd);
        }
        return null;
    }

    public String getPwd() {
        return new String(txtPwd.getPassword());
    }
}
