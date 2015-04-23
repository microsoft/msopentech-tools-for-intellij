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
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.interopbridges.tools.windowsazure.*;
import com.microsoftopentechnologies.azurecommons.roleoperations.JdkSrvConfigUtilMethods;
import com.microsoftopentechnologies.azurecommons.roleoperations.WAServerConfUtilMethods;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.ui.util.JdkSrvConfig;
import com.microsoftopentechnologies.intellij.ui.util.JdkSrvConfigListener;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.intellij.util.WAHelper;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;


public class JdkServerPanel {
    private final String AUTO = "auto";
    private final int HTTP_PORT = 80;

    private JPanel rootPanel;
    private JPanel jdkSettings;
    private JPanel serverSettings;
    private JTabbedPane settingsPane;
    private JCheckBox jdkCheckBox;
    private TextFieldWithBrowseButton jdkPath;
    private JRadioButton uploadLocalJdk;
    private JRadioButton thirdPartyJdk;
    private JComboBox thirdPartyJdkName;
    private JRadioButton customDownloadJdk;
    private JTextField jdkUrl;
    private JComboBox storageAccountJdk;
    private JTextField javaHome;
    private JLabel lblJavaHome;
    private JLabel lblNoteJavaHome;
    //Server tab
    private JCheckBox serverCheckBox;
    private TextFieldWithBrowseButton serverPath;
    private JComboBox serverType;
    private JRadioButton thrdPrtSrvBtn;
    private JRadioButton uploadLocalServer;
    private JRadioButton customDownloadServer;
    private JTextField serverUrl;
    private JComboBox storageAccountServer;
    private JTextField serverHomeDir;
    private JXHyperlink accountsButton;
    private JXHyperlink serverAccountsButton;
    private JLabel jdkUrlLabel;
    private JLabel storageAccountJdkLabel;
    private JLabel lblHomeDir;
    private JLabel lblDlNoteUrl;
    private JLabel lblSelect;
    private JLabel lblUrlSrv;
    private JLabel lblDlNoteUrlSrv;
    private JLabel lblKeySrv;
    private JPanel applicationsSettings;
    private JLabel lblNoteHomeDir;
    private JComboBox thrdPrtSrvCmb;

    private ApplicationsTab applicationsTab;

    private final Project project;
    private final WindowsAzureRole waRole;
    private final WindowsAzureProjectManager waProjManager;
    private boolean accepted = false;
    private String jdkPrevName;
    private final ArrayList<String> fileToDel = new ArrayList<String>();
    private String finalSrvPath;
    private WindowsAzureRoleComponentImportMethod finalImpMethod;
    private String finalAsName;
    private String finalJdkPath;
    private boolean isManualUpdate = true;
    private boolean modified;

    public JdkServerPanel(Project project, WindowsAzureRole waRole, WindowsAzureProjectManager waProjManager) {
        this.project = project;
        this.waRole = waRole;
        this.waProjManager = waProjManager;
        applicationsTab.init(project, waProjManager, waRole, fileToDel);
        applicationsTab.initAppTab();
        init();
        // preference page
        if (waProjManager != null) {
            initForPreference();
            initJdkTab();
            initServerTab();
        } else {
            initForWizard();
        }
    }

    public int getSelectedIndex() {
        return settingsPane.getSelectedIndex();
    }

    public void setSelectedIndex(int currentTab) {
        settingsPane.setSelectedIndex(currentTab);
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public boolean isModified() {
        return modified || applicationsTab.isModified();
    }

    public void setModified(boolean modified) {
        this.modified = modified;
        applicationsTab.setModified(modified);
    }

    public ApplicationsTab getApplicationsTab() {
        return applicationsTab;
    }

    public ArrayList<String> getFileToDel() {
        return fileToDel;
    }

    public String getFinalJdkPath() {
        return finalJdkPath;
    }

    public String getFinalSrvPath() {
        return finalSrvPath;
    }

    public WindowsAzureRoleComponentImportMethod getFinalImpMethod() {
        return finalImpMethod;
    }

    public String getFinalAsName() {
        return finalAsName;
    }

    private void initJdkTab() {
        // Check JDK is already enabled or not
        // and if enabled show appropriate values on property page
        try {
            String jdkSrcPath = waRole.getJDKSourcePath();
            if (jdkSrcPath == null) {
                setEnableJDK(false);
                setEnableDlGrp(false, false);
                uploadLocalJdk.setSelected(true);
            } else {
                if (jdkSrcPath.isEmpty()) {
                    setEnableJDK(false);
                } else {
                    setEnableJDK(true);
                    jdkPath.setText(jdkSrcPath);
                }
                String jdkName = waRole.getJDKCloudName();
                // project may be using deprecated JDK, hence pass to method
                showThirdPartyJdkNames(jdkName);
                String jdkUrlValue = waRole.getJDKCloudURL();
                // JDK download group
                if (jdkUrl != null && !jdkUrlValue.isEmpty()) {
                    // JDK auto upload option configured
                    if (JdkSrvConfigUtilMethods.isJDKAutoUploadPrevSelected(waRole)) {
                        setEnableDlGrp(true, true);
                        // check for third party JDK
                        if (jdkName.isEmpty()) {
                            uploadLocalJdk.setSelected(true);
                        } else {
                            thirdPartyJdk.setSelected(true);
                            enableThirdPartyJdkCombo(true);
                            thirdPartyJdkName.setSelectedItem(jdkName);
                            /*
							 * License has already been accepted
							 * on wizard or property page previously.
							 */
                            accepted = true;
                            jdkPrevName = jdkName;
                        }
                    } else {
                        // JDK deploy option configured
                        uploadLocalJdk.setSelected(false);
                        customDownloadJdk.setSelected(true);
                        setEnableDlGrp(true, false);
                    }

                    // Update URL text box
                    if (jdkUrlValue.equalsIgnoreCase(AUTO)) {
                        jdkUrlValue = JdkSrvConfig.AUTO_TXT;
                    }
                    jdkUrl.setText(jdkUrlValue);

                    // Update JAVA_HOME text box
                    if (waProjManager.getPackageType().equals(WindowsAzurePackageType.LOCAL)) {
                        javaHome.setText(waRole.getJDKCloudHome());
                    } else {
                        javaHome.setText(waRole.getRuntimeEnv(message("jvHome")));
                    }

                    // Update note below JDK URL text box
                    if (jdkSrcPath.isEmpty()) {
                        lblDlNoteUrl.setText(message("dlgDlNtLblUrl"));
                    } else {
                        String dirName = new File(jdkSrcPath).getName();
                        lblDlNoteUrl.setText(String.format(message("dlNtLblDir"), dirName));
                    }
                    // Update storage account combo box.
                    String jdkKey = waRole.getJDKCloudKey();
                    UIUtils.populateStrgNameAsPerKey(jdkKey, storageAccountJdk);
                }
            }

            uploadLocalJdk.addActionListener(createUploadLocalJdkListener());
            checkSDKPresenceAndEnable();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("jdkPathErrTtl"), message("getJdkErrMsg"), e);
        }
    }

    private void initServerTab() {
        // Check Server is already enabled or not
        // and if enabled show appropriate values on property page
        try {
            String srvSrcPath = null;
            String srvName = null;

            srvSrcPath = waRole.getServerSourcePath();
            srvName = waRole.getServerName();

            if (srvSrcPath != null && srvName != null) {
                serverCheckBox.setSelected(true);
                setEnableServer(true);
                isManualUpdate = false;
                serverType.setSelectedItem(srvName);
                serverPath.setText(srvSrcPath);
                // Server download group
                String srvUrl = waRole.getServerCloudURL();
                if (srvUrl != null && !srvUrl.isEmpty()) {
                    // server auto upload option configured
                    if (JdkSrvConfigUtilMethods.isServerAutoUploadPrevSelected(waRole)) {
                        uploadLocalServer.setSelected(true);
                        setEnableDlGrpSrv(true, true);
                    } else {
                        // server deploy option configured
                        customDownloadServer.setSelected(true);
                        setEnableDlGrpSrv(true, false);
                    }
                    if (srvUrl.equalsIgnoreCase(AUTO)) {
                        srvUrl = JdkSrvConfig.AUTO_TXT;
                    }
                    serverUrl.setText(srvUrl);
                    // Update server home text box
                    if (waProjManager.getPackageType().equals(WindowsAzurePackageType.LOCAL)) {
                        serverHomeDir.setText(waRole.getServerCloudHome());
                    } else {
                        serverHomeDir.setText(waRole.getRuntimeEnv(waRole.getRuntimeEnvName(message("typeSrvHm"))));
                    }
                    // Update note below Server URL text box
                    String dirName = new File(srvSrcPath).getName();
                    lblDlNoteUrlSrv.setText(String.format(message("dlNtLblDir"), dirName));
                    String srvKey = waRole.getServerCloudKey();
                    UIUtils.populateStrgNameAsPerKey(srvKey, storageAccountServer);
                }
            } else {
                setEnableServer(false);
                setEnableDlGrpSrv(false, false);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("getSrvBothErrMsg"), e);
        } finally {
            isManualUpdate = true;
        }
        if (jdkCheckBox.isSelected() || thirdPartyJdk.isSelected() || customDownloadJdk.isSelected()) {
            serverCheckBox.setEnabled(true);
        }
    }

