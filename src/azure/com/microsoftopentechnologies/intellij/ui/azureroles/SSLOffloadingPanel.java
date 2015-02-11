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
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import com.interopbridges.tools.windowsazure.*;
import com.microsoftopentechnologies.azurecommons.roleoperations.WASSLOffloadingUtilMethods;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class SSLOffloadingPanel extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final int HTTPS_NXT_PORT = 8443;

    private JPanel contentPane;
    private JCheckBox btnSSLOffloading;
    private JComboBox comboEndpt;
    private JComboBox comboCert;
    private JTextPane linkNote;
    private JXHyperlink linkCert;
    private JXHyperlink linkEndpoint;
    private JLabel lblEndptToUse;
    private JLabel lblCert;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;

    private boolean isManualUpdate = true;

    public SSLOffloadingPanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        init();
    }

    private void init() {
        btnSSLOffloading.addActionListener(createBtnSSLOffloadingListener());
        comboEndpt.addItemListener(createComboEndptListener());
        linkEndpoint.setAction(createLinkEndpointAction());
        comboCert.addItemListener(createComboCertListener());
        linkCert.setAction(createLinkCertAction());
        Messages.configureMessagePaneUi(linkNote, message("lbSslNote"));

        try {
            if (waRole != null) {
                isManualUpdate = false;
                WindowsAzureEndpoint sslEndpt = waRole.getSslOffloadingInputEndpoint();
                btnSSLOffloading.setSelected(sslEndpt != null);
                if (sslEndpt == null) {
                    enableDisableControls(false);
                } else {
                    enableDisableControls(true);
                    populateCertList();
                    populateEndPointList();
                    comboCert.setSelectedItem(waRole.getSslOffloadingCert().getName());
                    comboEndpt.setSelectedItem(String.format(message("dbgEndPtStr"), sslEndpt.getName(), sslEndpt.getPort(), sslEndpt.getPrivatePort()));
                    isEditableEndpointCombo(sslEndpt);
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("dlgDbgErr"), e);
        } finally {
            isManualUpdate = true;
        }
    }

    private ActionListener createBtnSSLOffloadingListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableControls(btnSSLOffloading.isSelected());
                if (btnSSLOffloading.isSelected()) {
                    enableSslOffloading();
                    setModified(true);
                } else {
                    try {
                        waRole.setSslOffloading(null, null);
                        setModified(true);
                    } catch (WindowsAzureInvalidProjectOperationException ex) {
                        PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
                    }
                }
                removeErrorMsg();
                try {
                    handlePageComplete(true);
                } catch (ConfigurationException e1) {
                    // ignore
                }
            }
        };
    }

    private ItemListener createComboEndptListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && isManualUpdate) {
                    try {
                        isManualUpdate = false;
                        setModified(true);
                        String newText = (String) comboEndpt.getSelectedItem();
                        int port = Integer.valueOf(newText.substring(newText.indexOf(":") + 1, newText.indexOf(",")));
                        if (port == HTTPS_PORT) {
                            // user trying to set endpoint with public port 443
                            PluginUtil.displayWarningDialog(message("sslTtl"), message("sslWarnMsg"));
                        } else if (port == HTTP_PORT) {
                            WindowsAzureEndpoint httpsEndPt = WAEclipseHelperMethods.findEndpointWithPubPort(HTTPS_PORT, waRole);
                            if (httpsEndPt != null) {
                                /*
                                 * If HTTPS endpoint with public port 443,
					             * is present on same role and listed in endpoint combo box
					             * then show warning
					             */
                                PluginUtil.displayWarningDialog(message("sslTtl"), String.format(message("httpsPresent"), httpsEndPt.getName(),
                                        httpsEndPt.getPort()));
                                comboEndpt.setSelectedItem(null);
                            } else {
                                WindowsAzureRole role = WAEclipseHelperMethods.findRoleWithEndpntPubPort(HTTPS_PORT, waProjManager);
                                WindowsAzureEndpoint httpEndPt = WAEclipseHelperMethods.findEndpointWithPubPort(HTTP_PORT, waRole);
                                int pubPort = HTTPS_NXT_PORT;
                                if (role != null) {
                                    /*
                                     * Else if endpoint with public port 443
						             * is already used by some other role or
						             * on same role but with type InstanceInput
						             * then prompt for changing port 80
						             * with the next available public port starting with 8443
						             * across all roles
						             */
                                    while (!waProjManager.isValidPort(String.valueOf(pubPort), WindowsAzureEndpointType.Input)) {
                                        pubPort++;
                                    }
                                } else {
                                    // Else prompt for changing port 80 with 443 across all roles
                                    pubPort = HTTPS_PORT;
                                }
                                int choice = Messages.showYesNoDialog(message("sslhttp").replace("${epName}", httpEndPt.getName()).replace("${pubPort}",
                                                String.valueOf(pubPort)).replace("${privPort}", httpEndPt.getPrivatePort()),
                                        message("sslTtl"), Messages.getQuestionIcon());
                                if (choice == Messages.YES) {
                                    httpEndPt.setPort(String.valueOf(pubPort));
                                    populateEndPointList();
                                    comboEndpt.setSelectedItem(String.format(message("dbgEndPtStr"), httpEndPt.getName(), httpEndPt.getPort(),
                                            httpEndPt.getPrivatePort()));
                                    isEditableEndpointCombo(httpEndPt);
                                } else {
                                    comboEndpt.setSelectedItem(null);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log(message("sslTtl"), ex);
                    } finally {
                        isManualUpdate = true;
                    }
                    removeErrorMsg();
                }
            }
        };
    }

    private Action createLinkEndpointAction() {
        return new AbstractAction(message("linkLblEndpt")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open remote access dialog
                String curSel = (String) comboEndpt.getSelectedItem();
                final DefaultDialogWrapper endpointsDialog = new DefaultDialogWrapper(module.getProject(), new RoleEndpointsPanel(module, waProjManager, waRole));
                endpointsDialog.show();

                if (endpointsDialog.isOK() && btnSSLOffloading.isSelected()) {
                    try {
                        populateEndPointList();
                        for (int i = 0; i < comboEndpt.getItemCount(); i++) {
                            if (comboEndpt.getItemAt(i).equals(curSel)) {
                                comboEndpt.setSelectedIndex(i);
                                return;
                            }
                        }
                        comboEndpt.setSelectedItem(null);
                    } catch (WindowsAzureInvalidProjectOperationException ex) {
                        log(message("dlgDbgEndPtErrTtl"), ex);
                    }
                }
            }
        };
    }

    private ItemListener createComboCertListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                removeErrorMsg();
            }
        };
    }

    private Action createLinkCertAction() {
        return new AbstractAction(message("linkLblCert")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open remote access dialog
                String curSel = (String) comboCert.getSelectedItem();

                // open remote access dialog
                final DefaultDialogWrapper certificatesDialog = new DefaultDialogWrapper(module.getProject(), new CertificatesPanel(module, waProjManager, waRole));
                certificatesDialog.show();

                if (certificatesDialog.isOK() && btnSSLOffloading.isSelected()) {
                    try {
                        String pageSel = certificatesDialog.getSelectedValue();
                        String nameToSet;
                        if (pageSel != null && !pageSel.isEmpty()) {
                            nameToSet = pageSel;
                        } else {
                            nameToSet = curSel;
                        }
                        populateCertList();
                        if (nameToSet.equalsIgnoreCase(message("remoteAccessPasswordEncryption"))) {
                            PluginUtil.displayErrorDialog(message("genErrTitle"), message("usedByRemAcc"));
                        } else {
                            for (int i = 0; i < comboCert.getItemCount(); i++) {
                                if (comboCert.getItemAt(i).equals(nameToSet)) {
                                    comboCert.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                        removeErrorMsg();
                    } catch (WindowsAzureInvalidProjectOperationException ex) {
                        log(message("certErrTtl"), ex);
                    }
                }
            }
        };
    }

    private void enableSslOffloading() {
        try {
            WindowsAzureEndpoint endpt = null;
            populateEndPointList();
            populateCertList();
            endpt = findInputEndpt();
            populateEndPointList();
            /*
             * If Endpoint is null,
			 * 1. Check if session affinity
			 * is enabled or no appropriate endpoints to populate,
			 * if yes then uncheck
			 * SSL check box
			 * 2. Else don't do anything
			 * keep blank in combo box.
			 */
            if (endpt == null) {
                if (waRole.getSessionAffinityInputEndpoint() != null) {
                    btnSSLOffloading.setSelected(false);
                    enableDisableControls(false);
                }
				/*
				 * No endpoints appropriate for SSL offloading,
				 * neither user wants to create new endpoint
				 * nor there is single valid endpoint on that role to list in endpoint combo box
				 * (i.e. zero endpoints on that role or all endpoints of type internal)
				 * then just prompt and exit
				 */
                else if (comboEndpt.getItemCount() < 1) {
                    PluginUtil.displayWarningDialog(message("sslTtl"), message("noEndPtMsg"));
                    btnSSLOffloading.setSelected(false);
                    enableDisableControls(false);
                }
            } else {
                comboEndpt.setSelectedItem(String.format(message("dbgEndPtStr"), endpt.getName(), endpt.getPort(), endpt.getPrivatePort()));
                isEditableEndpointCombo(endpt);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
    }

    private WindowsAzureEndpoint findInputEndpt() throws WindowsAzureInvalidProjectOperationException {
        WindowsAzureEndpoint endpt = null;
        WindowsAzureEndpoint sessionAffEndPt = waRole.getSessionAffinityInputEndpoint();
        // check session affinity is already enabled, then consider same endpoint
        if (sessionAffEndPt != null) {
            // check port of session affinity endpoint
            String stSesPubPort = sessionAffEndPt.getPort();
            if (stSesPubPort.equalsIgnoreCase(String.valueOf(HTTP_PORT))) {
                // check 443 is already available on same role (input enpoint)
                WindowsAzureEndpoint httpsEndPt = WAEclipseHelperMethods.findEndpointWithPubPort(HTTPS_PORT, waRole);
                if (httpsEndPt != null) {
					/*
					 * If HTTPS endpoint with public port 443,
					 * is present on same role then show warning
					 */
                    PluginUtil.displayWarningDialog(String.format(message("httpsPresentSt"), httpsEndPt.getName(), httpsEndPt.getPort(), httpsEndPt.getName()),
                            message("sslTtl"));
                    endpt = null;
                } else {
					/*
					 * Check if 443 is used on same role (instance endpoint)
					 * or any other role
					 * if yes then consider 8443.
					 */
                    int portToUse = HTTPS_PORT;
                    if (WAEclipseHelperMethods.findRoleWithEndpntPubPort(HTTPS_PORT, waProjManager) != null) {
                        // need to use 8443
                        int pubPort = HTTPS_NXT_PORT;
                        while (!waProjManager.isValidPort(String.valueOf(pubPort), WindowsAzureEndpointType.Input)) {
                            pubPort++;
                        }
                        portToUse = pubPort;
                    }
                    int choice = Messages.showYesNoDialog(message("sslhttp").replace("${epName}", sessionAffEndPt.getName())
                                    .replace("${pubPort}", String.valueOf(portToUse)).replace("${privPort}", sessionAffEndPt.getPrivatePort()),
                            message("sslTtl"), Messages.getQuestionIcon());
                    if (choice == Messages.YES) {
                        sessionAffEndPt.setPort(String.valueOf(portToUse));
                        endpt = sessionAffEndPt;
                    } else {
                        // no button pressed
                        endpt = null;
                    }
                }
            } else {
                // port is other than 80, then directly consider it.
                endpt = sessionAffEndPt;
            }
        } else {
            // check this role uses public port 443
            endpt = WAEclipseHelperMethods.findEndpointWithPubPort(HTTPS_PORT, waRole);
            if (endpt != null) {
                // endpoint on this role uses public port 443
                PluginUtil.displayWarningDialog(message("sslTtl"), message("sslWarnMsg"));
            } else {
                // check if another role uses 443 as a public port
                WindowsAzureRole roleWithHTTPS = WAEclipseHelperMethods.findRoleWithEndpntPubPort(HTTPS_PORT, waProjManager);
                if (roleWithHTTPS != null) {
                    // another role uses 443 as a public port
                    // 1. If this role uses public port 80
                    endpt = WAEclipseHelperMethods.findEndpointWithPubPort(HTTP_PORT, waRole);
                    if (endpt != null) {
						/*
						 * endpoint on this role uses public port 80
						 * and 443 has been used on some other role then set to 8443
						 * or some suitable public port
						 */
                        int pubPort = HTTPS_NXT_PORT;
                        while (!waProjManager.isValidPort(String.valueOf(pubPort), WindowsAzureEndpointType.Input)) {
                            pubPort++;
                        }
                        int choice = Messages.showYesNoDialog(message("sslhttp").replace("${epName}", endpt.getName())
                                        .replace("${pubPort}", String.valueOf(pubPort)).replace("${privPort}", endpt.getPrivatePort()),
                                message("sslTtl"), Messages.getQuestionIcon());
                        if (choice == Messages.YES) {
                            endpt.setPort(String.valueOf(pubPort));
                        } else {
                            // no button pressed
                            endpt = null;
                        }
                    } else {
                        // 2. Ask for creating new endpoint
                        List<String> endPtData = WASSLOffloadingUtilMethods.prepareEndpt(HTTPS_NXT_PORT, waRole, waProjManager);
                        int choice = Messages.showYesNoCancelDialog(String.format(message("sslNoHttp"), endPtData.get(0), endPtData.get(1), endPtData.get(2)),
                                message("sslTtl"), Messages.getQuestionIcon());
                        if (choice == Messages.YES) {
                            endpt = waRole.addEndpoint(endPtData.get(0), WindowsAzureEndpointType.Input, endPtData.get(2), endPtData.get(1));
                            setModified(true);
                        } else {
                            // no button pressed
                            endpt = null;
                        }
                    }
                } else {
                    // no public port 443 on this role, nor on other any role
                    // 1. If this role uses public port 80
                    endpt = WAEclipseHelperMethods.findEndpointWithPubPort(HTTP_PORT, waRole);
                    if (endpt != null) {
                        // endpoint on this role uses public port 80
                        int choice = Messages.showYesNoDialog(message("sslhttp").replace("${epName}", endpt.getName())
                                        .replace("${pubPort}", String.valueOf(HTTPS_PORT)).replace("${privPort}", endpt.getPrivatePort()),
                                message("sslTtl"), Messages.getQuestionIcon());
                        if (choice == Messages.YES) {
                            endpt.setPort(String.valueOf(HTTPS_PORT));
                        } else {
                            // no button pressed
                            endpt = null;
                        }
                    } else {
                        // 2. Ask for creating new endpoint
                        List<String> endPtData = WASSLOffloadingUtilMethods.prepareEndpt(HTTPS_PORT, waRole, waProjManager);
                        int choice = Messages.showYesNoDialog(String.format(message("sslNoHttp"), endPtData.get(0), endPtData.get(1), endPtData.get(2)),
                                message("sslTtl"), Messages.getQuestionIcon());
                        if (choice == Messages.YES) {
                            endpt = waRole.addEndpoint(endPtData.get(0), WindowsAzureEndpointType.Input, endPtData.get(2), endPtData.get(1));
                            setModified(true);
                        } else {
                            // no button pressed
                            endpt = null;
                        }
                    }
                }
            }
        }
        return endpt;
    }


    private void enableDisableControls(boolean value) {
        lblEndptToUse.setEnabled(value);
        lblCert.setEnabled(value);
        comboEndpt.setEnabled(value);
        comboCert.setEnabled(value);
        if (!value) {
            comboEndpt.removeAllItems();
            comboCert.removeAllItems();
        }
    }

    /**
     * Populates endpoints having type input in combo box.
     *
     * @throws WindowsAzureInvalidProjectOperationException
     */
    private void populateEndPointList() throws WindowsAzureInvalidProjectOperationException {
        List<WindowsAzureEndpoint> endpointsList = waRole.getEndpoints();
        comboEndpt.removeAllItems();
        for (WindowsAzureEndpoint endpoint : endpointsList) {
            if (endpoint.getEndPointType().equals(WindowsAzureEndpointType.Input) && endpoint.getPrivatePort() != null
                    && !endpoint.equals(waRole.getDebuggingEndpoint())) {
                isManualUpdate = false;
                comboEndpt.addItem(String.format(message("dbgEndPtStr"), endpoint.getName(), endpoint.getPort(), endpoint.getPrivatePort()));
                isManualUpdate = true;
            }
        }
    }

    private void populateCertList() throws WindowsAzureInvalidProjectOperationException {
        comboCert.removeAllItems();
        for (Iterator<Map.Entry<String, WindowsAzureCertificate>> iterator = waRole.getCertificates().entrySet().iterator(); iterator.hasNext(); ) {
            WindowsAzureCertificate cert = iterator.next().getValue();
            if (!cert.isRemoteAccess()) {
                isManualUpdate = false;
                comboCert.addItem(cert.getName());
                isManualUpdate = true;
            }
        }
    }

    private void isEditableEndpointCombo(WindowsAzureEndpoint endPt)
            throws WindowsAzureInvalidProjectOperationException {
        if (endPt.equals(waRole.getSessionAffinityInputEndpoint())) {
            comboEndpt.setEnabled(false);
        } else {
            comboEndpt.setEnabled(true);
        }
    }

    private void removeErrorMsg() {
        String endPtStr = comboEndpt.getSelectedItem() == null ? null : ((String) comboEndpt.getSelectedItem()).trim();
        String certStr = comboCert.getSelectedItem() == null ? null : ((String) comboCert.getSelectedItem()).trim();
        if (btnSSLOffloading.isSelected()) {
            if (endPtStr != null && !endPtStr.isEmpty() && certStr != null && !certStr.isEmpty()) {
//                setErrorMessage(null);
            }
        } else {
//            setErrorMessage(null);
        }
    }

    private boolean handlePageComplete(boolean isOkToLeave) throws ConfigurationException {
        boolean okToProceed = true;
        if (btnSSLOffloading.isSelected()) {
            String endPtStr = comboEndpt.getSelectedItem() == null ? null : ((String) comboEndpt.getSelectedItem()).trim();
            String certStr = comboCert.getSelectedItem() == null ? null : ((String) comboCert.getSelectedItem()).trim();
            if (endPtStr == null || certStr == null || endPtStr.isEmpty() || certStr.isEmpty()) {
                okToProceed = false;
                if (isOkToLeave) {
//                    setErrorMessage(message("eptNmEndPtMsg"));
                } else {
                    PluginUtil.displayErrorDialog(message("genErrTitle"), message("eptNmEndPtMsg"));
                    throw new ConfigurationException(message("eptNmEndPtMsg"), message("genErrTitle"));
                }
            } else {
                String endpointName = endPtStr.substring(0, endPtStr.indexOf("(") - 1);
                try {
                    WindowsAzureEndpoint newEndPt = waRole.getEndpoint(endpointName);
                    WindowsAzureCertificate newCert = waRole.getCertificate(certStr);
                    // check if SSL offloading is already configured
                    WindowsAzureEndpoint oldEndPt = waRole.getSslOffloadingInputEndpoint();
                    WindowsAzureCertificate oldCert = waRole.getSslOffloadingCert();
                    if (newEndPt != null && newCert != null) {
                        if (oldEndPt != null && oldCert != null) {
                            if (!oldEndPt.getName().equalsIgnoreCase(newEndPt.getName())) {
                                waRole.setSslOffloading(newEndPt, newCert);
                            } else if (!oldCert.getName().equalsIgnoreCase(newCert.getName())) {
                                waRole.setSslOffloadingCert(newCert);
                            }
                        } else {
                            waRole.setSslOffloading(newEndPt, newCert);
                        }
                    } else {
                        okToProceed = false;
                    }
                } catch (WindowsAzureInvalidProjectOperationException e) {
                    okToProceed = false;
                }
            }
        }
        return okToProceed;
    }

    @NotNull
    @Override
    public String getId() {
        return getDisplayName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return message("cmhLblSsl");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_ssl_offloading_page";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (handlePageComplete(false)) {
            try {
                waProjManager.save();
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
                throw new ConfigurationException(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"));
            }
        }
        setModified(false);
    }

    @Override
    public void reset() {
        setModified(false);
    }

    @Override
    public void disposeUIResources() {

    }
}
