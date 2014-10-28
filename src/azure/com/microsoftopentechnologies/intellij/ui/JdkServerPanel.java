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
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.ui.util.JdkSrvConfig;
import com.microsoftopentechnologies.intellij.ui.util.JdkSrvConfigListener;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.intellij.util.WAHelper;
import com.microsoftopentechnologies.roleoperations.JdkSrvConfigUtilMethods;
import com.microsoftopentechnologies.roleoperations.WAServerConfUtilMethods;
import com.microsoftopentechnologies.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.util.WAEclipseHelperMethods;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;


public class JdkServerPanel {
    private final String AUTO = "auto";

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
    //Server tab
    private JCheckBox serverCheckBox;
    private TextFieldWithBrowseButton serverPath;
    private JComboBox serverType;
    private JRadioButton uploadLocalServer;
    private JRadioButton customDownloadServer;
    private JTextField serverUrl;
    private JComboBox storageAccountServer;
    private JTextField serverHomeDir;
    private JXHyperlink accountsButton;
    private JXHyperlink serverAccountsButton;
    private JLabel jdkUrlLabel;
    private JLabel storageAccountJdkLabel;
    private JLabel serverHomeDirLabel;
    private JLabel lblDlNoteUrl;
    private JLabel lblSelect;
    private JLabel lblUrlSrv;
    private JLabel lblDlNoteUrlSrv;
    private JLabel lblKeySrv;
    private JPanel applicationsSettings;

    private ApplicationsTab applicationsTab;

    private final Project project;
    private final WindowsAzureRole waRole;
    private final WindowsAzureProjectManager waProjManager;
    private static boolean accepted = false;
    private String jdkPrevName;
    private boolean isManualUpdate = true;
    private boolean modified;

