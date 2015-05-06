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
    private JLabel lblSrvPath;

    private ApplicationsTab applicationsTab;

    private final Project project;
    private final WindowsAzureRole waRole;
    private final WindowsAzureProjectManager waProjManager;
    private boolean accepted = false;
    private boolean srvAccepted = false;
    private String jdkPrevName;
    private final ArrayList<String> fileToDel = new ArrayList<String>();
    private String finalSrvPath;
    private WindowsAzureRoleComponentImportMethod finalImpMethod;
    private String finalAsName;
    private String finalJdkPath;
    private boolean isManualUpdate = true;
    private boolean modified;
    private String srvPrevName;

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
            String srvName = waRole.getServerName();
            if (srvName == null) {
                setEnableServer(false);
                setEnableDlGrpSrv(false, false);
                enableApplicationTab(false);
            } else {
                String srvSrcPath = waRole.getServerSourcePath();
                String thirdServerName = waRole.getServerCloudName();

                serverCheckBox.setSelected(true);
                setEnableServer(true);
                isManualUpdate = false;
                serverType.setSelectedItem(srvName);
                serverPath.setText(srvSrcPath);
                enableApplicationTab(true);
                showThirdPartySrvNames(true, srvName, thirdServerName);
                // Server download group
                String srvUrl = waRole.getServerCloudURL();
                if (srvUrl != null && !srvUrl.isEmpty()) {
                    // server auto upload option configured
                    if (JdkSrvConfigUtilMethods.isServerAutoUploadPrevSelected(waRole)) {
                        if (thirdServerName.isEmpty()) {
                            uploadLocalServer.setSelected(true);
                        } else {
                            thrdPrtSrvBtn.setSelected(true);
                            enableThirdPartySrvCombo(true);
                            srvAccepted = true;
                            srvPrevName = thirdServerName;
                        }
                        setEnableDlGrpSrv(true, true);
                        if (!thirdServerName.isEmpty()) {
                            thrdPrtSrvCmb.setSelectedItem(thirdServerName);
                        }
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
                    if (srvSrcPath.isEmpty()) {
                        lblDlNoteUrlSrv.setText(message("dlgDlNtLblUrl"));
                    } else {
                        String dirName = new File(srvSrcPath).getName();
                        lblDlNoteUrlSrv.setText(String.format(message("dlNtLblDir"), dirName));
                    }
                    String srvKey = waRole.getServerCloudKey();
                    UIUtils.populateStrgNameAsPerKey(srvKey, storageAccountServer);
                }
            }
            checkSDKPresenceAndEnableServer();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("srvErrTtl"), message("getSrvBothErrMsg"), e);
        } finally {
            isManualUpdate = true;
        }
        if (jdkCheckBox.isSelected() || thirdPartyJdk.isSelected() || customDownloadJdk.isSelected()) {
            serverCheckBox.setEnabled(true);
        }
        if (!serverPath.getText().isEmpty()) {
            enforceSameLocalCloudServer();
        }
    }

    public void checkSDKPresenceAndEnable() {
        String sdkVersion = WindowsAzureProjectManager.getLatestAzureVersionForSA();
        if (sdkVersion == null || sdkVersion.isEmpty()) {
            jdkCheckBox.setEnabled(false);
            setEnableJDK(false);
        }
    }

    public void checkSDKPresenceAndEnableServer() {
        String sdkVersion = WindowsAzureProjectManager.getLatestAzureVersionForSA();
        if ((sdkVersion == null || sdkVersion.isEmpty()) && !uploadLocalServer.isSelected()) {
            enableLocalServerPathCmpnts(false);
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
        thrdPrtSrvBtn.addActionListener(createThirdPartySrvListener());
        setEnableDlGrpSrv(false, false);
        storageAccountServer.addItemListener(createStorageAccountServerListener());
        serverUrl.getDocument().addDocumentListener(createServerUrlListener());
        checkSDKPresenceAndEnable();
        settingsPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isManualUpdate) {
                    if (!createAccLicenseAggDlg(true) && !((waProjManager == null || configureJdkCloudDeployment()))) {
                        try {
                            isManualUpdate = false;
                            settingsPane.setSelectedIndex(0);
                        } finally {
                            isManualUpdate = true;
                        }
                    }
                    if (!createAccLicenseAggDlg(false) && !((waProjManager == null || configureSrvCloudDeployment()))) {
                        try {
                            isManualUpdate = false;
                            settingsPane.setSelectedIndex(1);
                        } finally {
                            isManualUpdate = true;
                        }
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
        serverType.addItemListener(createServerTypeListener());
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

    private ItemListener createServerTypeListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (isManualUpdate && serverType.getSelectedItem() != null) {
                    enforceSameLocalCloudServer();
                    if (uploadLocalServer.isSelected()) {
                        updateServerHome(serverPath.getText());
                    } else if (thrdPrtSrvBtn.isSelected()) {
                        updateServerHomeForThirdParty();
                        String currentName = (String) thrdPrtSrvCmb.getSelectedItem();
                        if (!currentName.equalsIgnoreCase(srvPrevName)) {
                            srvAccepted = false;
                            srvPrevName = currentName;
                        }
                    } else if (customDownloadServer.isSelected()) {
                        if (serverUrl.getText().isEmpty()) {
                            updateServerHome(serverPath.getText());
                        } else {
                            modifySrvUrlText();
                        }
                    }
                    modified = true;
                }
            }
        };
    }

    private ItemListener createServerTypePreferenceListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (isManualUpdate && serverType.getSelectedItem() != null) {
                    enforceSameLocalCloudServer();
                    if (uploadLocalServer.isSelected()) {
                        updateServerHome(serverPath.getText());
                    } else if (thrdPrtSrvBtn.isSelected()) {
                        updateServerHomeForThirdParty();
                        String currentName = (String) thrdPrtSrvCmb.getSelectedItem();
                        if (!currentName.equalsIgnoreCase(srvPrevName)) {
                            srvAccepted = false;
                            srvPrevName = currentName;
                        }
                    } else if (customDownloadServer.isSelected()) {
                        if (serverUrl.getText().isEmpty()) {
                            updateServerHome(serverPath.getText());
                        } else {
                            modifySrvUrlText();
                        }
                    }
                    modified = true;
                }
            }
        };
    }

    private ItemListener createServerPreferenceListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (serverCheckBox.isSelected()) {
                    srvChkBoxChecked();
                } else {
                    if (serverType.getSelectedItem() != null) {
                        // Remove server home settings
                        removeServerHomeSettings();
                        // Remove server setting
                        updateServer(null, null, AzurePlugin.cmpntFile);
                        srvChkBoxUnChecked();
                    }
                }
                modified = true;
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

    private void handleServerDirRemoval() throws WindowsAzureInvalidProjectOperationException {
        String oldName = waRole.getServerName();
        String oldPath = waRole.getServerSourcePath();
        // Remove old server from approot
        if (oldName != null && oldPath != null && !oldPath.isEmpty() &&  !fileToDel.contains("srv")) {
            fileToDel.add("srv");
            WindowsAzureRoleComponent cmp = getPrevCmpnt(message("typeSrvDply"));
            if (cmp != null) {
                finalSrvPath = cmp.getImportPath();
                finalImpMethod = cmp.getImportMethod();
                finalAsName = cmp.getDeployName();
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
     * Method updates server home,
     * according to current package type.
     * Method will get called when user click
     * on OK button or tries to navigate to other page.
     * @param srvHome
     */
    private void updateServerHomeAsPerPackageType(String srvHome) {
        try {
            WAServerConfUtilMethods.updateServerHome(srvHome, waRole, waProjManager, serverPath.getText().trim(), getServerName(), AzurePlugin.cmpntFile);
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("srvHomeErr"));
        }
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
                    if (customDownloadServer.isSelected() || uploadLocalServer.isSelected()) {
                        updateServerHome(waRole);
                    } else if (thrdPrtSrvBtn.isSelected()) {
                        updateServerHomeForThirdParty();
                    }
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

    private ActionListener createThirdPartySrvListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modified = true;
                if (!thrdPrtSrvCmb.isEnabled()) {
                    thirdPartySrvBtnSelected();
                    srvPrevName = (String) thrdPrtSrvCmb.getSelectedItem();
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
            } else if (uploadLocalServer.isSelected() && serverPath.getText().isEmpty()) {
                return createValidationInfo(message("dplEmtSerPtMsg"), serverPath);
            } else if (!serverPath.getText().isEmpty() && !(new File(serverPath.getText()).exists())) {
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
                    srvChkBoxChecked();
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
                    configureAutoUploadServerSettings();
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
                    srvDeployBtnSelected();
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

    public void enableThirdPartySrvCombo(Boolean status) {
        thrdPrtSrvCmb.setEnabled(status);
        thrdPrtSrvBtn.setSelected(status);
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

    public void showThirdPartySrvNames(Boolean status, String localSrvName, String depSrvName) {
        if (status) {
            try {
                String[] thrdPrtSrvArr;
                if (localSrvName == null || localSrvName.isEmpty()) {
                    thrdPrtSrvArr = WindowsAzureProjectManager.getAllThirdPartySrvNames(AzurePlugin.cmpntFile, depSrvName);
                } else {
                    thrdPrtSrvArr = WindowsAzureProjectManager.getThirdPartySrvNames(AzurePlugin.cmpntFile, localSrvName, depSrvName);
                }
                // check at least one element is present else disable
                if (thrdPrtSrvArr.length >= 1) {
                    thrdPrtSrvCmb.setModel(new DefaultComboBoxModel(thrdPrtSrvArr));
                    String valueToSet = "";
                    if (localSrvName == null || localSrvName.isEmpty()) {
                        valueToSet = WindowsAzureProjectManager.getFirstDefaultThirdPartySrvName(AzurePlugin.cmpntFile);
                    } else {
                        valueToSet = WindowsAzureProjectManager.getDefaultThirdPartySrvName(AzurePlugin.cmpntFile, localSrvName);
                    }
                    if (valueToSet.isEmpty()) {
                        valueToSet = thrdPrtSrvArr[0];
                    }
                    thrdPrtSrvCmb.setSelectedItem(valueToSet);
                }
            } catch (WindowsAzureInvalidProjectOperationException e) {
                AzurePlugin.log(e.getMessage());
            }
        } else {
            thrdPrtSrvCmb.removeAllItems();
//            getThrdPrtSrvCmb().setText("");
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
     * Listener for third party JDK name combo box.
     * Updates URL and java home.
     */
    public void thirdPartySrvComboListener() {
        updateServerDlURL();
        try {
            serverHomeDir.setText(WindowsAzureProjectManager.getThirdPartyServerHome((String) thrdPrtSrvCmb.getSelectedItem(), AzurePlugin.cmpntFile));
        } catch (WindowsAzureInvalidProjectOperationException e) {
            AzurePlugin.log(e.getMessage());
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
    public void srvChkBoxChecked() {
        setEnableServer(true);
        try {
            String[] servList = WindowsAzureProjectManager.getServerTemplateNames(AzurePlugin.cmpntFile);
            Arrays.sort(servList);
            serverType.setModel(new DefaultComboBoxModel(servList));
            // select third party server button.
            thrdPrtSrvBtn.setSelected(true);
            if (uploadLocalServer.isSelected()) {
                configureAutoUploadServerSettings();
            } else if (thrdPrtSrvBtn.isSelected()) {
                thirdPartySrvBtnSelected();
            } else {
                srvDeployBtnSelected();
            }
            checkSDKPresenceAndEnableServer();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            AzurePlugin.log(e.getMessage());
        }
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
            updateSrvDlNote();
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

    public void enableLocalServerPathCmpnts(boolean status) {
        lblSrvPath.setEnabled(status);
        serverPath.setEnabled(status);
    }

    /**
     * Enable or disable components of
     * Server download group according to status.
     * @param status
     */
    public void setEnableDlGrpSrv(boolean status, boolean applyAutoUlParams) {
        thrdPrtSrvBtn.setEnabled(status);
        customDownloadServer.setEnabled(status);
        uploadLocalServer.setEnabled(status);
        if (!serverCheckBox.isSelected()) {
            showThirdPartySrvNames(true, "", "");
        }
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
            thrdPrtSrvBtn.setSelected(false);
            customDownloadServer.setSelected(false);
            uploadLocalServer.setSelected(false);
            serverUrl.setText("");
            storageAccountServer.removeAllItems();
            serverHomeDir.setText("");
            lblDlNoteUrlSrv.setText(message("dlNtLblUrlSrv"));
            enableThirdPartySrvCombo(false);
        } else {
            JdkSrvConfig.populateStrgAccComboBox((String) storageAccountServer.getSelectedItem(), storageAccountServer, JdkSrvConfig.SRV_TXT, false);
        }
    }

    public void enableApplicationTab(boolean status) {
        applicationsTab.setEnable(status);
    }

    /**
     * Method used when server auto upload radio
     * button selected.
     */
    public void configureAutoUploadServerSettings() {
        setEnableDlGrpSrv(true, true);
        enableLocalServerPathCmpnts(true);
        populateDefaultStrgAccForSrvAuto();
        updateServerDlURL();
        updateSrvDlNote();
        updateServerHome(serverPath.getText());
        enableThirdPartySrvCombo(false);
        enableApplicationTab(true);
        enforceSameLocalCloudServer();
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
    public void updateSrvDlNote() {
        // Update note below server URL text box
        String srvPath = serverPath.getText();
        File file = new File(srvPath);
        if (!srvPath.isEmpty() && file.exists()) {
            String dirName = file.getName();
            lblDlNoteUrlSrv.setText(String.format(message("dlNtLblDir"), dirName));
        } else {
            lblDlNoteUrlSrv.setText(message("dlgDlNtLblUrl"));
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
            String oldServerType = (String) serverType.getSelectedItem();
            // Update note below server URL text box
            if (customDownloadServer.isSelected()) {
                String dirName = serverDir.getName();
                lblDlNoteUrlSrv.setText(String.format(label, dirName));
            }
            // Auto detect server family
            String newServerType = WAEclipseHelperMethods.detectServer(serverDir, WAHelper.getTemplateFile(AzureBundle.message("cmpntFileName")));
            isManualUpdate = false;
            if (oldServerType == null || oldServerType.isEmpty()) {
                // if server family is not selected already
                if (newServerType != null && !newServerType.isEmpty()) {
                    serverType.setSelectedItem(newServerType);
                } else {
                    PluginUtil.displayInfoDialog(message("srvTtl"), message("srvNoDetectionMsg"));
                }
            } else {
                if (newServerType != null && !newServerType.isEmpty()) {
                    if (!oldServerType.equalsIgnoreCase(newServerType)) {
                        PluginUtil.displayInfoDialog(message("srvTtl"), String.format(message("srvWrngDetectionMsg"), newServerType));
                        serverType.setSelectedItem(newServerType);
                    }
                } else {
                    PluginUtil.displayInfoDialog(message("srvTtl"), message("srvNoDetectionMsg"));
                }
            }
//            String serverName = WAEclipseHelperMethods.detectServer(serverDir, WAHelper.getTemplateFile(AzureBundle.message("cmpntFileName")));
//            isManualUpdate = false;
//            if (serverName != null && !serverName.isEmpty()) {
//                this.serverType.setSelectedItem(serverName);
//            } else {
//                serverType.setSelectedItem(null);
//            }
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
        combo.setSelectedItem(JdkSrvConfigUtilMethods.getNameToSet(url, nameInUrl, JdkSrvConfig.accNames));
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
            } else if (tabControl != null && JdkSrvConfig.SRV_TXT.equals(tabControl) && thrdPrtSrvBtn.isSelected()) {
                urlTxt = prepareUrlForThirdPartySrv((String) thrdPrtSrvCmb.getSelectedItem(), newUrl);
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
            if (tabControl != null && JdkSrvConfig.SRV_TXT.equals(tabControl) && (uploadLocalServer.isSelected() || thrdPrtSrvBtn.isSelected())) {
                urlTxt = JdkSrvConfig.AUTO_TXT;
                return urlTxt;
            }
        }
        return urlTxt;
    }

    public static String prepareUrlForThirdPartySrv(String srvName, String url) {
        String finalUrl = "";
        try {
            finalUrl = JdkSrvConfigUtilMethods.prepareUrlForThirdPartySrv(srvName, url, AzurePlugin.cmpntFile);
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage());
        }
        return finalUrl;
    }


    /**
     * Method is used when server's deploy from download
     * radio button is selected.
     */
    public void srvDeployBtnSelected() {
        // server deploy radio button selected
        setEnableDlGrpSrv(true, false);
        checkSDKPresenceAndEnableServer();
        updateSrvDlNote();
        updateServerHome(serverPath.getText());
        enableThirdPartySrvCombo(false);
        enableApplicationTab(true);
        enforceSameLocalCloudServer();
    }

    /**
     * Method is used when third party JDK
     * radio button is selected.
     */
    public void thirdPartySrvBtnSelected() {
        setEnableDlGrpSrv(true, true);
        checkSDKPresenceAndEnableServer();
        enableThirdPartySrvCombo(true);
        thirdPartySrvComboListener();
        updateSrvDlNote();
        enableApplicationTab(true);
    }

    /**
     * Method is used when server URL text is modified.
     */
    public void modifySrvUrlText() {
		/*
		 * Extract storage account name
		 * and service endpoint from URL
		 * entered by user.
		 */
        String url = serverUrl.getText().trim();
        String nameInUrl = StorageRegistryUtilMethods.getAccNameFromUrl(url);
        urlModifyListner(url, nameInUrl, storageAccountServer);
		/*
		 * update home directory for server accordingly
		 */
        if (WAEclipseHelperMethods.isBlobStorageUrl(url) && url.endsWith(".zip")) {
            url = url.substring(0, url.indexOf(".zip"));
            updateServerHome(url);
        }
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
            if (createAccLicenseAggDlg(true)) {
                okToProceed = configureJdkCloudDeployment();
            } else {
                okToProceed = false;
            }
        }
        if (okToProceed) {
            boolean tempAccepted = true;
            if (thrdPrtSrvBtn.isSelected() && !srvAccepted) {
                tempAccepted = createAccLicenseAggDlg(false);
            }
            if (tempAccepted) {
                okToProceed = configureSrvCloudDeployment();
            } else {
                okToProceed = false;
            }
        }
        return okToProceed;
    }

    public boolean createAccLicenseAggDlg(boolean isForJdk) {
        String name = "";
        String url = "";
        try {
            if (isForJdk && !accepted && thirdPartyJdk.isSelected()) {
                name = (String) thirdPartyJdkName.getSelectedItem();
                url = WindowsAzureProjectManager.getLicenseUrl(name, AzurePlugin.cmpntFile);
                accepted = showAcceptDialog(name, url);
                return accepted;
            } else if (!isForJdk && !srvAccepted && thrdPrtSrvBtn.isSelected()) {
                name = (String) thrdPrtSrvCmb.getSelectedItem();
                url = WindowsAzureProjectManager.getThirdPartyServerLicenseUrl(name, AzurePlugin.cmpntFile);
                srvAccepted = showAcceptDialog(name, url);
                return srvAccepted;
            } else {
                return true;
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(e.getMessage(), e);
            return showAcceptDialog(name, url);
        }
    }

    private boolean showAcceptDialog(String name, String url) {
        StringBuilder sb = new StringBuilder("<html>").append(String.format(message("aggMsg"), name));
        sb.append(String.format(message("aggLnk"), url, url)).append("</html>");
        return Messages.showYesNoDialog(project, sb.toString(), message("aggTtl"), message("acptBtn"), "Cancel", null) == Messages.YES;
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
            handleServerDirRemoval();

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
                updateJavaHomeAsPerPackageType(javaHome);
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
     * @return
     */
    private boolean configureSrvCloudDeployment() {
        boolean isValid = true;
        String srvPath = serverPath.getText();
        String srvUrl = serverUrl.getText();
        String srvHome = serverHomeDir.getText();
        String srvName = getServerName();
        try {
            WAServerConfUtilMethods.removeServerHomeSettings(waRole, waProjManager);
            waRole.setServerCloudName(null);
            waRole.setServer(null, "", AzurePlugin.cmpntFile);

            if (serverCheckBox.isSelected()) {
                if (!srvName.isEmpty()) {
                    handleEndpointSettings(srvName);
                    waRole.setServer(srvName, srvPath, AzurePlugin.cmpntFile);
                    // JDK download group
                    // By default auto upload will be selected.
                    if (uploadLocalServer.isSelected() || thrdPrtSrvBtn.isSelected()) {
                        if (srvUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                            srvUrl = AUTO;
                        }
                        if (thrdPrtSrvBtn.isSelected()) {
                            waRole.setServerCldAltSrc(getServerCloudAltSource());
                            waRole.setServerCloudName((String) thrdPrtSrvCmb.getSelectedItem());
                        }
                        waRole.setServerCloudUploadMode(WARoleComponentCloudUploadMode.auto);
                    }
                    waRole.setServerCloudURL(srvUrl);
                    waRole.setServerCloudKey(AzureWizardModel.getAccessKey(storageAccountServer));
                    updateServerHomeAsPerPackageType(srvHome);
                }
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
    private void updateJavaHomeAsPerPackageType(String javaHome) {
        try {
            WAServerConfUtilMethods.updateJavaHome(javaHome, waRole, waProjManager, jdkPath.getText().trim(), AzurePlugin.cmpntFile);
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("jvHomeErr"));
        }
    }

    /**
     * Utility method to update server home value.
     */
    public void updateServerHome(String srvPath) {
        // set server home directory text box value
        try {
            String srvHome = WindowsAzureRole.constructServerHome((String) serverType.getSelectedItem(), srvPath, AzurePlugin.cmpntFile);
            serverHomeDir.setText(srvHome);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            AzurePlugin.log(e.getMessage());
        }
    }

    public void updateServerHomeForThirdParty() {
        // set server home directory text box value
        try {
            serverHomeDir.setText(WindowsAzureProjectManager.getThirdPartyServerHome((String) thrdPrtSrvCmb.getSelectedItem(), AzurePlugin.cmpntFile));
        } catch (WindowsAzureInvalidProjectOperationException e) {
            AzurePlugin.log(e.getMessage());
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

    public void enforceSameLocalCloudServer() {
        try {
            String srvName = (String) serverType.getSelectedItem();
            String cloudSrv = "";
            if (thrdPrtSrvBtn.isSelected()) {
                cloudSrv = (String) thrdPrtSrvCmb.getSelectedItem();
            }
            if (cloudSrv.isEmpty()) {
				/*
				 * user first selects a local server and
				 * third party radio button is not selected.
				 */
                populateServerNames(srvName);
            } else {
                if (WindowsAzureProjectManager.checkCloudAndLocalFamilyAreEqual(AzurePlugin.cmpntFile, srvName, cloudSrv)) {
					/*
					 * user first selects the cloud server
					 * and then a local server that is compatible with cloud server.
					 */
                    showThirdPartySrvNames(true, srvName, "");
                    thrdPrtSrvCmb.setSelectedItem(cloudSrv);
                } else {
					/*
					 * user first selects the cloud server
					 * and then a local server that is different from cloud server.
					 */
                    populateServerNames(srvName);
                    if (thrdPrtSrvCmb.getItemCount() <= 0) {
                        // if no third party servers available
                        uploadLocalServer.setSelected(true);
                        configureAutoUploadServerSettings();
                    } else {
                        thirdPartySrvBtnSelected();
                    }
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            AzurePlugin.log(e.getMessage());
        }
    }

    private void populateServerNames(String srvName) {
        showThirdPartySrvNames(false, "", "");
        showThirdPartySrvNames(true, srvName, "");
        if (thrdPrtSrvCmb.getItemCount() <= 0) {
            // if no third party servers available
            enableThirdPartySrvCombo(false);
            thrdPrtSrvBtn.setEnabled(false);
        } else {
            thrdPrtSrvBtn.setEnabled(true);
        }
    }

    public void apply() throws ConfigurationException {
        if (!okToLeave()) {
            throw new ConfigurationException(message("error"));
        }
        boolean isJdkValid = true;
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
                throw new ConfigurationException(message("dplEmtSerMsg"), message("srvErrTtl"));
            } else if (uploadLocalServer.isSelected() && serverPath.getText().isEmpty()) {
                throw new ConfigurationException(message("dplWrngSerMsg"), message("srvErrTtl"));
            } else if (!serverPath.getText().isEmpty() && !(new File(serverPath.getText()).exists())) {
                throw new ConfigurationException(message("dplWrngSerMsg"), message("srvErrTtl"));
            } else {
                // Server download group
                if (customDownloadServer.isSelected()) {
                    // Validate Server URL
                    String srvUrl = this.serverUrl.getText().trim();
                    if (srvUrl.isEmpty()) {
                        throw new ConfigurationException(message("dlgDlUrlErrMsg"), message("dlgDlUrlErrTtl"));
                    } else {
                        // Server cloud option selected
                        try {
                            new URL(srvUrl);
                            if (WAEclipseHelperMethods.isBlobStorageUrl(srvUrl)) {
                                String srvHome = serverHomeDir.getText().trim();
                                if (srvHome.isEmpty()) {
                                    throw new ConfigurationException(message("srvHomeErMsg"), message("srvErrTtl"));
                                }
                            } else {
                                throw new ConfigurationException(message("dlgDlUrlErrMsg"), message("dlgDlUrlErrTtl"));
                            }
                        } catch (MalformedURLException e) {
                            throw new ConfigurationException(message("dlgDlUrlErrMsg"), message("dlgDlUrlErrTtl"));
                        }
                    }

                }
            }
        }
    }
}