    public void checkSDKPresenceAndEnable() {
        String sdkVersion = WindowsAzureProjectManager.getLatestAzureVersionForSA();
        if (sdkVersion == null || sdkVersion.isEmpty()) {
            jdkCheckBox.setEnabled(false);
            setEnableJDK(false);
        }
    }

//    @Override
//    public JComponent prepare(final WizardNavigationState state) {
//        rootPanel.revalidate();
//        state.FINISH.setEnabled(true);
//        return rootPanel;
//    }

    public void init() {
        accountsButton.setAction(createStorageAccountsAction(storageAccountJdk, JdkSrvConfig.JDK_TXT));
        serverAccountsButton.setAction(createStorageAccountsAction(storageAccountServer, JdkSrvConfig.SRV_TXT));
        uploadLocalServer.addActionListener(createUploadLocalServerListener());
        customDownloadServer.addActionListener(createCustomDownloadServerListener());
//        thirdPartyJdkName = new JComboBox(JdkSrvConfigListener.getThirdPartyJdkNames(true));
        thirdPartyJdk.addActionListener(createThirdPartyJdkListener());
        customDownloadJdk.addActionListener(createCustomDownloadJdkListener());
//        uploadLocalJdk.setSelected(true);
        showThirdPartyJdkNames("");
        jdkUrl.getDocument().addDocumentListener(createJdkUrlListener());
        storageAccountJdk.addItemListener(createStorageAccountJdkListener());
        thirdPartyJdkName.addItemListener(createThirdPartyJdkNameListener());
//        setEnableDlGrp(false, false);
        setEnableDlGrpSrv(false, false);
        storageAccountServer.addItemListener(createStorageAccountServerListener());
        serverUrl.getDocument().addDocumentListener(createServerUrlListener());
        checkSDKPresenceAndEnable();
        settingsPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isManualUpdate && createAccLicenseAggDlg() && (waProjManager == null || configureJdkCloudDeployment())) {
                } else {
                    try {
                        isManualUpdate = false;
                        settingsPane.setSelectedIndex(0);
                    } finally {
                        isManualUpdate = true;
                    }
                }
            }
        });
    }

    public void initForWizard() {
        jdkCheckBox.addItemListener(createJdkCheckBoxListener());
        jdkPath.addActionListener(UIUtils.createFileChooserListener(jdkPath, null, FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        jdkPath.getTextField().getDocument().addDocumentListener(createJdkPathListener());
        uploadLocalJdk.addActionListener(createUploadLocalJdkListener());
        uploadLocalJdk.setSelected(true);
        jdkChkBoxUnChecked();
        serverPath.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            protected void onFileChoosen(@NotNull VirtualFile chosenFile) {
                super.onFileChoosen(chosenFile);
                serBrowseBtnListener();
                modifySrvText(waRole, message("dlNtLblDirSrv"));
                modified = true;
            }
        });
        serverCheckBox.addItemListener(createServerListener());
        serverCheckBox.setSelected(false);
    }

    private void initForPreference() {
        jdkPath.addFocusListener(createJdkPathPreferenceListener());
        jdkCheckBox.addItemListener(createJdkCheckBoxPreferenceListener());
        jdkPath.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            protected void onFileChoosen(@NotNull VirtualFile chosenFile) {
                String oldTxt = jdkPath.getText();
                super.onFileChoosen(chosenFile);
                String directory = jdkPath.getText();
                if (directory != null && !directory.equalsIgnoreCase(oldTxt)) {
                    modified = true;
                    modifyJdkText(waRole, message("dlNtLblDir"));
                }
            }
        });
        serverPath.addFocusListener(createServerPathPreferenceListener());
        serverCheckBox.addItemListener(createServerPreferenceListener());
        serverType.addItemListener(createServerTypePreferenceListener());
        serverPath.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            protected void onFileChoosen(@NotNull VirtualFile chosenFile) {
                super.onFileChoosen(chosenFile);
                serBrowseBtnListener();
                modifySrvText(waRole, message("dlNtLblDirSrv"));
                modified = true;
                /*
		         * Check server configured previously
		         * and now server name is changed.
		         */
                if (serverPath.getText() != null) {
                    updateServer((String) serverType.getSelectedItem(), serverPath.getText(), AzurePlugin.cmpntFile);
                }
            }
        });
        serverCheckBox.setSelected(false);
    }

    private ItemListener createServerTypePreferenceListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (isManualUpdate && serverType.getSelectedItem() != null) {
                    updateServer((String) serverType.getSelectedItem(), serverPath.getText(), AzurePlugin.cmpntFile);
                    if (customDownloadServer.isSelected() || uploadLocalServer.isSelected()) {
                        updateServerHome(serverPath.getText());
                    }
                    modified = true;
//                handlePageComplete();
                }
            }
        };
    }

    private ItemListener createServerPreferenceListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (serverCheckBox.isSelected()) {
                    srvChkBoxChecked(waRole, message("dlNtLblDir"));
                } else {
                    if (serverType.getSelectedItem() != null) {
                        // Remove server home settings
                        removeServerHomeSettings();
                        // Remove server setting
                        updateServer(null, null, AzurePlugin.cmpntFile);
                        srvChkBoxUnChecked();
                    }
                }
