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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.interopbridges.tools.windowsazure.WindowsAzureCertificate;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.util.WAEclipseHelperMethods;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class CertificatesPanel {
    private JPanel contentPane;
    private JPanel tablePanel;
    private TableView<WindowsAzureCertificate> tblCertificates;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private Map<String, WindowsAzureCertificate> mapCert;
    public String certSelected = "";
    private boolean modified;

    public CertificatesPanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        init();
    }

    private void init() {
        try {
            mapCert = waRole.getCertificates();
            CertificatesTableModel myModel = new CertificatesTableModel(new ArrayList<WindowsAzureCertificate>(mapCert.values()));
            tblCertificates.setModelAndUpdateColumns(myModel);
            tblCertificates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
        }
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public JPanel getPanel() {
        return contentPane;
    }

    private final ColumnInfo<WindowsAzureCertificate, String> NAME = new ColumnInfo<WindowsAzureCertificate, String>(message("evColName")) {
        public String valueOf(WindowsAzureCertificate object) {
            return object.getName();
        }

        @Override
        public void setValue(WindowsAzureCertificate cert, String modifiedVal) {
            try {
                String certName = modifiedVal.trim();
                if (certName.isEmpty()) {
                    PluginUtil.displayErrorDialog(message("certErrTtl"), message("certInvMsg"));
                } else {
                    if (WAEclipseHelperMethods.isAlphaNumericUnderscore(certName)) {
                        boolean isValidName = true;
                        for (Iterator<String> iterator = mapCert.keySet().iterator(); iterator.hasNext();) {
                            String key = iterator.next();
                            if (key.equalsIgnoreCase(certName)) {
                                isValidName = false;
                                break;
                            }
                        }
                        if (isValidName || certName.equalsIgnoreCase(cert.getName())) {
                            cert.setName(certName);
                        } else {
                            PluginUtil.displayErrorDialog(message("certErrTtl"), message("certAddErrMsg"));
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("certErrTtl"), message("certRegMsg"));
                    }
                }
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
        }
    };

    private final ColumnInfo<WindowsAzureCertificate, String> THUMBPRINT = new ColumnInfo<WindowsAzureCertificate, String>(message("evColName")) {
        public String valueOf(WindowsAzureCertificate object) {
            return object.getFingerPrint();
        }

        @Override
        public void setValue(WindowsAzureCertificate cert, String modifiedVal) {
            try {
                String modifiedTxt = modifiedVal.trim();
                if (modifiedTxt.isEmpty()) {
                    PluginUtil.displayErrorDialog(message("certErrTtl"), message("certInvMsg"));
                } else {
                    boolean isValidName = true;
                    for (Iterator<Map.Entry<String, WindowsAzureCertificate>> iterator = mapCert.entrySet().iterator(); iterator.hasNext();) {
                        WindowsAzureCertificate certObj = iterator.next().getValue();
                        if (certObj.getFingerPrint().equalsIgnoreCase(modifiedTxt)) {
                            isValidName = false;
                            break;
                        }
                    }
                    if (isValidName || modifiedTxt.equalsIgnoreCase(cert.getFingerPrint())) {
                        cert.setFingerPrint(modifiedTxt.toUpperCase());
                    } else {
                        PluginUtil.displayErrorDialog(message("certErrTtl"), message("certAddErrMsg"));
                    }
                }
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
        }
    };

    private void createUIComponents() {
        tblCertificates = new TableView<WindowsAzureCertificate>();
        tablePanel = ToolbarDecorator.createDecorator(tblCertificates, null)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addCertificate();
                    }
                }).setEditAction(null).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeCertificate();
                    }
                }).setEditActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblCertificates.getSelectedObject() != null;
                    }
                }).setRemoveActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblCertificates.getSelectedObject() != null;
                    }
                }).disableUpDownActions().createPanel();
        tablePanel.setPreferredSize(new Dimension(-1, 200));
    }

    private void addCertificate() {
        CertificateDialog dialog = new CertificateDialog(module, mapCert, waRole);
        dialog.show();
        if (dialog.isOK()) {
            modified = true;
            String name = dialog.getNewlyAddedCert();
            tblCertificates.getListTableModel().addRow(mapCert.get(name));
            List<WindowsAzureCertificate> items = tblCertificates.getItems();
            for (int i = 0; i < items.size(); i++) {
                WindowsAzureCertificate cert = items.get(i);
                if (cert.getName().equalsIgnoreCase(name)) {
                    tblCertificates.addSelection(cert);
                    break;
                }
            }
            decideCurSelectedCert();
        }
    }

    private void removeCertificate() {
        try {
//                Map.Entry<String, WindowsAzureCertificate> certEntry =
//                        (Map.Entry<String, WindowsAzureCertificate>)
//                                tblViewer.getTable().getItem(selIndex).getData();
//                WindowsAzureCertificate delCert = certEntry.getValue();
            WindowsAzureCertificate delCert = tblCertificates.getSelectedObject();
            if (delCert.isRemoteAccess() && delCert.isSSLCert()) {
                String temp = String.format("%s%s%s", message("sslTtl"), " and ", message("cmhLblRmtAces"));
                PluginUtil.displayErrorDialog(message("certRmTtl"), String.format(message("certComMsg"), temp, temp));
            } else if (delCert.isRemoteAccess()) {
                PluginUtil.displayErrorDialog(message("certRmTtl"), String.format(message("certComMsg"), message("cmhLblRmtAces"), message("cmhLblRmtAces")));
            } else if (delCert.isSSLCert()) {
                PluginUtil.displayErrorDialog(message("certRmTtl"), String.format(message("certComMsg"), message("sslTtl"), message("sslTtl")));
            } else {
                int choice = Messages.showOkCancelDialog(String.format(message("certRmMsg"), delCert.getName()), message("certRmTtl"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    delCert.delete();
                    certSelected = "";
                    modified = true;
                    tblCertificates.getListTableModel().removeRow(tblCertificates.getSelectedRow());
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
        }
    }

    private void decideCurSelectedCert() {
        if (tblCertificates.getSelectedObject() != null) {
            WindowsAzureCertificate cert = tblCertificates.getSelectedObject();
            certSelected = cert.getName();
        } else {
            certSelected = "";
        }
    }

    private class CertificatesTableModel extends ListTableModel<WindowsAzureCertificate> {
        private CertificatesTableModel(java.util.List<WindowsAzureCertificate> listCertificates) {
            super(new ColumnInfo[]{NAME, THUMBPRINT}, listCertificates, 0);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            boolean retVal = true;
            @SuppressWarnings("unchecked")
            WindowsAzureCertificate certificate = getRowValue(rowIndex);
            try {
                if (certificate.isRemoteAccess() || certificate.isSSLCert()) {
                    retVal = false;
                }
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("certErrTtl"), message("certErrMsg"), e);
            }
            return retVal;
        }
    }
}
