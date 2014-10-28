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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.interopbridges.tools.windowsazure.WindowsAzureCertificate;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.exception.AzureCommonsException;
import com.microsoftopentechnologies.intellij.ui.NewCertificateDialog;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.roleoperations.CertificateDialogUtilMethods;
import com.microsoftopentechnologies.wacommon.commoncontrols.NewCertificateDialogData;
import com.microsoftopentechnologies.wacommonutil.CerPfxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.security.cert.X509Certificate;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class CertificateDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JTextField txtThumb;

    private Module module;
    private Map<String, WindowsAzureCertificate> mapCert;
    private WindowsAzureRole waRole;
    public String certNameAdded = "";

    public CertificateDialog(Module module, Map<String, WindowsAzureCertificate> mapCert, WindowsAzureRole waRole) {
        super(true);
        this.module = module;
        this.mapCert = mapCert;
        this.waRole = waRole;
        init();
    }

    @Override
    protected void init() {
        createDocumentListener(txtName);
        createDocumentListener(txtThumb);
        myOKAction.setEnabled(false);
        super.init();
    }

    private void createDocumentListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisableOkBtn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisableOkBtn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisableOkBtn();
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("certAddTtl"), message("certMsg"));
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        final AbstractAction importAction = new AbstractAction(message("importBtn")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                importBtnListner();
            }
        };
        final AbstractAction newAction = new AbstractAction(message("newBtn")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                newBtnListener();
            }
        };
        return new Action[]{importAction, newAction};
    }

    /**
     * Method to remember which certificate got added recently.
     */
    public String getNewlyAddedCert() {
        return certNameAdded;
    }

    @Override
    protected void doOKAction() {
        boolean retVal;
        try {
            String name = txtName.getText().trim();
            String thumb = txtThumb.getText().trim();
            retVal = validateNameAndThumbprint(name, thumb);
            if (retVal) {
                waRole.addCertificate(name, thumb.toUpperCase());
                certNameAdded = name;
            }
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            retVal = false;
        }
        if (retVal) {
            super.doOKAction();
        }
    }

    private void enableDisableOkBtn() {
        if (txtThumb.getText().trim().isEmpty() || txtName.getText().trim().isEmpty()) {
            myOKAction.setEnabled(false);
        } else {
            myOKAction.setEnabled(true);
        }
    }

    private boolean validateNameAndThumbprint(String name, String thumb) {
        boolean retVal = true;
        try {
            retVal = CertificateDialogUtilMethods.validateNameAndThumbprint(name, thumb, mapCert);
        } catch (AzureCommonsException e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), e.getMessage());
        }
        return retVal;
    }

    private void importBtnListner() {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                return file.isDirectory() || (file.getExtension() != null && (file.getExtension().equals("pfx") || file.getExtension().equals("cer")));
            }

            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return (file.getExtension() != null && (file.getExtension().equals("pfx") || file.getExtension().equals("cer")));
            }
        };
        fileChooserDescriptor.setTitle("Select Certificate");

        FileChooser.chooseFile(fileChooserDescriptor, null, null, new Consumer<VirtualFile>() {
            @Override
            public void consume(VirtualFile virtualFile) {
                if (virtualFile != null) {
                    String path = virtualFile.getPath();
                    String password = null;
                    boolean proceed = true;
                    if (path.endsWith(".pfx")) {
                        SimplePfxPwdDlg dlg = new SimplePfxPwdDlg(path);
                        dlg.show();
                        if (dlg.isOK()) {
                            password = dlg.getPwd();
                        } else {
                            proceed = false;
                        }
                    }
                    if (proceed) {
                        X509Certificate cert = CerPfxUtil.getCert(path, password);
                        if (cert != null) {
                            if (txtName.getText().isEmpty()) {
                                populateCertName(CertificateDialogUtilMethods.removeSpaceFromCN(cert.getSubjectDN().getName()));
                            }
                            String thumbprint = "";
                            try {
                                thumbprint = CerPfxUtil.getThumbPrint(cert);
                            } catch (Exception e) {
                                PluginUtil.displayErrorDialog(message("certErrTtl"), message("certImpEr"));
                            }
                            txtThumb.setText(thumbprint);
                        }
                    }
                }
            }
        });
    }

    /**
     * Method checks if certificate name is already
     * used then make it unique by concatenating current date.
     *
     * @param certNameParam
     */
    private void populateCertName(String certNameParam) {
        txtName.setText(CertificateDialogUtilMethods.populateCertName(certNameParam, mapCert));
    }

    private void newBtnListener() {
        NewCertificateDialogData data = new NewCertificateDialogData();
        String jdkPath;
        try {
            jdkPath = waRole.getJDKSourcePath();
        } catch (Exception e) {
            jdkPath = "";
        }
        NewCertificateDialog dialog = new NewCertificateDialog(data, jdkPath, module.getProject());
        dialog.show();
        if (dialog.isOK()) {
            if (txtName.getText().isEmpty()) {
                populateCertName(CertificateDialogUtilMethods.removeSpaceFromCN(data.getCnName()));
            }
            try {
                txtThumb.setText(CerPfxUtil.getThumbPrint(data.getCerFilePath()));
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("certErrTtl"), message("certImpEr"));
            }
        }
    }
}