    public JdkServerPanel(Project project, WindowsAzureRole waRole, WindowsAzureProjectManager waProjManager) {
        this.project = project;
        this.waRole = waRole;
        this.waProjManager = waProjManager;
        init();
        // existing role
        if (waProjManager != null) {
            initJdkTab();
            initServerTab();
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
        applicationsTab.setModified(false);
    }

    public ApplicationsTab getApplicationsTab() {
        return applicationsTab;
    }

    private void initJdkTab() {
        // Check JDK is already enabled or not
        // and if enabled show appropriate values on property page
        try {
            String jdkSrcPath = null;
            jdkSrcPath = waRole.getJDKSourcePath();

            if (jdkSrcPath == null) {
                setEnableJDK(false);
            } else {
                setEnableJDK(true);
                jdkPath.setText(jdkSrcPath);
                String jdkName = waRole.getJDKCloudName();
                // project may be using deprecated JDK, hence pass to method
                showThirdPartyJdkNames(true, jdkName);
                String jdkUrlValue = waRole.getJDKCloudURL();
                // JDK download group
                if (jdkUrl != null && !jdkUrlValue.isEmpty()) {
                    // JDK auto upload option configured
                    if (JdkSrvConfigUtilMethods.isJDKAutoUploadPrevSelected(waRole)) {
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
                        setEnableDlGrp(true, true);
                    } else {
                        // JDK deploy option configured
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
                    String dirName = new File(jdkSrcPath).getName();
                    lblDlNoteUrl.setText(String.format(message("dlNtLblDir"), dirName));

                    // Update storage account combo box.
                    String jdkKey = waRole.getJDKCloudKey();
                    UIUtils.populateStrgNameAsPerKey(jdkKey, storageAccountJdk);
                }
            }
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
                        serverPath.setText(waRole.getServerCloudHome());
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
        }
        if (jdkCheckBox.isSelected()) {
            serverCheckBox.setEnabled(true);
        } else {
            setEnableDlGrp(false, false);
            setEnableServer(false);
            setEnableDlGrpSrv(false, false);
        }

    }

//    @Override
//    public WizardStep onNext(final AzureWizardModel model) {
//        int currentTab = settingsPane.getSelectedIndex();
//        if (currentTab == 2) {
//            return super.onNext(model);
//        } else {
//            settingsPane.setSelectedIndex(++currentTab);
//            return this;
//        }
//    }

//    @Override
//    public WizardStep onPrevious(final AzureWizardModel model) {
//        int currentTab = settingsPane.getSelectedIndex();
//        if (currentTab == 0) {
//            return super.onPrevious(model);
//        } else {
//            settingsPane.setSelectedIndex(--currentTab);
//            return this;
//        }
//    }

//    @Override
//    public JComponent prepare(final WizardNavigationState state) {
//        rootPanel.revalidate();
//        state.FINISH.setEnabled(true);
//        return rootPanel;
//    }

    public void init() {
        rootPanel.revalidate();
        jdkCheckBox.addItemListener(createJdkCheckBoxListener());
        jdkCheckBox.setSelected(true);
        jdkPath.addActionListener(UIUtils.createFileChooserListener(jdkPath, null, FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        jdkPath.getTextField().getDocument().addDocumentListener(createJdkPathListener());
//        serverPath.addActionListener(createServerPathListener()); // todo
        serverPath.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
            protected void onFileChoosen(@NotNull VirtualFile chosenFile) {
                super.onFileChoosen(chosenFile);
                serBrowseBtnListener();
                modifySrvText(waRole, message("dlNtLblDirSrv"));
                modified = true;
            }
        });
        accountsButton.setAction(createStorageAccountsAction(storageAccountJdk, JdkSrvConfig.JDK_TXT));
        serverAccountsButton.setAction(createStorageAccountsAction(storageAccountServer, JdkSrvConfig.SRV_TXT));
        serverCheckBox.addItemListener(createServerListener());
        serverCheckBox.setSelected(false);
        uploadLocalServer.addActionListener(createUploadLocalServerListener());
        customDownloadServer.addActionListener(createCustomDownloadServerListener());
//        thirdPartyJdkName = new JComboBox(JdkSrvConfigListener.getThirdPartyJdkNames(true));
        uploadLocalJdk.addActionListener(createUploadLocalJdkListener());
        thirdPartyJdk.addActionListener(createThirdPartyJdkListener());
        customDownloadJdk.addActionListener(createCustomDownloadJdkListener());
        jdkUrl.getDocument().addDocumentListener(createJdkUrlListener());
        storageAccountJdk.addItemListener(createStorageAccountJdkListener());
        thirdPartyJdkName.addItemListener(createThirdPartyJdkNameListener());
//        setEnableDlGrp(false, false);
        setEnableDlGrpSrv(false, false);
        storageAccountServer.addItemListener(createStorageAccountServerListener());
        serverUrl.getDocument().addDocumentListener(createServerUrlListener());
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
                    jdkDeployBtnSelected(waRole);
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
                    thirdPartyJdkBtnSelected(waRole, message("dlNtLblDir"));
                    jdkPrevName = (String) thirdPartyJdkName.getSelectedItem();
                }
            }
        };
    }

    public Map<String, String> getDeployPageValues() {
        Map <String, String> values = new HashMap<String, String>();
        // JDK
        values.put("jdkChecked", String.valueOf(jdkCheckBox.isSelected()));
        values.put("jdkLoc" , jdkPath.getText());
        // JDK download group
        values.put("jdkDwnldChecked", String.valueOf(customDownloadJdk.isSelected()));
        values.put("jdkAutoDwnldChecked", String.valueOf(uploadLocalJdk.isSelected()));
        values.put("jdkThrdPartyChecked" , String.valueOf(thirdPartyJdk.isSelected()));
        values.put("jdkName" , thirdPartyJdkName.getSelectedItem().toString());
        values.put("jdkUrl" , jdkUrl.getText());
        values.put("jdkKey" , AzureWizardModel.getAccessKey(storageAccountJdk));
        values.put("javaHome", javaHome.getText());
        // Server
        values.put("serChecked", String.valueOf(serverCheckBox.isSelected()));
        values.put("servername", (String) serverType.getSelectedItem());
        values.put("serLoc", serverPath.getText());
        values.put("tempFile", WAHelper.getTemplateFile(message("cmpntFileName")));
        // Server download group
        values.put("srvDwnldChecked", String.valueOf(customDownloadServer.isSelected()));
        values.put("srvAutoDwnldChecked", String.valueOf(uploadLocalServer.isSelected()));
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
                if (jdkCheckBox.isSelected()) {
                    try {
                        // populate third party JDKs whose status in not deprecated
                        jdkChkBoxChecked(waRole, "");
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } else {
                    jdkChkBoxUnChecked();
                    accepted = false;
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
        // JDK
        if (jdkCheckBox.isSelected()) {
            if (jdkPath.getText().isEmpty()) {
                return createValidationInfo(message("jdkPathErrMsg"), jdkPath);
            } else {
                File file = new File(jdkPath.getText());
                if (!file.exists()) {
                    return createValidationInfo(message("dplWrngJdkMsg"), jdkPath);
                } else {
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
                }
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
        applicationsTab = new ApplicationsTab(project, waProjManager, waRole);
        applicationsTab.initAppTab();
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
                    configureAutoUploadJDKSettings(waRole, message("dlNtLblDir"));
                } catch (Exception ex) {
                    log(ex.getMessage());
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
                } else {
					/*
					 * server auto upload radio button unselected
					 * and deploy button selected.
					 */
                    if (customDownloadServer.isSelected()) {
                        serverUrl.setText(getUrl(storageAccountServer));
                        return;
                    }
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
                }
//                handlePageComplete();
            }
        };
    }

    /**
     * Method is used when JDK check box is checked.
     * @return
     */
    public String jdkChkBoxChecked(WindowsAzureRole role, String depJdkName) throws Exception {
        // Pre-populate with auto-discovered JDK if any
        String jdkDefaultDir = WAEclipseHelperMethods.jdkDefaultDirectory(null);
        jdkPath.setText(jdkDefaultDir);
        setEnableJDK(true);
        enableJdkRdButtons(uploadLocalJdk);
        serverCheckBox.setEnabled(true);
        configureAutoUploadJDKSettings(role, message("dlNtLblDir"));
        showThirdPartyJdkNames(true, depJdkName);
        return jdkDefaultDir;
    }

    /**
     * Method is used when JDK check box is unchecked.
     */
    public void jdkChkBoxUnChecked() {
        serverCheckBox.setSelected(false);
        setEnableJDK(false);
        setEnableServer(false);
        setEnableDlGrp(false, false);
        setEnableDlGrpSrv(false, false);
        showThirdPartyJdkNames(false, "");
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
    public void setEnableJDK(boolean status){
//        jdkCheckBox.setSelected(status);
        jdkPath.setEnabled(status);
        if (!status) {
            jdkPath.setText("");
        }
    }

    /**
     * Enable both radio buttons of JDK
     * cloud deployment and select local one.
     * @param defaultSelectButton
     */
    public void enableJdkRdButtons(JRadioButton defaultSelectButton) {
        uploadLocalJdk.setEnabled(true);
        customDownloadJdk.setEnabled(true);
        thirdPartyJdk.setEnabled(true);
        defaultSelectButton.setSelected(true);
    }

    /**
     * Method decides whether to
     * show third party JDK names or not.
     * @param status
     */
    public void showThirdPartyJdkNames(Boolean status, String depJdkName) {
        if (status) {
            try {
                String[] thrdPrtJdkArr = WindowsAzureProjectManager.getThirdPartyJdkNames(AzurePlugin.cmpntFile, depJdkName);
                // check at least one element is present
                if (thrdPrtJdkArr.length >= 1) {
                    thirdPartyJdkName.setModel(new DefaultComboBoxModel(thrdPrtJdkArr));
                }
            } catch (WindowsAzureInvalidProjectOperationException ex) {
                log(ex.getMessage(), ex);
            }
        } else {
            thirdPartyJdkName.removeAllItems();
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
     * Method is used when JDK's deploy from download
     * radio button is selected.
     * @param role
     */
    public void jdkDeployBtnSelected(WindowsAzureRole role) {
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
     * @param role
     * @param label
     */
    public void thirdPartyJdkBtnSelected(WindowsAzureRole role, String label) {
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
        uploadLocalJdk.setEnabled(status);
        customDownloadJdk.setEnabled(status);
        thirdPartyJdk.setEnabled(status);
        storageAccountServer.setEnabled(status);
        storageAccountJdkLabel.setEnabled(status);
        jdkUrlLabel.setEnabled(status);
        lblDlNoteUrl.setEnabled(status);
        javaHome.setEnabled(status);
        jdkUrl.setEnabled(status);
        if (status && applyAutoUlParams) {
            // Always disable and auto-generate JDK url and derive Java home.
            jdkUrl.setEditable(false);
            javaHome.setEnabled(!status);
        } else {
            jdkUrl.setEditable(true);
            javaHome.setEnabled(status);
        }
        if (!status) {
            customDownloadJdk.setSelected(false);
            uploadLocalJdk.setSelected(false);
            thirdPartyJdk.setSelected(false);
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
        serverHomeDirLabel.setEnabled(status);
        serverUrl.setEnabled(status);

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
     * Method used when JDK auto upload radio
     * button selected.
     * @param role
     * @param label
     */
    public void configureAutoUploadJDKSettings(WindowsAzureRole role, String label) throws Exception {
        setEnableDlGrp(true, true);
        updateJDKDlURL();
        updateJDKDlNote(label);
        updateJDKHome(jdkPath.getText());
        enableThirdPartyJdkCombo(false);
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
            if (serverName != null && !serverName.isEmpty()) {
                this.serverType.setSelectedItem(serverName);
            } else {
                serverType.setSelectedItem(null);
            }
        } catch (Exception e) {
            log(e.getMessage(), e);
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
        if (index != 0) {
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
    public  String cmbBoxListener(JComboBox combo, String urlTxt, String tabControl) {
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
            boolean tempAccepted = true;
            if (thirdPartyJdk.isSelected() && !accepted) {
                tempAccepted = createAccLicenseAggDlg();
                accepted = tempAccepted;
            }
            if (tempAccepted) {
                String jdkUrl = this.jdkUrl.getText().trim();
                if ((uploadLocalJdk.isSelected() || thirdPartyJdk.isSelected())&& jdkUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                    jdkUrl = AUTO;
                }
                okToProceed = configureJdkCloudDeployment(jdkUrl, javaHome.getText().trim());
            } else {
                okToProceed = false;
            }
        }

        if (okToProceed && jdkCheckBox.isSelected()&& serverCheckBox.isSelected()) {
            String srvUrl = serverUrl.getText().trim();
            if (uploadLocalServer.isSelected() && srvUrl.equalsIgnoreCase(JdkSrvConfig.AUTO_TXT)) {
                srvUrl = AUTO;
            }
            okToProceed = configureSrvCloudDeployment(srvUrl, serverHomeDir.getText().trim());
        }
        return okToProceed;
    }

    public boolean createAccLicenseAggDlg() {
        String jdkName = (String) thirdPartyJdkName.getSelectedItem();
        StringBuilder sb = new StringBuilder("<html>").append(String.format(message("aggMsg"), jdkName));
        String url = "";
        try {
            url = WindowsAzureProjectManager.getLicenseUrl(jdkName, AzurePlugin.cmpntFile);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(e.getMessage(), e);
        }
        sb.append(String.format(message("aggLnk"), url, url)).append("</html>");

        return Messages.showYesNoDialog(project, sb.toString(), message("aggTtl"), message("acptBtn"), "Cancel", null) == Messages.YES;
    }

    /**
     * Method configures cloud deployment for JDK
     * by saving URL, key and cloud method.
     * @param jdkUrl
     * @param javaHome
     * @return
     */
    private boolean configureJdkCloudDeployment(String jdkUrl, String javaHome) {
        boolean isValid = true;
        try {
            waRole.setJDKCloudURL(jdkUrl);
            updateJavaHome(javaHome);
            waRole.setJDKCloudKey(AzureWizardModel.getAccessKey(storageAccountJdk));
			/*
			 * If third party radio button selected.
			 */
            if (thirdPartyJdk.isSelected()) {
                waRole.setJDKCloudUploadMode(WARoleComponentCloudUploadMode.auto);
				/*
				 * Create JDK name property.
				 * Set cloudvalue in environment variable
				 * Set cloudaltsrc in component
				 */
                String jdkName = (String) thirdPartyJdkName.getSelectedItem();
                waRole.setJDKCloudName(jdkName);
                waRole.setJdkCloudValue(WindowsAzureProjectManager.getCloudValue(jdkName, AzurePlugin.cmpntFile));
                waRole.setJdkCldAltSrc(WindowsAzureProjectManager.getCloudAltSrc(jdkName, AzurePlugin.cmpntFile));
            } else {
                waRole.setJDKCloudName(null);
                waRole.setJdkCloudValue(null);
                waRole.setJdkCldAltSrc(null);
                if (uploadLocalJdk.isSelected()) {
                    waRole.setJDKCloudUploadMode(WARoleComponentCloudUploadMode.auto);
                } else {
                    waRole.setJDKCloudUploadMode(null);
                }
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

    public void apply() throws ConfigurationException {
        if (!okToLeave()) {
            throw new ConfigurationException(message("error"));
        }
    }
}