//                handlePageComplete();
            }
        };
    }

    private FocusListener createServerPathPreferenceListener() {
        return new FocusListener() {
            private String oldTxt = "";
            @Override
            public void focusGained(FocusEvent e) {
                oldTxt = serverPath.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                String path = serverPath.getText().trim();
                if (!(serverType.getSelectedItem() == null || ((String) serverType.getSelectedItem()).isEmpty())
                        && !serverPath.getText().equalsIgnoreCase(oldTxt)) {
                    File file = new File(path);
                    if (file.exists() && file.isDirectory()) {
                        // Server auto-detection
                        String serverName = WAEclipseHelperMethods.detectServer(file, WAHelper.getTemplateFile(AzureBundle.message("cmpntFileName")));
                        if (serverName != null) {
                            serverType.setSelectedItem(serverName);
                        } else {
                            String srvComboTxt = (String) serverType.getSelectedItem();
                            if (srvComboTxt != null && !srvComboTxt.isEmpty()) {
                                serverName = srvComboTxt;
                            }
                        }
                        updateServer(serverName, path, AzurePlugin.cmpntFile);
                    }
                }
                focusLostSrvText(path, message("dlNtLblDir"), message("dlgDlNtLblUrl"));
            }
        };
    }

    private FocusListener createJdkPathPreferenceListener() {
        return new FocusListener() {
            private String oldTxt = "";
            @Override
            public void focusGained(FocusEvent e) {
                oldTxt = jdkPath.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                String jdkPath = JdkServerPanel.this.jdkPath.getText();
                focusLostJdkText(jdkPath);
            }
        };
    }

    private ItemListener createJdkCheckBoxPreferenceListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                modified = true;
                if (jdkCheckBox.isSelected()) {
                    // populate third party JDKs whose status in not deprecated
                    jdkChkBoxChecked("");
                } else {
                    jdkChkBoxUnChecked();
                }
            }
        };
    }

    /** Sets the JDK.
     * @throws WindowsAzureInvalidProjectOperationException
     */
    private void handleJdkDirRemoval() throws WindowsAzureInvalidProjectOperationException {
        // deleting JDK entry from approot
        String oldJdkPath = waRole.getJDKSourcePath();
        if (oldJdkPath != null && !oldJdkPath.isEmpty() && !fileToDel.contains("jdk")) {
            fileToDel.add("jdk");
            WindowsAzureRoleComponent cmp = getPrevCmpnt(message("typeJdkDply"));
            if (cmp != null) {
                finalJdkPath = cmp.getImportPath();
            }
        }
    }

    /**
     * Method removes server home settings,
     * according to current package type.
     * Method will get called on the event of
     * check box uncheck.
     */
    private void removeServerHomeSettings() {
        try {
            WAServerConfUtilMethods.removeServerHomeSettings(waRole, waProjManager);
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("srvHomeErr"));
        }
    }

    /**
     * Method returns component object according to component type.
     * If component not present then returns NULL.
     * @param cmpntType
     * @return WindowsAzureRoleComponent
     */
    private WindowsAzureRoleComponent getPrevCmpnt(String cmpntType) {
        WindowsAzureRoleComponent cmp = null;
        try {
            cmp = WAServerConfUtilMethods.getPrevCmpnt(cmpntType, waRole);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cmpntSetErrTtl"), message("cmpntgetErrMsg"), e);
        }
        return cmp;
    }

    /**
     * Updates server settings when UI controls change.
     * @param newName
     * @param newPath
     * @param componentFile
     */
    private void updateServer(String newName, String newPath, File componentFile) {
        try {
            String oldName = waRole.getServerName();
            String oldPath = waRole.getServerSourcePath();
            String path = newPath;
			/*
			 * Trying to set server to same value,
			 * then don't do anything.
			 */
            if (newName != null && path != null && newName.equalsIgnoreCase(oldName) && path.equalsIgnoreCase(oldPath)) {
                handleEndpointSettings(newName);
                return;
            }
            // Remove old server from approot
            if (oldName != null && !fileToDel.contains("srv")) {
                fileToDel.add("srv");
                WindowsAzureRoleComponent cmp = getPrevCmpnt(message("typeSrvDply"));
                if (cmp != null) {
                    finalSrvPath = cmp.getImportPath();
                    finalImpMethod = cmp.getImportMethod();
                    finalAsName = cmp.getDeployName();
                }
            }
			/*
			 * Trying to set server with name only.
			 * Consider scenario where user selected server type using combo box
			 * without selecting server directory path
			 * i.e. server path text box is empty.
			 */
            if (path == null || path.isEmpty()) {
                path = message("dummySrvPath");
            }
            // Remove the current server if any
            waRole.setServer(null, message("dummySrvPath"), componentFile);
            // Add the new server if desired
			/*
			 * If both name and path are null
			 * that means we don't want to set server
			 * and old server gets removed in previous step only.
			 */
            if (newName != null && path != null) {
                handleEndpointSettings(newName);
                waRole.setServer(newName, path, componentFile);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("setSrvNmErrMsg"), e);
        }
    }

    private void handleEndpointSettings(String srvName) {
        try {
            String srvPriPort = WindowsAzureProjectManager.getHttpPort(srvName, AzurePlugin.cmpntFile);
            // Check server's private port already used on role
            int count = 0;
            WindowsAzureEndpoint endptWithPort = null;
            for (WindowsAzureEndpoint endpoint : waRole.getEndpoints()) {
                String priPort = endpoint.getPrivatePort();
                if (priPort != null && priPort.equalsIgnoreCase(srvPriPort)) {
                    count++;
                    endptWithPort = endpoint;
                }
            }
            if (count == 0) {
                // server's private port is not used
                WindowsAzureEndpoint sslEndpt = waRole.getSslOffloadingInternalEndpoint();
                WindowsAzureEndpoint stickyEndpt = waRole.getSessionAffinityInternalEndpoint();
                if (sslEndpt != null) {
                    sslEndpt.setPrivatePort(srvPriPort);
                } else if (stickyEndpt != null) {
                    stickyEndpt.setPrivatePort(srvPriPort);
                } else {
                    checkForHttpElseAddEndpt(srvPriPort);
                }
            } else if (count == 1 && endptWithPort.getEndPointType().equals(WindowsAzureEndpointType.InstanceInput)) {
                // one endpoint is using server's private port
                checkForHttpElseAddEndpt(srvPriPort);
            }
			/*
			 * If two endpoints of type Input and InstanceInput
			 * are using server's private port then don't do anything
			 */
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("errSrvPort"), e);
        }
    }

    private void checkForHttpElseAddEndpt(String srvPriPort) {
        try {
            WindowsAzureEndpoint httpEndpt = WAEclipseHelperMethods.findEndpointWithPubPortWithAuto(HTTP_PORT, waRole);
            if (httpEndpt != null) {
                httpEndpt.setPrivatePort(srvPriPort);
            } else {
                WindowsAzureRole httpRole = WAEclipseHelperMethods.findRoleWithEndpntPubPort(HTTP_PORT, waProjManager);
                if (httpRole != null) {
                    PluginUtil.displayWarningDialog(message("cmhLblSrvCnfg"), String.format(message("srvPortWarn"), httpRole.getName()));
                } else {
                    // create an endpoint
                    WAServerConfUtilMethods.addEndpt(srvPriPort, waRole);
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("errSrvPort"), e);
        }
    }

    /**
     * Method is used when focus is lost
     * from server directory text box.
     * @param srvPath
     * @param label
     * @param labelNext
     */
    public void focusLostSrvText(String srvPath, String label, String labelNext) {
        File file = new File(srvPath);
        if (customDownloadServer.isSelected() && !srvPath.isEmpty() && file.exists()) {
            String dirName = file.getName();
            lblDlNoteUrlSrv.setText(String.format(label, dirName));
        } else {
            lblDlNoteUrlSrv.setText(labelNext);
        }
    }

    private DocumentListener createJdkPathListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleUpdate();
            }

            private void handleUpdate() {
                modifyJdkText(waRole, message("dlNtLblDir"));
//                handlePageComplete();
            }
        };
    }

    private DocumentListener createJdkUrlListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleUpdate();
            }

            private void handleUpdate() {
                modified = true;
                if (uploadLocalJdk.isSelected()) {
//                    handlePageComplete();
                    // no need to do any checks if auto upload is selected
                    return;
                }
                isManualUpdate = false;
                String jdkPath = jdkUrl.getText().trim();
                modifyUrlText(jdkPath, storageAccountJdk);
                if (jdkPath.endsWith(".zip")) {
                    jdkPath = jdkPath.substring(0, jdkPath.indexOf(".zip"));
                }
                updateJDKHome(jdkPath);
                isManualUpdate = true;
//                handlePageComplete();
            }
        };
    }

    private DocumentListener createServerUrlListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleUpdate();
            }

            private void handleUpdate() {
                modified = true;
                // If auto upload is selected, no need to handle this case
                if (uploadLocalServer.isSelected()) {
//                    handlePageComplete();
                    return;
                }
                String url = serverUrl.getText().trim();
                isManualUpdate = false;
                modifyUrlText(url, storageAccountServer);
                /*
                 * update home directory for server accordingly
                 */
                if (url.endsWith(".zip")) {
                    url = url.substring(0, url.indexOf(".zip"));
                }
                String srvDirName = new File(url).getName();
                serverHomeDir.setText("%DEPLOYROOT%\\" + srvDirName);
                isManualUpdate = true;
//                handlePageComplete();
            }
        };
    }

    private ActionListener createCustomDownloadJdkListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modified = true;
                if (customDownloadJdk.isSelected()) {
                    jdkUrl.setText(getUrl(storageAccountJdk));
                    jdkDeployBtnSelected();
                    serverCheckBox.setEnabled(true);
                    setEnableServer(true);
                }
//                handlePageComplete();
                accepted = false;
            }
        };
    }

    private ItemListener createStorageAccountServerListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && isManualUpdate) {
                    updateServerDlURL();
                    updateServerHome(waRole);
//                    handlePageComplete()
                }
            }
        };
    }

    private ActionListener createServerPathListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifySrvText(waRole, message("dlNtLblDirSrv"));
