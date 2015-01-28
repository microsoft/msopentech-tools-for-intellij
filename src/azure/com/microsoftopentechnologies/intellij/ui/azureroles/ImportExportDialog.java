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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.interopbridges.tools.windowsazure.*;
import com.microsoftopentechnologies.azurecommons.roleoperations.ImportExportDialogUtilMethods;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.intellij.ui.AzureWizardModel;
import com.microsoftopentechnologies.intellij.ui.StorageAccountPanel;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.ui.util.JdkSrvConfig;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.util.PluginUtil.BASE_PATH;

public class ImportExportDialog extends DialogWrapper {
    private static final String[] IMP_METHODS = {"WAR", "JAR", "EAR", "copy", "zip", "none"};
    private static final String[] CLOUD_METHODS = {"same", "unzip", "copy"};

    private JPanel contentPane;
    private JTextField txtFromPath;
    private JButton btnFileSys;
    private JButton btnDir;
    private JComboBox comboImport;
    private JTextField txtName;
    private JComboBox comboDeploy;
    private JTextField txtToDir;
    private JCheckBox dlCheckBtn;
    private JTextField txtUrl;
    private JLabel lblUrl;
    private JLabel lblDlMethod;
    private JComboBox comboCloud;
    private JLabel lblStrgAcc;
    private JComboBox comboStrgAcc;
    private JXHyperlink accLink;

    private Project project;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private WindowsAzureRoleComponent winAzureRoleCmpnt;
    public ArrayList<String> cmpList = new ArrayList<String>();
    private boolean isEdit;
    private boolean isManualUpdate = true;
    private boolean modified;

