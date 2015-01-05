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

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.wacommonutil.CerPfxUtil;
import com.microsoftopentechnologies.azurecommons.wacommonutil.Utils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.wacommon.commoncontrols.NewCertificateDialogData;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class NewCertificateDialog extends DialogWrapper {
    private JPanel contentPane;
    private JPasswordField txtPwd;
    private JPasswordField txtConfirmPwd;
    private JTextField txtCNName;
    private TextFieldWithBrowseButton txtCertFile;
    private TextFieldWithBrowseButton txtPFXFile;

    private NewCertificateDialogData newCertificateDialogHolder;
    private String jdkPath;
    private Project myProject;

    public NewCertificateDialog(NewCertificateDialogData data, String jdkPath, Project project) {
        super(true);
        setTitle(message("newCertDlgCertTxt"));
        this.newCertificateDialogHolder = data;
        this.jdkPath = jdkPath;
        this.myProject = project;
        init();
    }

    private ActionListener getFileSaverListener(final TextFieldWithBrowseButton field, final TextFieldWithBrowseButton fieldToUpdate,
                                                final String suffixToReplace, final String suffix) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(
                        new FileSaverDescriptor(message("newCertDlgBrwFldr"), "", suffixToReplace), field);
                final VirtualFile baseDir = myProject.getBaseDir();
                final VirtualFileWrapper save = dialog.save(baseDir, "");
                if (save != null) {
                    field.setText(FileUtil.toSystemDependentName(save.getFile().getAbsolutePath()));
                    if (fieldToUpdate.getText().isEmpty()) {
                        fieldToUpdate.setText(Utils.replaceLastSubString(field.getText(), suffixToReplace, suffix));
                    }
                }
            }
        };
    }

    @Override
    protected void init() {
        txtCNName.setText(Utils.getDefaultCNName());
        txtCertFile.addActionListener(getFileSaverListener(txtCertFile, txtPFXFile, "cer", "pfx"));
        txtPFXFile.addActionListener(getFileSaverListener(txtPFXFile, txtCertFile, "pfx", "cer"));
        super.init();
    }

    @Override
    protected void doOKAction() {
        try {
            String alias = message("newCertDlgAlias");
            // fix for #2663
            if (jdkPath == null || jdkPath.isEmpty()) {
                jdkPath = WAEclipseHelperMethods.jdkDefaultDirectory(null);
            }
            CerPfxUtil.createCertificate(txtCertFile.getText(), txtPFXFile.getText(), alias, String.valueOf(txtPwd.getPassword()), txtCNName.getText(), jdkPath);

            //At this point certificates are created , populate the values for caller
            if(newCertificateDialogHolder != null ) {
                newCertificateDialogHolder.setCerFilePath(txtCertFile.getText());
                newCertificateDialogHolder.setPfxFilePath(txtPFXFile.getText());
                newCertificateDialogHolder.setPassword(txtPFXFile.getText());
                newCertificateDialogHolder.setCnName(txtCNName.getText());
            }
            super.doOKAction();
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("newCertDlgCrtErTtl"), message("newCerDlgCrtCerEr"), e);
        }
    }

    @Nullable
    protected ValidationInfo doValidate() {
        if (txtPwd.getPassword().length == 0) {
            return new ValidationInfo(message("newCertDlgPwNul"), txtPwd);
        } else if (txtConfirmPwd.getPassword().length == 0) {
            return new ValidationInfo(message("newCertDlgPwNul"), txtPwd);
        }
        Pattern pattern = Pattern.compile("^\\S+$");
        Matcher match = pattern.matcher(String.valueOf(txtPwd.getPassword()));
        if (!match.find()) {
            return new ValidationInfo(message("newCertDlgPwNtCor"), txtPwd);
        }
        match = pattern.matcher(String.valueOf(txtConfirmPwd.getPassword()));
        if (!match.find()) {
            return new ValidationInfo(message("newCertDlgPwNtCor"), txtPwd);
        }
        if (!Arrays.equals(txtPwd.getPassword(), txtConfirmPwd.getPassword())) {
            return new ValidationInfo(message("newCerDlgPwNtMsg"));
        }
        if (txtCNName.getText().isEmpty()) {
            return new ValidationInfo(message("newCertDlgCNNull"), txtCNName);
        }
        if (txtCertFile.getText().isEmpty()) {
            return new ValidationInfo(message("newCertDlgCerNul"), txtCertFile);
        }
        if (txtPFXFile.getText().isEmpty()) {
            return new ValidationInfo(message("newCertDlgPFXNull"), txtPFXFile);
        }
        String certFilePath = txtCertFile.getText();
        String pfxFilePath = txtPFXFile.getText();
        if (certFilePath.lastIndexOf(File.separator) == -1 || pfxFilePath.lastIndexOf(File.separator) == -1) {
            return new ValidationInfo(message("newCerDlgInvldPth"));
        }
        if ((!certFilePath.endsWith(".cer")) || (!pfxFilePath.endsWith(".pfx"))) {
            return new ValidationInfo(message("newCerDlgInvdFlExt"));
        }
        String certFolder = certFilePath.substring(0, certFilePath.lastIndexOf(File.separator));
        String pfxFolder = pfxFilePath.substring(0, pfxFilePath.lastIndexOf(File.separator));
        File certFile = new File(certFolder);
        File pfxFile = new File(pfxFolder);
        if (!(certFile.exists() && pfxFile.exists())) {
            return new ValidationInfo(message("newCerDlgInvldPth"));
        }
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("newCertDlgCertTxt"), message("newCertDlgCertMsg"));
    }
}