//                handlePageComplete();
            }
        };
    }

    private ItemListener createThirdPartyJdkNameListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                modified = true;
                thirdPartyComboListener();
				/*
				 * If JDK name is changed by user then license
				 * has to be accepted again.
				 */
                String currentName = (String) thirdPartyJdkName.getSelectedItem();
                if (currentName == null || !currentName.equalsIgnoreCase(jdkPrevName)) {
                    accepted = false;
                    jdkPrevName = currentName;
                }
            }
        };
    }

    private ItemListener createStorageAccountJdkListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && isManualUpdate) {
                    modified = true;
                    updateJDKDlURL();
//                    handlePageComplete();
                }
            }
        };
    }

    private ActionListener createThirdPartyJdkListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modified = true;
                /*
				 * Check if third party radio button
				 * is already selected
				 * and user is selecting same radio button again
				 * then do not do any thing.
				 */
                if (!thirdPartyJdkName.isEnabled()) {
                    thirdPartyJdkBtnSelected(message("dlNtLblDir"));
                    jdkPrevName = (String) thirdPartyJdkName.getSelectedItem();
                    serverCheckBox.setEnabled(true);
                    accepted = false;
                }
            }
        };
    }

    public Map<String, String> getDeployPageValues() {
        Map<String, String> values = new HashMap<String, String>();
        // JDK
        values.put("jdkChecked", String.valueOf(jdkCheckBox.isSelected()));
        values.put("jdkLoc", jdkPath.getText());
        // JDK download group
        values.put("jdkDwnldChecked", String.valueOf(customDownloadJdk.isSelected()));
        values.put("jdkAutoDwnldChecked", String.valueOf(uploadLocalJdk.isSelected()));
        values.put("jdkThrdPartyChecked", String.valueOf(thirdPartyJdk.isSelected()));
        values.put("jdkName", thirdPartyJdkName.getSelectedItem() == null ? "" : (String) thirdPartyJdkName.getSelectedItem());
        values.put("jdkUrl", jdkUrl.getText());
        values.put("jdkKey", AzureWizardModel.getAccessKey(storageAccountJdk));
        values.put("javaHome", javaHome.getText());
        // Server
        values.put("serChecked", String.valueOf(serverCheckBox.isSelected()));
        values.put("servername", (String) serverType.getSelectedItem());
        values.put("serLoc", serverPath.getText());
        values.put("tempFile", WAHelper.getTemplateFile(message("cmpntFileName")));
        // Server download group
        values.put("srvDwnldChecked", String.valueOf(customDownloadServer.isSelected()));
        values.put("srvAutoDwnldChecked", String.valueOf(uploadLocalServer.isSelected()));
        values.put("srvThrdPartyChecked", String.valueOf(thrdPrtSrvBtn.isSelected()));
        values.put("srvThrdPartyName", thrdPrtSrvCmb.getSelectedItem() == null ? "" : (String) thrdPrtSrvCmb.getSelectedItem());
        values.put("srvThrdAltSrc", getServerCloudAltSource());
        values.put("srvUrl", serverUrl.getText());
        values.put("srvKey", AzureWizardModel.getAccessKey(storageAccountServer));
        values.put("srvHome", serverHomeDir.getText());
        return values;
    }

    private ItemListener createJdkCheckBoxListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                modified = true;
                try {
                    isManualUpdate = false;
                    if (jdkCheckBox.isSelected()) {
                        try {
                            // populate third party JDKs whose status in not deprecated
                            jdkChkBoxChecked("");
                        } catch (Exception e1) {
                            log(message("error"), e1);
                        }
                    } else {
                        jdkChkBoxUnChecked();
                    }
                } finally {
                    isManualUpdate = true;
                }
//        handlePageComplete();

            }
        };
    }

    private Action createStorageAccountsAction(JComboBox combo, String tabName) {
        return new AccountsAction(combo, tabName);
    }

    //    @Override
    public ValidationInfo doValidate() {
        boolean isJdkValid = false;
        // JDK emulator group
        if (jdkCheckBox.isSelected()) {
            if (jdkPath.getText().isEmpty()) {
                return createValidationInfo(message("jdkPathErrMsg"), jdkPath);
            } else {
                File file = new File(jdkPath.getText());
                if (!file.exists()) {
                    return createValidationInfo(message("dplWrngJdkMsg"), jdkPath);
                }
            }
        }
        // JDK download group
        // cloud radio button selected
        if (customDownloadJdk.isSelected()) {
            // Validate JDK URL
            String url = jdkUrl.getText();
            if (url.isEmpty()) {
                return createValidationInfo(message("dlgDlUrlErrMsg"), jdkUrl);
            } else {
                try {
                    new URL(url);
                    if (WAEclipseHelperMethods.isBlobStorageUrl(url)) {
                        String jdkHome = javaHome.getText().trim();
                        if (jdkHome.isEmpty()) {
                            return createValidationInfo(message("jvHomeErMsg"), javaHome);
                        } else {
                            isJdkValid = true;
                        }
                    } else {
                        return createValidationInfo(message("dlgDlUrlErrMsg"), jdkUrl);
                    }
                } catch (MalformedURLException e) {
                    return createValidationInfo(message("dlgDlUrlErrMsg"), jdkUrl);
                }
            }
        }
        // No Validation needed if auto upload or
        // third party JDK is selected
        // local radio button selected
        else {
            isJdkValid = true;
        }
        // Server
        if (isJdkValid && serverCheckBox.isSelected()) {
            if (serverType.getSelectedItem() == null) {
                return createValidationInfo(message("dplEmtSerMsg"), serverType);
            } else if (serverPath.getText().isEmpty()) {
                return createValidationInfo(message("dplEmtSerPtMsg"), serverPath);
            } else if (!(new File(serverPath.getText()).exists())) {
                return createValidationInfo(message("dplWrngSerMsg"), serverPath);
            } else {
                // Server download group
                if (customDownloadServer.isSelected()) {
                    String srvUrl = serverUrl.getText();
                    if (srvUrl.isEmpty()) {
                        return createValidationInfo(message("dlgDlUrlErrMsg"), serverUrl);
                    } else {
                        try {
                            // Validate Server URL
                            new URL(srvUrl);
                            if (WAEclipseHelperMethods.isBlobStorageUrl(srvUrl)) {
                                String srvHome = serverHomeDir.getText().trim();
                                if (srvHome.isEmpty()) {
                                    return createValidationInfo(message("srvHomeErMsg"), serverHomeDir);
                                }
                            } else {
                                return createValidationInfo(message("dlgDlUrlErrMsg"), serverUrl);
                            }
                        } catch (MalformedURLException e) {
                            return createValidationInfo(message("dlgDlUrlErrMsg"), serverUrl);
                        }
                    }
                }
                // No validations if auto upload Server is selected
                // local radio button selected
            }
        }
//        myModel.getCurrentNavigationState().FINISH.setEnabled(true);
//        myModel.getCurrentNavigationState().NEXT.setEnabled(true);
        return null;
    }

    private ValidationInfo createValidationInfo(String message, JComponent component) {
//        myModel.getCurrentNavigationState().FINISH.setEnabled(false);
//        myModel.getCurrentNavigationState().NEXT.setEnabled(false);
        return new ValidationInfo(message, component);
    }

    private void createUIComponents() {
        applicationsTab = new ApplicationsTab();
        applicationsSettings = applicationsTab.getPanel();
    }

    private class AccountsAction extends AbstractAction {
        private JComboBox myCombo;
        private String myTabName;

        private AccountsAction(JComboBox myCombo, String tabName) {
            super("Accounts...");
            this.myCombo = myCombo;
            this.myTabName = tabName;
        }

        public void actionPerformed(ActionEvent e) {
            final DefaultDialogWrapper storageAccountDialog = new DefaultDialogWrapper(project, new StorageAccountPanel(project));
            storageAccountDialog.show();
                /*
                 * Update data in every case.
		         * No need to check which button (OK/Cancel)
		         * has been pressed as change is permanent
		         * even though user presses cancel
		         * according to functionality.
		        */
                /*
		         * store old value which was selected
		         * previously so that we can populate
		         * the same later.
		         */
            String cmbName = (String) myCombo.getSelectedItem();
            String accPgName = storageAccountDialog.getSelectedValue();
            String finalNameToSet;
			/*
			 * If row selected on preference page.
			 * set combo box to it always.
			 * Else keep combo box's previous value
			 * as it is.
			 */
            if (accPgName != JdkSrvConfig.NONE_TXT && accPgName != JdkSrvConfig.AUTO_TXT) {
                finalNameToSet = accPgName;
            } else {
                finalNameToSet = cmbName;
            }
            // update storage account combo box
            myCombo = JdkSrvConfig.populateStrgAccComboBox(finalNameToSet, myCombo, myTabName, JdkSrvConfig.AUTO_TXT.equals(finalNameToSet));
        }
    }

    private ActionListener createUploadLocalJdkListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modified = true;
                // auto upload radio button selected
                try {
                    configureAutoUploadJDKSettings(message("dlNtLblDir"));
                } catch (Exception ex) {
                    log(ex.getMessage());
                }
                if (!jdkCheckBox.isSelected()) {
                    setEnableServer(false);
                    setEnableDlGrpSrv(false, false);
                }
