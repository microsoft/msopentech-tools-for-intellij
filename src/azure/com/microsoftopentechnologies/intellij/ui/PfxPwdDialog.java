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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.interopbridges.tools.windowsazure.WindowsAzureCertificate;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class PfxPwdDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel pfxInputMsg;
    private TextFieldWithBrowseButton txtPfxPath;
    private JPasswordField passwordField;


    private WindowsAzureCertificate cert;

    public PfxPwdDialog(WindowsAzureCertificate cert) {
        super(true);
        setTitle(message("pfxInputTtl"));
        this.cert = cert;
        init();
    }

    @Override
    protected void init() {
        txtPfxPath.addActionListener(UIUtils.createFileChooserListener(txtPfxPath, null, FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        pfxInputMsg.setText(String.format(message("pfxInputMsg"), cert.getName()));
        super.init();
    }

    public String getPfxPath() {
        return txtPfxPath.getText();
    }

    public String getPwd() {
        return new String(passwordField.getPassword());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