    public ImportExportDialog(Project project, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole, WindowsAzureRoleComponent component) {
        super(true);
        this.project = project;
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        this.winAzureRoleCmpnt = component;
        this.isEdit = component != null;
        try {
            for (int i = 0; i < waRole.getComponents().size(); i++) {
                WindowsAzureRoleComponent cmpnt =waRole.getComponents().get(i);
                cmpList.add(PluginUtil.getAsName(project, cmpnt.getImportPath(), cmpnt.getImportMethod(), cmpnt.getDeployName()).toLowerCase());
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialog(message("cmpntSetErrTtl"), message("cmpntgetErrMsg"));
        }
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void init() {
        setTitle(winAzureRoleCmpnt == null ? message("dlgImpShellTtl") : message("dlgImpEdtShlTtl"));

        btnFileSys.addActionListener(createBtnFileSysListener());
        btnDir.addActionListener(createBtnDirListener());
        comboImport.setModel(new DefaultComboBoxModel(IMP_METHODS));
        comboImport.setSelectedIndex(1);
        comboImport.addItemListener(createComboImportListener());
        txtName.addFocusListener(createTxtNameListener());

        comboDeploy.setModel(new DefaultComboBoxModel(WindowsAzureRoleComponentDeployMethod.values()));
        comboDeploy.addItemListener(createComboDeployListener());
        txtToDir.setText(".\\");

        dlCheckBtn.addItemListener(createDlCheckBoxListener());
        comboCloud.setModel(new DefaultComboBoxModel(CLOUD_METHODS));
        setEnableDlGrp(false);
        comboStrgAcc.addItemListener(createStorageAccListener());
        accLink.setAction(createAccLinkAction());

        if (isEdit) {
            populateData();
        }
        super.init();
    }

    private FocusListener createTxtNameListener() {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!txtFromPath.getText().isEmpty() && txtName.getText().isEmpty()) {
                    txtName.setText(getAsName());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        };
    }

    public ActionListener createBtnFileSysListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String oldPath = txtFromPath.getText();
                VirtualFile[] files = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), txtFromPath, project,
                        txtFromPath.getText().isEmpty() ? null : LocalFileSystem.getInstance().findFileByPath(txtFromPath.getText()));
                if (files.length > 0) {
                    txtFromPath.setText(FileUtil.toSystemDependentName(files[0].getPath()));
                    try {
                        String selFile = txtFromPath.getText();
                        String projectPath = project.getBasePath();
                        if (isWorkspaceProj(waProjManager.getProjectName()) && selFile.contains(projectPath)) {
                            String replaceString = selFile;
                            String subString = selFile.substring(selFile.indexOf(projectPath), projectPath.length());
                            selFile = replaceString.replace(subString, BASE_PATH);
                        }
                        txtFromPath.setText(selFile);
                            /*
					         * If new from path is selected then
					         * remove previous As name text.
					         */
                        if (!oldPath.equals(selFile)) {
                            txtName.setText("");
                        }
                        updateImportMethodCombo(selFile);
                        updateDeployMethodCombo();
                    } catch (WindowsAzureInvalidProjectOperationException ex) {
                        log(ex.getMessage(), ex);
                    }
                }
            }
        };
    }

    public ActionListener createBtnDirListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String oldPath = txtFromPath.getText();
                VirtualFile[] files = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFolderDescriptor(), txtFromPath, project,
                        txtFromPath.getText().isEmpty() ? null : LocalFileSystem.getInstance().findFileByPath(txtFromPath.getText()));
                if (files.length > 0) {
                    txtFromPath.setText(FileUtil.toSystemDependentName(files[0].getPath()));
                    String directory = txtFromPath.getText();
                    try {
                        String projectPath = project.getBasePath();
                        if (isWorkspaceProj(waProjManager.getProjectName()) && directory.contains(projectPath)) {
                            String replaceString = directory;
                            String subString = directory.substring(directory.indexOf(projectPath), projectPath.length());
                            directory = replaceString.replace(subString, BASE_PATH);
                        }
                        txtFromPath.setText(directory);
				/*
				 * If new from path is selected then
				 * remove previous As name text.
				 */
                        if (!oldPath.equals(directory)) {
                            txtName.setText("");
                        }
                        updateImportMethodCombo(directory);
                        updateDeployMethodCombo();
                    } catch (WindowsAzureInvalidProjectOperationException ex) {
                        log(ex.getMessage(), ex);
                    }
                }
            }
        };
    }

    private ItemListener createComboImportListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && isManualUpdate) {
                    try {
                        isManualUpdate = false;
                        if (!txtFromPath.getText().isEmpty() && !txtName.getText().isEmpty()) {
                            String oldVal = txtName.getText();
                            String newVal = getAsName();
                            if (!oldVal.equals(newVal)) {
                                txtName.setText("");
                            }
                        }
                        updateDeployMethodCombo();
                    } finally {
                        isManualUpdate = true;
                    }
                }
            }
        };
    }

    private ItemListener createComboDeployListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && isManualUpdate) {
                    txtToDir.setEnabled(!comboDeploy.getSelectedItem().equals(WindowsAzureRoleComponentDeployMethod.none));
                }
            }
        };
    }

    private ItemListener createDlCheckBoxListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnableDlGrp(dlCheckBtn.isSelected());
            }
        };
    }

    private Action createAccLinkAction() {
        return new AbstractAction("Accounts...") {
            @Override
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
                String cmbName = (String) comboStrgAcc.getSelectedItem();
                String accPgName = storageAccountDialog.getSelectedValue();
                String finalNameToSet;
			    /*
			     * If row selected on preference page.
			     * set combo box to it always.
			     * Else keep combo box's previous value
			     * as it is.
			     */
                if (!(JdkSrvConfig.NONE_TXT.equals(accPgName) || JdkSrvConfig.AUTO_TXT.equals(accPgName))) {
                    finalNameToSet = accPgName;
                } else {
                    finalNameToSet = cmbName;
                }
                // update storage account combo box
                JdkSrvConfig.populateStrgAccComboBox(finalNameToSet, comboStrgAcc, null, JdkSrvConfig.AUTO_TXT.equals(finalNameToSet));
            }
        };
    }

    private ItemListener createStorageAccListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int index = comboStrgAcc.getSelectedIndex();
                    String urlTxt = txtUrl.getText();
                    String url = urlTxt.trim();
                    // check value is not none and auto.
                    if (index > 0) {
                        String newUrl = StorageAccountRegistry.getStrgList().get(index - 1).getStrgUrl();
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
                        txtUrl.setText(urlTxt);
                    }
                }
            }
        };
    }

    /**
     * Method returns As Name according to
     * import method.
     * @return
     */
    public String getAsName() {
        File file = new File(PluginUtil.convertPath(project, txtFromPath.getText()));
        return ImportExportDialogUtilMethods.getAsName(file, comboImport.getSelectedItem().toString());
    }

    /**
     * This method used for updating the import
     * method combo box values.Values gets changed on user input.
     * @param impPath import file path
     */
    private void updateImportMethodCombo(String impPath) {
        try {
            String path = impPath;
            isManualUpdate = false;
            comboImport.removeAllItems();
            comboImport.addItem(WindowsAzureRoleComponentImportMethod.copy.name());
            if (path.startsWith(BASE_PATH)) {
                path = PluginUtil.convertPath(project, path);
            }
            String nature = findSrcPathNature(path);
            // todo!
        /*if (nature.equalsIgnoreCase(message("proj"))) {
            ProjExportType type = ProjectNatureHelper.getProjectNature(ProjectNatureHelper.findProjectFromWorkSpace(path));
            comboImport.addItem(type.name());
            comboImport.setSelectedItem(type.name());
        } else*/
            if (nature.equalsIgnoreCase(message("dir"))) {
                comboImport.addItem(WindowsAzureRoleComponentImportMethod.zip.name());
                comboImport.addItem(WindowsAzureRoleComponentImportMethod.none.name());
                comboImport.setSelectedItem(WindowsAzureRoleComponentImportMethod.zip.name());
            } else if (nature.equalsIgnoreCase(message("file"))) {
                comboImport.addItem(WindowsAzureRoleComponentImportMethod.none.name());
                if (path.endsWith(".zip")) {
                    comboImport.setSelectedItem(WindowsAzureRoleComponentImportMethod.copy.name());
                } else {
                    comboImport.addItem(WindowsAzureRoleComponentImportMethod.zip);
                    comboImport.setSelectedItem(WindowsAzureRoleComponentImportMethod.copy.name());
                }
            } else {
                comboImport.addItem(WindowsAzureRoleComponentImportMethod.none.name());
                comboImport.removeItem(WindowsAzureRoleComponentImportMethod.copy.name());
                comboImport.setSelectedItem(WindowsAzureRoleComponentImportMethod.none.name());
            }
            if (isEdit
                    && (txtFromPath.getText().equalsIgnoreCase(winAzureRoleCmpnt.getImportPath())
                    || txtFromPath.getText().equalsIgnoreCase("\\."))) {
                if (winAzureRoleCmpnt.getImportMethod() != null) {
                    comboImport.setSelectedItem(winAzureRoleCmpnt.getImportMethod().name());
                }
            }
        } finally {
            isManualUpdate = true;
        }
    }


    /**
     * This method is used for updating deploy combo
     * box values.
     */
    private void updateDeployMethodCombo() {
		/* If project manager object returns
		 * From Path as empty then its .\ i.e approot
		 */
        String oldPath = "";
        if (isEdit) {
            oldPath = winAzureRoleCmpnt.getImportPath();
            if (oldPath.isEmpty()) {
                oldPath = ".\\";
            }
        }
        try {
            isManualUpdate = false;
            comboDeploy.removeAllItems();
            comboDeploy.addItem(WindowsAzureRoleComponentDeployMethod.copy);
            comboDeploy.addItem(WindowsAzureRoleComponentDeployMethod.none);
            String impTxt = (String) comboImport.getSelectedItem();
            if (impTxt.equalsIgnoreCase("EAR") || impTxt.equalsIgnoreCase("WAR") || impTxt.equalsIgnoreCase("JAR")) {
                comboDeploy.setSelectedItem(WindowsAzureRoleComponentImportMethod.copy);
            } else if (impTxt.equalsIgnoreCase(WindowsAzureRoleComponentImportMethod.zip.name())) {
                comboDeploy.addItem(WindowsAzureRoleComponentDeployMethod.unzip);
                comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.unzip);
            } else if (impTxt.equalsIgnoreCase(WindowsAzureRoleComponentImportMethod.copy.name())
                    || impTxt.equalsIgnoreCase(WindowsAzureRoleComponentImportMethod.none.name())) {
                File file = new File(PluginUtil.convertPath(project, txtFromPath.getText()));
                if (!file.getAbsolutePath().endsWith(".zip")) {
                    comboDeploy.addItem(WindowsAzureRoleComponentDeployMethod.exec);
                    comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.copy);
                } else if (file.exists() && file.isFile() && file.getAbsolutePath().endsWith(".zip")) {
                    comboDeploy.addItem(WindowsAzureRoleComponentDeployMethod.unzip);
                    comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.unzip);
                } else if (file.exists()) {
                    comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.copy);
                } else {
                    comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.none);
                }
                if (impTxt.equalsIgnoreCase(WindowsAzureRoleComponentImportMethod.none.name())) {
                    comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.none);
                }
            } else {
                comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.copy);
            }
            if (isEdit && txtFromPath.getText().equalsIgnoreCase(oldPath)) {
                if (winAzureRoleCmpnt.getDeployMethod() != null) {
                    comboDeploy.setSelectedItem(winAzureRoleCmpnt.getDeployMethod());
                } else if (winAzureRoleCmpnt.getDeployMethod() == null || winAzureRoleCmpnt.getDeployMethod().toString().isEmpty()) {
                    comboDeploy.setSelectedItem(WindowsAzureRoleComponentDeployMethod.none);
                }
            }
        } finally {
            isManualUpdate = true;
        }
    }

    /**
     * This method determines the nature of the import source.
     * @param srcPath import path
     * @return nature of resource(file,folder,project)
     */
    private String findSrcPathNature(String srcPath) {
        String path = srcPath;
        String nature = "";
        if (path.startsWith(BASE_PATH)) {
            path = PluginUtil.convertPath(project, path);
        }
        if (path.equalsIgnoreCase(".\\")) {
            return nature;
        }
        File file = new File(path);
        if (!file.exists()) {
            if (!path.isEmpty()) {
                txtName.setText("");
            }
            return "";
        } else if (file.isDirectory()) {
            Module module = PluginUtil.findModule(project, path);
            if (module == null) {
                nature = message("dir");
            } else {
                nature = message("proj");
            }
        } else {
            //consider it as file
            nature = message("file");
        }
        return nature;
    }

    /**
     * This method is used for evaluating that the project
     * is in workspace or not to populate the relative path
     * of project. If that project is in same workspace then
     * this method will return true so we have to make the path
     * relative else we have to display absolute path.
     * @param prjName project name
     * @return: true if project is in workspace else false
     */
    private boolean isWorkspaceProj(String prjName) {
        return ModuleManager.getInstance(project).findModuleByName(prjName) != null;
    }

    /**
     * Populates the corresponding values of selected
     * component for editing.
     */
    private void populateData() {
        if (winAzureRoleCmpnt.getImportPath() == null || winAzureRoleCmpnt.getImportPath().isEmpty()) {
            txtFromPath.setText(".\\");
        } else {
            txtFromPath.setText(winAzureRoleCmpnt.getImportPath());
        }
        if (winAzureRoleCmpnt.getDeployName() == null || winAzureRoleCmpnt.getDeployName().isEmpty()) {
            txtName.setText("");
        } else {
            txtName.setText(winAzureRoleCmpnt.getDeployName());
        }
        if (winAzureRoleCmpnt.getDeployDir() == null || winAzureRoleCmpnt.getDeployDir().isEmpty()) {
            txtToDir.setText(".\\");
        } else {
            txtToDir.setText(winAzureRoleCmpnt.getDeployDir());
        }
        cmpList.remove(PluginUtil.getAsName(project, winAzureRoleCmpnt.getImportPath(), winAzureRoleCmpnt.getImportMethod(),
                winAzureRoleCmpnt.getDeployName()).toLowerCase());

        updateImportMethodCombo(txtFromPath.getText());
        updateDeployMethodCombo();
        updateClouldDlGroup();
    }

    /**
     * Method populates values of deploy from download group
     * that is URL, cloud method and access key.
     */
    private void updateClouldDlGroup() {
        String url;
        try {
            url = winAzureRoleCmpnt.getCloudDownloadURL();
            if (url == null || url.isEmpty()) {
                setEnableDlGrp(false);
            } else {
                setEnableDlGrp(true);
                dlCheckBtn.setSelected(true);
                txtUrl.setText(url);
                if (winAzureRoleCmpnt.getCloudMethod() != null) {
                    switch (winAzureRoleCmpnt.getCloudMethod()) {
                        case none:
                            comboCloud.setSelectedItem(CLOUD_METHODS[0]);
                            break;
                        case  unzip:
                            comboCloud.setSelectedItem(CLOUD_METHODS[1]);
                            break;
                        default:
                            comboCloud.setSelectedItem(CLOUD_METHODS[2]);
                    }
                }
				/*
				 * Find storage account name
				 * associated with the component's access key
				 * and populate it.
				 */
                String accessKey = winAzureRoleCmpnt.getCloudKey();
                UIUtils.populateStrgNameAsPerKey(accessKey, comboStrgAcc);
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("urlKeyGetErMsg"));
        }
    }

    /**
     * Enable or disable components of
     * download group according to status.
     * @param status
     */
    private void setEnableDlGrp(boolean status) {
        txtUrl.setEnabled(status);
        comboCloud.setEnabled(status);
        lblStrgAcc.setEnabled(status);
        comboStrgAcc.setEnabled(status);
        lblDlMethod.setEnabled(status);
        lblUrl.setEnabled(status);
        if (!status) {
            txtUrl.setText("");
            comboStrgAcc.removeAllItems();
            comboCloud.removeAllItems();
        } else {
            comboCloud.setModel(new DefaultComboBoxModel(CLOUD_METHODS));
            JdkSrvConfig.populateStrgAccComboBox("", comboStrgAcc, null, false);
        }
    }

    /**
     * This method validated the data entered by user.
     * @return true if all data is valid else return false
     */
    private boolean validateData() {
        boolean isValidPath = true;
        String path = txtFromPath.getText();
        if ((path.isEmpty()|| txtFromPath.getText().equalsIgnoreCase(".\\")) && txtName.getText().isEmpty()) {
            isValidPath = false;
            PluginUtil.displayErrorDialog(message("impExpErrTtl"), message("impExpErrMsg"));
            return isValidPath;
        } else if (path.isEmpty() && !txtName.getText().isEmpty()) {
            isValidPath = true;
        }
        if (path.startsWith(BASE_PATH)) {
            path = PluginUtil.convertPath(project, path);
        }
        File file = new File(path);
        if (!path.isEmpty() && !file.exists()) {
            isValidPath = false;
            PluginUtil.displayErrorDialog(message("dlgImpInvPthTtl"), message("dlgImpInvPthMsg"));
            return isValidPath;
        }
        boolean isvalidname = false;

        WindowsAzureRoleComponentImportMethod newMethod = null;
        // if UI shows import method as war, jar or ear then internally its auto
        if (comboImport.getSelectedItem().toString().equalsIgnoreCase("WAR")
                || comboImport.getSelectedItem().toString().equalsIgnoreCase("JAR")
                || comboImport.getSelectedItem().toString().equalsIgnoreCase("EAR")) {
            newMethod = WindowsAzureRoleComponentImportMethod.auto;
        } else {
            newMethod = WindowsAzureRoleComponentImportMethod.valueOf(comboImport.getSelectedItem().toString());
        }
        String name = PluginUtil.getAsName(project, txtFromPath.getText(), newMethod, txtName.getText());

        if ((isEdit) && (winAzureRoleCmpnt.getDeployName().equalsIgnoreCase(name))) {
            isvalidname = true;
        } else {
            try {
                isvalidname = waRole.isValidDeployName(name);
            } catch (Exception e) {
                isvalidname = false;
            }
        }
        if (!isvalidname) {
            PluginUtil.displayErrorDialog(message("dlgImpInvDplTtl"), message("dlgImpInvDplMsg"));
        }
        boolean isValidDlGrp = validateDlGroup();

        return isValidPath && isvalidname && isValidDlGrp;
    }

    /**
     * Method validates URL and access key given
     * for deploy from download group.
     * @return
     */
    private boolean validateDlGroup() {
        boolean isValidUrl = ImportExportDialogUtilMethods.validateDlGroup(dlCheckBtn.isSelected(), txtUrl.getText().trim());
        if (!isValidUrl) {
            PluginUtil.displayErrorDialog(message("dlgDlUrlErrTtl"), message("dlgDlUrlErrMsg"));
        }
        return isValidUrl;
    }

    @Override
    protected void doOKAction() {
        if (validateData()) {
            try {
                String newMethod = comboImport.getSelectedItem().toString();
                // if UI shows import method as war, jar or ear then internally its auto
                if (newMethod.equalsIgnoreCase("WAR") || newMethod.equalsIgnoreCase("JAR") || newMethod.equalsIgnoreCase("EAR")) {
                    newMethod = "auto";
                }
                if (isEdit) {
					/* If project manager object returns
					 * import method as null then in UI its none
					 * and if From Path as empty then its .\ i.e approot
					 */
                    WindowsAzureRoleComponentImportMethod oldImpMethod = winAzureRoleCmpnt.getImportMethod();
                    if (oldImpMethod == null) {
                        oldImpMethod = WindowsAzureRoleComponentImportMethod.none;
                    }
                    String oldPath = winAzureRoleCmpnt.getImportPath();
                    if (oldPath.isEmpty()) {
                        oldPath = ".\\";
                    }
					/* To get exported component's As name using getAsName method
					 * when component has empty As name
					 */
                    String oldAsName = winAzureRoleCmpnt.getDeployName();
                    if (oldAsName.isEmpty()) {
                        oldAsName = PluginUtil.getAsName(project, oldPath, oldImpMethod, oldAsName);
                    }
                    String modulePath = PluginUtil.getModulePath(ModuleManager.getInstance(project).findModuleByName(waProjManager.getProjectName()));
					/* if import method or from path is changed
					 * then delete exported cmpnt file from approot
					 */
                    if (!newMethod.equalsIgnoreCase(oldImpMethod.name()) || !txtFromPath.getText().equalsIgnoreCase(oldPath)) {
                        String cmpntPath = String.format("%s%s%s%s%s", modulePath, File.separator, waRole.getName(), message("approot"), oldAsName);
                        File file = new File(cmpntPath);
                        if (file.exists()) {
                            if (file.isFile()) {
                                file.delete();
                            } else if (file.isDirectory()) {
                                WAEclipseHelperMethods.deleteDirectory(file);
                            }
                        }
                    }
					/* if import as is changed while import method
					 * and from path is same
					 * then rename exported cmpnt file from approot
					 */
                    if (!txtName.getText().equalsIgnoreCase(winAzureRoleCmpnt.getDeployName())) {
                        String cmpntPath = String.format("%s%s%s%s%s", modulePath, File.separator, waRole.getName(), message("approot"), oldAsName);
                        File file = new File(cmpntPath);
                        if (file.exists()) {
                            String dest = String.format("%s%s%s%s%s", modulePath, File.separator, waRole.getName(), message("approot"), txtName.getText());
                            file.renameTo(new File(dest));
                        }
                    }
                } else {
                    // Error if duplicate file component entry is added
                    if (cmpList.contains(PluginUtil.getAsName(project, txtFromPath.getText(),
                            WindowsAzureRoleComponentImportMethod.valueOf(newMethod),
                            txtName.getText()).toLowerCase())) {
                        PluginUtil.displayErrorDialog(message("dlgImpInvDplTtl"), message("dlgImpInvDplMsg"));
                        return;
                    }
                    if (!txtName.getText().isEmpty()) {
                        winAzureRoleCmpnt = waRole.addComponent("importas", txtName.getText().trim());
                    } else {
                        winAzureRoleCmpnt = waRole.addComponent("importsrc", txtFromPath.getText().trim());
                    }
                }
                modified = true;
                winAzureRoleCmpnt = ImportExportDialogUtilMethods.okPressedPart2(
                        winAzureRoleCmpnt,
                        txtToDir.getText().trim(),
                        comboDeploy.getSelectedItem().toString(),
                        txtName.getText().trim(),
                        (String) comboImport.getSelectedItem(),
                        txtUrl.getText().trim(),
                        CLOUD_METHODS,
                        comboCloud.getSelectedItem() == null ? "" : (String) comboCloud.getSelectedItem(),
                        AzureWizardModel.getAccessKey(comboStrgAcc));

                if (!txtFromPath.getText().startsWith(BASE_PATH) && txtFromPath.getText().contains(project.getBasePath())) {
                    String projectPath = project.getBasePath();
                    String replaceString = txtFromPath.getText().trim();
                    String subString = txtFromPath.getText().substring(txtFromPath.getText().indexOf(projectPath), projectPath.length());
                    txtFromPath.setText(replaceString.replace(subString, BASE_PATH));
                }
                if (!txtFromPath.getText().isEmpty() || !txtFromPath.getText().equalsIgnoreCase("\\.")) {
                    winAzureRoleCmpnt.setImportPath(txtFromPath.getText().trim());
                }
                super.doOKAction();
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("cmpntSetErrTtl"), message("cmpntRmvErrMsg"), e);
            }
        }
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("dlgImpTtl"), message("dlgImpMsg"));
    }

    public boolean isModified() {
        return modified;
    }

    protected String getHelpId() {
        return "windows_azure_importexport_dialog";
    }
}