//                handlePageComplete();
                accepted = false;
            }
        };
    }

    private ActionListener createUploadLocalJdkPreferenceListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modified = true;
                // auto upload radio button selected
                try {
                    configureAutoUploadJDKSettings(message("dlNtLblDir"));
                } catch (Exception ex) {
                    log(ex.getMessage());
                }
                if (!jdkCheckBox.isSelected()) {
                    setEnableServer(false);
                    setEnableDlGrpSrv(false, false);
                    try {
                        if (waRole.getServerName() != null && waRole.getServerSourcePath() != null) {
                            removeServerHomeSettings();
                        }
                        // Remove server setting
                        updateServer(null, null, AzurePlugin.cmpntFile);
                    } catch (WindowsAzureInvalidProjectOperationException ex) {
                        log(message("error", ex));
                    }
                }
//                handlePageComplete();
                accepted = false;
            }
        };
    }

    private ItemListener createServerListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (serverCheckBox.isSelected()) {
                    srvChkBoxChecked(waRole, message("dlNtLblDirSrv"));
                } else {
                    srvChkBoxUnChecked();
                }
//                handlePageComplete(); todo:
            }
        };
    }

    private ActionListener createUploadLocalServerListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (uploadLocalServer.isSelected()) {
                    // server auto upload radio button selected
                    configureAutoUploadServerSettings(waRole, message("dlNtLblDirSrv"));
                }
//                handlePageComplete();
            }
        };
    }

    private ActionListener createCustomDownloadServerListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (customDownloadServer.isSelected()) {
                    srvDeployBtnSelected(waRole, message("dlNtLblDirSrv"));
                    /*
					 * server auto upload radio button unselected
					 * and deploy button selected.
					 */
                    serverUrl.setText(getUrl(storageAccountServer));
                }
//                handlePageComplete();
            }
        };
    }

    /**
     * Method is used when JDK check box is checked.
     * @return
     */
    public String jdkChkBoxChecked(String depJdkName) {
        // Pre-populate with auto-discovered JDK if any
        String jdkDefaultDir = null;
        try {
            jdkDefaultDir = WAEclipseHelperMethods.jdkDefaultDirectory(waRole.getJDKSourcePath());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(message("error"), e);
        }
        jdkPath.setText(jdkDefaultDir);
        setEnableJDK(true);
        serverCheckBox.setEnabled(true);
        if (uploadLocalJdk.isSelected()) {
            configureAutoUploadJDKSettings(message("dlNtLblDir"));
        } else if (thirdPartyJdk.isSelected()) {
            thirdPartyJdkBtnSelected(message("dlNtLblDir"));
        } else {
            jdkDeployBtnSelected();
        }
        return jdkDefaultDir;
    }

    /**
     * Method is used when JDK check box is unchecked.
     */
    public void jdkChkBoxUnChecked() {
        serverCheckBox.setSelected(false);
        setEnableJDK(false);
        if (uploadLocalJdk.isSelected()) {
            setEnableServer(false);
            setEnableDlGrpSrv(false, false);
            configureAutoUploadJDKSettings(message("dlNtLblDir"));
        } else {
            // incase of third party and custom download just change text
            uploadLocalJdk.setText(message("noJdkDplyLbl"));
        }
    }

    /**
     * Method is used when JDK directory text is modified.
     * @param role
     * @param label
     */
    public void modifyJdkText(WindowsAzureRole role, String label) {
        // update only for auto upload not for third party JDK.
        if (uploadLocalJdk.isSelected()) {
            jdkUrl.setText(cmbBoxListener(storageAccountJdk, jdkUrl.getText(), "JDK"));
            updateJDKDlNote(label);
            updateJDKHome(jdkPath.getText());
        }
    }

    /**
     * Enable or disable components of JDK group according to status.
     * @param status
     */
    public void setEnableJDK(boolean status) {
        if (isManualUpdate) {
            jdkCheckBox.setSelected(status);
        }
        jdkPath.setEnabled(status);
        if (!status) {
            jdkPath.setText("");
        }
    }

    /**
     * Method decides whether to
     * show third party JDK names or not.
     */
    public void showThirdPartyJdkNames(String depJdkName) {
        try {
            String[] thrdPrtJdkArr = WindowsAzureProjectManager.getThirdPartyJdkNames(AzurePlugin.cmpntFile, depJdkName);
            // check at least one element is present
            if (thrdPrtJdkArr.length >= 1) {
                thirdPartyJdkName.setModel(new DefaultComboBoxModel(thrdPrtJdkArr));
            }
        } catch (WindowsAzureInvalidProjectOperationException ex) {
            log(ex.getMessage(), ex);
        }
    }

    /**
     * Listener for third party JDK name combo box.
     * Updates URL and java home.
     */
    public void thirdPartyComboListener() {
        updateJDKDlURL();
        try {
            javaHome.setText(WindowsAzureProjectManager.getCloudValue((String) thirdPartyJdkName.getSelectedItem(), AzurePlugin.cmpntFile));
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(e.getMessage());
        }
    }

    /**
     * Method is used when focus is lost
     * from JDK directory text box.
     * @param jdkPath
     */
    public void focusLostJdkText(String jdkPath) {
        // Update note below JDK URL text box
        File file = new File(jdkPath);
        if (customDownloadJdk.isSelected() && !jdkPath.isEmpty() && file.exists()) {
            String dirName = file.getName();
            lblDlNoteUrl.setText(String.format(message("dlNtLblDir"), dirName));
        } else {
            lblDlNoteUrl.setText(message("dlgDlNtLblUrl"));
        }
    }

    /**
     * Method is used when JDK's deploy from custom download
     * radio button is selected.
     */
    public void jdkDeployBtnSelected() {
        // deploy radio button selected
        setEnableDlGrp(true, false);
        updateJDKDlNote(message("dlNtLblDir"));
        updateJDKHome(jdkPath.getText());
        enableThirdPartyJdkCombo(false);
    }

    /**
     * Method is used when third party JDK
     * radio button is selected.
     *
     * @param label
     */
    public void thirdPartyJdkBtnSelected(String label) {
        setEnableDlGrp(true, true);
        enableThirdPartyJdkCombo(true);
        thirdPartyComboListener();
        updateJDKDlNote(label);
    }

    /**
     * Method is used when JDK URL text is modified.
     */
    public void modifyUrlText(String url, JComboBox storageAccountCombo) {
		/*
		 * Extract storage account name
		 * and service endpoint from URL
		 * entered by user.
		 */
        String nameInUrl = StorageRegistryUtilMethods.getAccNameFromUrl(url);
        urlModifyListner(url, nameInUrl, storageAccountCombo);
    }


    /**
     * Enable or disable components of
     * JDK download group according to status.
     * @param status
     */
    public void setEnableDlGrp(boolean status, boolean applyAutoUlParams) {
        if (jdkCheckBox.isSelected()) {
            uploadLocalJdk.setText(message("autoDlJdkCldRdBtnLbl"));
        } else {
            uploadLocalJdk.setText(message("noJdkDplyLbl"));
            showThirdPartyJdkNames("");
        }
        // URL
        jdkUrlLabel.setEnabled(status);
        lblDlNoteUrl.setEnabled(status);
        jdkUrl.setEnabled(status);
        // storage account combo
        storageAccountJdk.setEnabled(status);
        storageAccountJdkLabel.setEnabled(status);
        // labels
        lblJavaHome.setEnabled(status);
        lblNoteJavaHome.setEnabled(status);

        if (status && applyAutoUlParams) {
            // Always disable and auto-generate JDK url and derive Java home.
            jdkUrl.setEditable(false);
            javaHome.setEnabled(!status);
        } else {
            jdkUrl.setEditable(true);
            javaHome.setEnabled(status);
        }
        if (!status) {
            jdkUrl.setText("");
            storageAccountJdk.removeAllItems();
            javaHome.setText("");
            lblDlNoteUrl.setText(message("dlgDlNtLblUrl"));
            thirdPartyJdkName.setEnabled(false);
        } else {
            JdkSrvConfig.populateStrgAccComboBox((String) storageAccountJdk.getSelectedItem(), storageAccountJdk, JdkSrvConfig.JDK_TXT, false);
        }
    }


    /**
     * Method is used when Server check box is checked.
     */
    public void srvChkBoxChecked(WindowsAzureRole role, String label) {
        enableSrvRdButtons();
        setEnableServer(true);
        serverType.setModel(new DefaultComboBoxModel(JdkSrvConfigListener.getServerList()));
        serverType.setSelectedItem(null);
        configureAutoUploadServerSettings(role, label);
    }

    /**
     * Method is used when Server check box is unchecked.
     */
    public void srvChkBoxUnChecked() {
        setEnableServer(false);
        setEnableDlGrpSrv(false, false);
        serverCheckBox.setEnabled(true);
    }

    /**
     * Listener for server browse button it is used in file system button.
     * It will open the file system location.
     */
    protected void serBrowseBtnListener() {
        utilSerBrowseBtnListener(message("dlNtLblDirSrv"));
//        handlePageComplete();
    }

    /**
     * Method is used when Server directory text is modified.
     * @param role
     * @param label
     */
    public void modifySrvText(WindowsAzureRole role, String label) {
        if (uploadLocalServer.isSelected()) {
            serverUrl.setText(cmbBoxListener(storageAccountServer, serverUrl.getText(), JdkSrvConfig.SRV_TXT));
            updateSrvDlNote(label);
            updateServerHome(role);
        }
    }

    private void enableSrvRdButtons() {
        uploadLocalServer.setEnabled(true);
        customDownloadServer.setEnabled(true);
        uploadLocalServer.setSelected(true);
    }

    /**
     * Enable or disable components of Server group according to status.
     * @param status
     */
    public void setEnableServer(boolean status) {
        serverCheckBox.setEnabled(status);
        serverType.setEnabled(status);
        lblSelect.setEnabled(status);
        serverUrl.setEnabled(status);
        uploadLocalServer.setEnabled(status);
        serverPath.setEnabled(status);
//        tblApp.setEnabled(status);
//        btnAdd.setEnabled(status);
        if (!status) {
//            serCheckBtn.setSelection(status);
            serverType.removeAllItems();
            serverPath.setText("");
//            btnRemove.setEnabled(status);
        }
    }

    /**
     * Enable or disable components of
     * Server download group according to status.
     * @param status
     */
    public void setEnableDlGrpSrv(boolean status, boolean applyAutoUlParams) {
        customDownloadServer.setEnabled(status);
        uploadLocalServer.setEnabled(status);
        storageAccountServer.setEnabled(status);
        lblKeySrv.setEnabled(status);
        lblUrlSrv.setEnabled(status);
        lblDlNoteUrlSrv.setEnabled(status);
        lblHomeDir.setEnabled(status);
        serverUrl.setEnabled(status);
        lblNoteHomeDir.setEnabled(status);
        if (status && applyAutoUlParams) {
            serverUrl.setEditable(false);
            serverHomeDir.setEnabled(!status);
        } else {
            serverUrl.setEditable(true);
            serverHomeDir.setEnabled(status);
        }
        if (!status) {
            customDownloadServer.setSelected(false);
            uploadLocalServer.setSelected(false);
            serverUrl.setText("");
            storageAccountServer.removeAllItems();
            serverHomeDir.setText("");
            lblDlNoteUrlSrv.setText(message("dlNtLblUrlSrv"));
        } else {
            JdkSrvConfig.populateStrgAccComboBox((String) storageAccountServer.getSelectedItem(), storageAccountServer, JdkSrvConfig.SRV_TXT, false);
        }
    }

    /**
     * Method used when server auto upload radio
     * button selected.
     * @param role
     * @param label
     */
    public void configureAutoUploadServerSettings(
            WindowsAzureRole role, String label) {
        setEnableDlGrpSrv(true, true);
        populateDefaultStrgAccForSrvAuto();
        updateServerDlURL();
        updateSrvDlNote(label);
        updateServerHome(role);
    }

    /**
     * Method used when JDK auto upload/no JDK deployment
     * radio button selected.
     * @param label
     */
    public void configureAutoUploadJDKSettings(String label) {
        if (jdkCheckBox.isSelected()) {
            setEnableDlGrp(true, true);
            updateJDKDlURL();
            updateJDKDlNote(label);
            updateJDKHome(jdkPath.getText());
            enableThirdPartyJdkCombo(false);
        } else {
            setEnableDlGrp(false, false);
        }
    }

    /**
     * Enable or disable third party JDK
     * related components.
     * @param status
     */
    public void enableThirdPartyJdkCombo(Boolean status) {
        thirdPartyJdkName.setEnabled(status);
        thirdPartyJdk.setSelected(status);
    }

    /**
     * Method to update JDK cloud URL.
     * Will get updated as per storage account
     * combo box and radio button selection.
     */
    public void updateJDKDlURL()/* throws Exception*/ {
        if (isSASelectedForJDK()) {
            jdkUrl.setText(cmbBoxListener(storageAccountJdk, jdkUrl.getText(), JdkSrvConfig.JDK_TXT));
        } else if (uploadLocalJdk.isSelected()) {
            jdkUrl.setText("");
        }
    }

    /**
     * Method to update server cloud URL.
     * Will get updated as per storage account
     * combo box and radio button selection.
     */
    public void updateServerDlURL() {
        if (isSASelectedForSrv()) {
            serverUrl.setText(cmbBoxListener(storageAccountServer, serverUrl.getText(), JdkSrvConfig.SRV_TXT));
        } else if (!customDownloadServer.isSelected()) {
            serverUrl.setText("");
        }
    }

    /**
     * Utility method to update note below text box for JDK.
     */
    public void updateJDKDlNote(String label) {
        // Update note below URL text box
        String jdkPath = this.jdkPath.getText();
        File file = new File(jdkPath);
        if (!jdkPath.isEmpty() && file.exists()) {
            String dirName = file.getName();
            lblDlNoteUrl.setText(String.format(label, dirName));
        }
    }

    /**
     * Utility method to update note below text box for Server.
     */
    public void updateSrvDlNote(String label) {
        // Update note below server URL text box
        String srvPath = serverPath.getText();
        File file = new File(srvPath);
        if (!srvPath.isEmpty() && file.exists()) {
            String dirName = file.getName();
            lblDlNoteUrlSrv.setText(String.format(label, dirName));
        }
    }

    /**
     * Utility method to update java home value.
     */
    public void updateJDKHome(String jdkPath) {
        try {
            String jdkHome = waRole.constructJdkHome(jdkPath, AzurePlugin.cmpntFile);
            javaHome.setText(jdkHome);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(e.getMessage());
        }
    }

    /**
     * Utility method to update server home value.
     */
    public void updateServerHome(WindowsAzureRole role) {
        // set server home directory text box value
        String srvPath = serverPath.getText();
        try {
            String srvHome = role.constructServerHome((String) serverType.getSelectedItem(), srvPath, AzurePlugin.cmpntFile);
            serverHomeDir.setText(srvHome);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(e.getMessage());
        }
    }

    /**
     * Server directory browse button listener.
     * @param label
     * @return
     */
    public void utilSerBrowseBtnListener(String label) {
        try {
            File serverDir = new File(serverPath.getText());
            // Update note below server URL text box
            if (customDownloadServer.isSelected()) {
                String dirName = serverDir.getName();
                lblDlNoteUrlSrv.setText(String.format(label, dirName));
            }
            // Auto detect server family
            String serverName = WAEclipseHelperMethods.detectServer(serverDir, WAHelper.getTemplateFile(AzureBundle.message("cmpntFileName")));
            isManualUpdate = false;
            if (serverName != null && !serverName.isEmpty()) {
                this.serverType.setSelectedItem(serverName);
            } else {
                serverType.setSelectedItem(null);
            }
        } catch (Exception e) {
            log(e.getMessage(), e);
        } finally {
            isManualUpdate = true;
        }
    }

    /**
     * API to determine if storage account is selected or not in JDK tab
     * @return true if storage account is selected in JDK tab else false.
     */
    public boolean isSASelectedForJDK() {
        return !JdkSrvConfig.NONE_TXT.equals(storageAccountJdk.getSelectedItem());
    }

    /**
     * API to determine if storage account is selected or not in Server tab
     * @return true if storage account is selected in Server tab else false.
     */
    public boolean isSASelectedForSrv() {
        return !JdkSrvConfig.NONE_TXT.equals(storageAccountServer.getSelectedItem());
    }

    /**
     * Method returns URL from storage registry
     * according to account name selected in combo box.
     */
    public static String getUrl(JComboBox combo) {
        int index = combo.getSelectedIndex();
        String url = "";
        if (index > 0) {
            url = StorageAccountRegistry.getStrgList().get(index - 1).getStrgUrl();
        }
        return url;
    }

    /**
     * Method will check if JDK storage account combo box
     * is set to valid value other than none
     * then while selecting auto upload option
     * for server, it will populate
     * storage account name selected for JDK
     * in server combo box.
     */
    public void populateDefaultStrgAccForSrvAuto() {
        int jdkIndex = storageAccountJdk.getSelectedIndex();
        int srvIndex = storageAccountServer.getSelectedIndex();
		/*
		 * JDK storage account combo box is enabled
		 * and account selected is other than (none).
		 * Also check storage account for server
		 * is not specified already then only change.
		 */
        if (jdkIndex > 0 && !(srvIndex > 0)) {
            storageAccountServer.setSelectedIndex(jdkIndex);
        }
    }

    /**
     * Listener for URL text box's text change.
     * @param url
     * @param nameInUrl
     * @param combo
     * @return
     */
    public void urlModifyListner(String url, String nameInUrl, JComboBox combo) {
        String endpoint = StorageRegistryUtilMethods.getServiceEndpoint(url);
        String accNameToSet = JdkSrvConfig.accNames[0];
        if (nameInUrl != null && !nameInUrl.isEmpty() && endpoint != null) {
            // check storage account name present in list
            if (Arrays.asList(JdkSrvConfig.accNames).contains(nameInUrl)) {
				/*
				 * check endpoint of storage account from list
				 * and from URL matches then
				 * only select storage account otherwise select none.
				 */
                int index = Arrays.asList(JdkSrvConfig.accNames).indexOf(nameInUrl);
                String endpointInReg = StorageRegistryUtilMethods.getServiceEndpoint(StorageAccountRegistry.getStrgList().get(index - 1).getStrgUrl());
                if (endpoint.equalsIgnoreCase(endpointInReg)) {
                    accNameToSet = nameInUrl;
                }
            } else if (StorageRegistryUtilMethods.isDuplicatePresent()) {
				/*
				 * If accounts with same name but
				 * different service URL exists
				 * then check concatenation of account name
				 * and service endpoint exists in list.
				 */
                String accAndUrl = StorageRegistryUtilMethods.getAccNmSrvcUrlToDisplay(nameInUrl, endpoint);
                if (Arrays.asList(JdkSrvConfig.accNames).contains(accAndUrl)) {
                    accNameToSet = accAndUrl;
                }
            }
        }
        combo.setSelectedItem(accNameToSet);
    }

    /**
     * Listener for storage account combo box.
     * @param combo
     * @param urlTxt
     * @param tabControl
     * @return
     */
    public String cmbBoxListener(JComboBox combo, String urlTxt, String tabControl) {
        int index = combo.getSelectedIndex();
        String url = urlTxt.trim();
        // check value is not none and auto.
        if (index > 0) {
            String newUrl = StorageAccountRegistry.getStrgList().get(index - 1).getStrgUrl();

            // For JDK tab and auto upload option selected
            if (tabControl != null && JdkSrvConfig.JDK_TXT.equals(tabControl)) {
                if (uploadLocalJdk.isSelected()) {
                    urlTxt = JdkSrvConfigUtilMethods.prepareCloudBlobURL(jdkPath.getText(), newUrl);
                    return urlTxt;
                } else if (thirdPartyJdk.isSelected()) {
                    try {
                        urlTxt = JdkSrvConfigUtilMethods.prepareUrlForThirdPartyJdk(thirdPartyJdkName.getSelectedItem().toString(), newUrl, AzurePlugin.cmpntFile);
                        return urlTxt;
                    } catch (Exception e) {
                        log(e.getMessage());
                        return "";
                    }
                }
            }

            // For Server and auto upload option selected
            if (tabControl != null && JdkSrvConfig.SRV_TXT.equals(tabControl) && uploadLocalServer.isSelected()) {
                urlTxt = JdkSrvConfigUtilMethods.prepareCloudBlobURL(serverPath.getText(), newUrl);
                return urlTxt;
            }
			/*
			 * If URL is blank and new storage account selected
			 * then auto generate with storage accounts URL.
			 */
            if (url.isEmpty()) {
                urlTxt = newUrl;
            } else {
				/*
				 * If storage account in combo box and URL
				 * are in sync then update
				 * corresponding portion of the URL
				 * with the URI of the newly selected storage account
				 * (leaving the container and blob name unchanged.
				 */
                String oldVal = StorageRegistryUtilMethods.getSubStrAccNmSrvcUrlFrmUrl(url);
                String newVal = StorageRegistryUtilMethods.getSubStrAccNmSrvcUrlFrmUrl(newUrl);
                urlTxt = url.replaceFirst(oldVal, newVal);
            }
        } else if (index == 0) {
            // index = 0 means none or auto is selected
            // For JDK tab and auto upload option selected
            if (tabControl != null && JdkSrvConfig.JDK_TXT.equals(tabControl) && (uploadLocalJdk.isSelected() || thirdPartyJdk.isSelected())) {
                urlTxt = JdkSrvConfig.AUTO_TXT;
                return urlTxt;
            }
            // For Server and auto upload option selected
            if (tabControl != null && JdkSrvConfig.SRV_TXT.equals(tabControl) && uploadLocalServer.isSelected()) {
                urlTxt = JdkSrvConfig.AUTO_TXT;
                return urlTxt;
            }
        }
        return urlTxt;
    }

    /**
     * Method is used when server's deploy from download
     * radio button is selected.
     * @param role
     * @param label
     */
    public void srvDeployBtnSelected(WindowsAzureRole role, String label) {
        // server deploy radio button selected
        setEnableDlGrpSrv(true, false);
        updateSrvDlNote(label);
        updateServerHome(role);
    }

    public boolean okToLeave() throws ConfigurationException {
        boolean okToProceed = true;
        ValidationInfo validationInfo = doValidate();
        if (validationInfo != null) {
            throw new ConfigurationException(validationInfo.message);
        }
        if (jdkCheckBox.isSelected()) {
            /*
			 * Check if third party JDK is selected
			 * then license is accepted or not.
			 */
            if (createAccLicenseAggDlg()) {
                okToProceed = configureJdkCloudDeployment();
            } else {
                okToProceed = false;
            }
        }

        if (okToProceed && serverCheckBox.isSelected()) {
            String srvUrl = serverUrl.getText().trim();
            if (uploadLocalServer.isSelected() && srvUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                srvUrl = AUTO;
            }
            okToProceed = configureSrvCloudDeployment(srvUrl, serverHomeDir.getText().trim());
        }
        return okToProceed;
    }

    public boolean createAccLicenseAggDlg() {
        if (!accepted && thirdPartyJdk.isSelected()) {
            String jdkName = (String) thirdPartyJdkName.getSelectedItem();
            StringBuilder sb = new StringBuilder("<html>").append(String.format(message("aggMsg"), jdkName));
            String url = "";
            try {
                url = WindowsAzureProjectManager.getLicenseUrl(jdkName, AzurePlugin.cmpntFile);
            } catch (WindowsAzureInvalidProjectOperationException e) {
                log(e.getMessage(), e);
            }
            sb.append(String.format(message("aggLnk"), url, url)).append("</html>");
            boolean result = Messages.showYesNoDialog(project, sb.toString(), message("aggTtl"), message("acptBtn"), "Cancel", null) == Messages.YES;
            accepted = result;
            return result;
        } else {
            return true;
        }
    }

    /**
     * Method configures cloud deployment for JDK
     * by saving URL, key and cloud method.
     * @return
     */
    private boolean configureJdkCloudDeployment() {
        boolean isValid = true;
        String jdkPath = this.jdkPath.getText().trim();
        String jdkUrl = this.jdkUrl.getText().trim();
        String javaHome = this.javaHome.getText().trim();
        String jdkName = (String) thirdPartyJdkName.getSelectedItem();
        try {
            handleJdkDirRemoval();

            WAServerConfUtilMethods.removeJavaHomeSettings(waRole, waProjManager);
            waRole.setJDKCloudName(null);
            waRole.setJDKSourcePath(null, AzurePlugin.cmpntFile, "");

            if (!(!jdkCheckBox.isSelected() && uploadLocalJdk.isSelected())) {
                if (thirdPartyJdk.isSelected()) {
                    waRole.setJDKSourcePath(jdkPath, AzurePlugin.cmpntFile, jdkName);
                } else {
                    waRole.setJDKSourcePath(jdkPath, AzurePlugin.cmpntFile, "");
                }
                // JDK download group
                // By default auto upload will be selected.
                if (uploadLocalJdk.isSelected() || thirdPartyJdk.isSelected()) {
                    if (jdkUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                        jdkUrl = AUTO;
                    }
                    if (thirdPartyJdk.isSelected()) {
                        waRole.setJDKCloudName(jdkName);
                    }
                    waRole.setJDKCloudUploadMode(WARoleComponentCloudUploadMode.auto);
                }
                waRole.setJDKCloudURL(jdkUrl);
                waRole.setJDKCloudKey(AzureWizardModel.getAccessKey(storageAccountJdk));
                updateJavaHome(javaHome);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            isValid = false;
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("urlKeySetErrMsg"));
        }
        return isValid;
    }

    /**
     * Method configures cloud deployment for server
     * by saving URL, key and cloud method.
     * @param srvUrl
     * @param srvHome
     * @return
     */
    private boolean configureSrvCloudDeployment(String srvUrl, String srvHome) {
        boolean isValid = true;
        try {
            waRole.setServerCloudURL(srvUrl);
            updateServerHome(srvHome);
            waRole.setServerCloudKey(AzureWizardModel.getAccessKey(storageAccountServer));
            if (uploadLocalServer.isSelected()) {
                waRole.setServerCloudUploadMode(WARoleComponentCloudUploadMode.auto);
            } else {
                waRole.setServerCloudUploadMode(null);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            isValid = false;
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("urlKeySetErMsgSrv"));
        }
        return isValid;
    }

    /**
     * Method updates java home,
     * according to current package type.
     * Method will get called when user click
     * on OK button or tries to navigate to other page.
     * @param javaHome
     */
    private void updateJavaHome(String javaHome) {
        try {
            WAServerConfUtilMethods.updateJavaHome(javaHome, waRole, waProjManager, jdkPath.getText().trim(), AzurePlugin.cmpntFile);
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("jvHomeErr"));
        }
    }

    /**
     * Method updates server home,
     * according to current package type.
     * Method will get called when user click
     * on OK button or tries to navigate to other page.
     * @param srvHome
     */
    private void updateServerHome(String srvHome) {
        try {
            WAServerConfUtilMethods.updateServerHome(srvHome, waRole, waProjManager, serverPath.getText().trim(), (String) serverType.getSelectedItem(),
                    AzurePlugin.cmpntFile);
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("srvHomeErr"));
        }
    }

    /**
     * Gives server name selected by user.
     *
     * @return serverName
     */
    public String getServerName() {
        String serverName = "";
        if (thrdPrtSrvBtn.isSelected()) {
            try {
                serverName = WindowsAzureProjectManager.getServerNameUsingThirdPartyServerName((String) thrdPrtSrvCmb.getSelectedItem(), AzurePlugin.cmpntFile);
            } catch (WindowsAzureInvalidProjectOperationException e) {
                serverName = "";
            }
        } else {
            serverName = (String) serverType.getSelectedItem();
        }
        return serverName;
    }

    public String getServerCloudAltSource() {
        String url = "";
        if (thrdPrtSrvBtn.isSelected()) {
            try {
                url = WindowsAzureProjectManager.getThirdPartyServerCloudAltSrc((String) thrdPrtSrvCmb.getSelectedItem(), AzurePlugin.cmpntFile);
            } catch (WindowsAzureInvalidProjectOperationException e) {
                url = "";
            }
        }
        return url;
    }

    public void apply() throws ConfigurationException {
        if (!okToLeave()) {
            throw new ConfigurationException(message("error"));
        }
        boolean okToProceed = false;
        boolean isJdkValid = true;
        boolean isSrvValid = true;
        // Validation for JDK
        if (jdkCheckBox.isSelected()) {
            if (jdkPath.getText().isEmpty()) {
                isJdkValid = false;
                throw new ConfigurationException(message("jdkPathErrMsg"), message("jdkPathErrTtl"));
            } else {
                File file = new File(jdkPath.getText());
                if (!file.exists() || !file.isDirectory()) {
                    throw new ConfigurationException(message("jdkPathErrMsg"), message("jdkPathErrTtl"));
                }
            }
        }
        // JDK download group
        // If scenario is "No deployment" then no validation
        if (!(!jdkCheckBox.isSelected() && uploadLocalJdk.isSelected())) {
            String jdkUrl = this.jdkUrl.getText().trim();
            if (jdkUrl.isEmpty()) {
                isJdkValid = false;
                throw new ConfigurationException(message("dlgDlUrlErrMsg"), message("dlgDlUrlErrTtl"));
            } else {
                Boolean isUrlValid = false;
                // JDK auto upload or third party option selected.
                if (uploadLocalJdk.isSelected() || thirdPartyJdk.isSelected()) {
                    if (jdkUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                        jdkUrl = AUTO;
                    }
                    isUrlValid = true;
                } else {
                    // JDK cloud option selected
                    try {
                        new URL(jdkUrl);
                        if (WAEclipseHelperMethods.isBlobStorageUrl(jdkUrl)) {
                            isUrlValid = true;
                        } else {
                            throw new ConfigurationException(message("dlgDlUrlErrMsg"), message("dlgDlUrlErrTtl"));
                        }
                    } catch (MalformedURLException e) {
                        throw new ConfigurationException(message("dlgDlUrlErrMsg"), message("dlgDlUrlErrTtl"));
                    }
                }
                if (isUrlValid) {
                    String javaHome = this.javaHome.getText().trim();
                    if (javaHome.isEmpty()) {
                        isJdkValid = false;
                        throw new ConfigurationException(message("jvHomeErMsg"), message("genErrTitle"));
                    } else {
                        if (createAccLicenseAggDlg()) {
                            isJdkValid = configureJdkCloudDeployment();
                        } else {
                            isJdkValid = false;
                        }
                    }
                } else {
                    isJdkValid = false;
                }
            }
        } else {
            isJdkValid = true;
        }

        // Validation for Server
        if (isJdkValid && serverCheckBox.isSelected()) {
            if (serverType.getSelectedItem() == null || ((String) serverType.getSelectedItem()).isEmpty()) {
                isSrvValid = false;
                PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("dplEmtSerMsg"), null);
            } else if (serverPath.getText().isEmpty()) {
                isSrvValid = false;
                PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("dplWrngSerMsg"), null);
            } else if ((new File(serverPath.getText()).exists()) && (new File(serverPath.getText()).isAbsolute())) {
                // Server download group
                if (customDownloadServer.isSelected() || uploadLocalServer.isSelected()) {
                    // Validate Server URL
                    String srvUrl = this.serverUrl.getText().trim();
                    if (srvUrl.isEmpty()) {
                        isSrvValid = false;
                        PluginUtil.displayErrorDialog(message("dlgDlUrlErrTtl"), message("dlgDlUrlErrMsg"));
                    } else {
                        Boolean isSrvUrlValid = false;
                        // Server auto upload option selected.
                        if (uploadLocalServer.isSelected()) {
                            if (srvUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                                srvUrl = AUTO;
                            }
                            isSrvUrlValid = true;
                        } else {
                            // Server cloud option selected
                            try {
                                new URL(srvUrl);
                                if (WAEclipseHelperMethods.isBlobStorageUrl(srvUrl)) {
                                    isSrvUrlValid = true;
                                } else {
                                    PluginUtil.displayErrorDialog(message("dlgDlUrlErrTtl"), message("dlgDlUrlErrMsg"));
                                }
                            } catch (MalformedURLException e) {
                                PluginUtil.displayErrorDialog(message("dlgDlUrlErrTtl"), message("dlgDlUrlErrMsg"));
                            }
                        }
                        if (isSrvUrlValid) {
                            String srvHome = serverHomeDir.getText().trim();
                            if (srvHome.isEmpty()) {
                                isSrvValid = false;
                                PluginUtil.displayErrorDialog(message("genErrTitle"), message("srvHomeErMsg"));
                            } else {
                                isSrvValid = configureSrvCloudDeployment(srvUrl, srvHome);
                            }
                        } else {
                            isSrvValid = false;
                        }
                    }
                } else {
                    isSrvValid = true;
                }
            } else {
                isSrvValid = false;
                PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("dplWrngSerMsg"), null);
            }
        }
    }
}

