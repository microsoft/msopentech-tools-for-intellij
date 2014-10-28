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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.PlatformUtils;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.interopbridges.tools.windowsazure.WindowsAzureRoleComponent;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.AppCmpntParam;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AddApplicationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JRadioButton fileRadioBtn;
    private JRadioButton projRadioBtn;
    private JComboBox projCombo;
    private JTextField asNameTxt;
    private JLabel nameLbl;
    private TextFieldWithBrowseButton fileTxt;

    private Project myProject;
    private WindowsAzureRole waRole;
    private AppCmpntParam appCmpntParam;
    private ArrayList<String> cmpList;

    public AddApplicationDialog(Project project, WindowsAzureRole waRole, ArrayList<String> cmpList) {
        super(true);
        myProject = project;
        this.waRole = waRole;
        this.cmpList = cmpList;
        init();
    }

    @Override
    protected void init() {
        setTitle(message("appDlgTxt"));
        fileTxt.addActionListener(UIUtils.createFileChooserListener(fileTxt, myProject, FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        fileRadioBtn.addItemListener(createFileBtnListener());
//        if (PlatformUtils.isIdeaUltimate()) {
//            projRadioBtn.addItemListener(createProjectBtnListener());
//        } else {
            projRadioBtn.setEnabled(false);
//        }
        super.init();
    }

    private ItemListener createFileBtnListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                /*
            	 * If user is again selecting File radio button
            	 * which was already selected,
            	 * and some file path was present in text box
            	 * then do not make text empty and keep OK enabled.
            	 */
                if (fileRadioBtn.isSelected() && !fileTxt.getText().equals("")) {
//                    getOKAction().setEnabled(true);
                } else {
                    projCombo.setEnabled(false);
                    asNameTxt.setText("");
                    asNameTxt.setEnabled(false);
                    nameLbl.setEnabled(false);
                    fileTxt.setEnabled(true);
                    fileTxt.setText("");
//                    getOKAction().setEnabled(false);
                }
            }
        };
    }

    private ItemListener createProjectBtnListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                /*
            	 * If user is again selecting Project radio button
            	 * which was already selected,
            	 * and some value present in combo box and text box
            	 * then do not make text empty and keep OK enabled.
            	 */
                if (projRadioBtn.isSelected() && !(projCombo.getSelectedItem() == null) && !asNameTxt.getText().equals("")) {
//                    getOKAction().setEnabled(true);
                } else {
                    fileTxt.setEnabled(false);
                    projCombo.setEnabled(true);
                    asNameTxt.setEnabled(true);
                    asNameTxt.setText("");
                    nameLbl.setEnabled(true);
                    projCombo.setModel(new DefaultComboBoxModel(getModules()));
                    projCombo.setSelectedItem(null);
                    projCombo.addItemListener(createProjComboListener());
//                    getOKAction().setEnabled(false);
                }
            }
        };
    }

    private ItemListener createProjComboListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
//        if (projCombo.getText().isEmpty() || asNameTxt.getText().isEmpty()) {
//            if (okButton != null) {
//                okButton.setEnabled(false);
//            } else {
//                if (okButton != null) {
//                    okButton.setEnabled(true);
//                }
//            }
//        }
//                Module module = ModuleManager.getInstance(myProject).findModuleByName(projCombo.getSelectedItem().toString());
//                asNameTxt.setText(getAsNameFrmProject(module.getModuleFilePath()));
                if (projCombo.getSelectedItem() != null) {
                    asNameTxt.setText(projCombo.getSelectedItem().toString().concat(".war"));
                }
            }
        };
    }

    /**
     * Method returns array of project names
     * which are open and has azure nature.
     * @return String[]
     */
    private String[] getModules() {
        String[] projectModules = null;
        try {
            Module[] modules = ModuleManager.getInstance(myProject).getModules();
            ArrayList<String> projList = new ArrayList<String>();
            for (Module module : modules) {
                if (!AzureModuleType.isAzureModule(module)) {
                    projList.add(module.getName());
                }
            }
            projectModules = new String[projList.size()];
            projectModules = projList.toArray(projectModules);
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("projSelTtl"), message("projSelMsg"), e);
        }
        return projectModules;
    }

    /**
     * Method checks type of project
     * and returns corresponding as name with
     * proper extension.
     * @param path
     * @return String
     */
//    private String getAsNameFrmProject(String path) {
//        String name = "";
//        ProjExportType type = ProjectNatureHelper.getProjectNature(getProjectFrmPath(path));
//        File file = new File(path);
//        if (file.exists() && file.isDirectory()) {
//            name = file.getName();
//            if (type.equals(ProjExportType.EAR)) {
//                name = name.concat(".ear");
//            } else if (type.equals(ProjExportType.WAR)) {
//                name = name.concat(".war");
//            } else {
//                name = name.concat(".jar");
//            }
//        }
//        File file = new File(path);
//        if (file.exists() && file.isDirectory()) {
//            name = file.getName().concat(".war");
//        }
//        return name;
//    }
    @Override
    protected void doOKAction() {
        if (fileRadioBtn.isSelected()) {
            if (cmpList.contains(new File(fileTxt.getText()).getName())) {
                PluginUtil.displayErrorDialog(message("appDlgDupNmeTtl"), message("appDlgDupNmeMsg"));
                return;
            }
            try {
                List<WindowsAzureRoleComponent> components = waRole.getComponents();
                for (int i = 0; i < components.size(); i++) {
                    if (components.get(i).getDeployName().equalsIgnoreCase(new File(fileTxt.getText()).getName())) {
                        PluginUtil.displayErrorDialog(message("appDlgDupNmeTtl"), message("appDlgDupNmeMsg"));
                        return;
                    }
                }
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("addAppErrTtl"), message("addAppErrMsg"), e);
            }
            initAppCpmntParam(fileTxt.getText(), new File(fileTxt.getText()).getName(), message("methodCopy"));
        } else if (projRadioBtn.isSelected()) {
//                IWorkspace workspace = ResourcesPlugin.getWorkspace();
//                IWorkspaceRoot root = workspace.getRoot();
//                IProject proj = root.getProject(projCombo.getText());
//                if (cmpList.contains(new File(asNameTxt.getText()).getName())) {
//                    PluginUtil.displayErrorDialog(this.getShell(),
//                            Messages.appDlgDupNmeTtl,
//                            Messages.appDlgDupNmeMsg);
//                    return;
//                }
//                if (depPage == null) {
//                    serverConf.addToAppList(proj.getLocation().toOSString(),
//                            asNameTxt.getText(),
//                            Messages.methodAuto);
//                } else {
//                    depPage.addToAppList(proj.getLocation().toOSString(),
//                            asNameTxt.getText(),
//                            Messages.methodAuto);
//                }
        }
        super.doOKAction();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (fileRadioBtn.isSelected()) {
            if (fileTxt.getText().isEmpty()) {
                return new ValidationInfo(message("appDlgInvFileTtl"), fileTxt);
            } else {
                File file = new File(fileTxt.getText());
                if (!file.exists()) {
                    return new ValidationInfo(message("appDlgInvFileMsg"), fileTxt);
                }
            }
        } else if (projRadioBtn.isSelected() && !asNameTxt.getText().isEmpty() && !(projCombo.getSelectedItem() == null)) {
            boolean isValidName;
            try {
                isValidName = waRole.isValidDeployName(asNameTxt.getText());
            } catch (Exception e) {
                isValidName = false;
            }
            if (!isValidName) {
                return new ValidationInfo(message("appDlgInvNmeMsg"), asNameTxt);
            }
        }
        return null;
    }

    /**
     * Adds the application to the application list.
     * @param src : import source location
     * @param name : import as name
     * @param method : import method
     */
    public void initAppCpmntParam(String src, String name, String method) {
        appCmpntParam = new AppCmpntParam();
        appCmpntParam.setImpSrc(src);
        appCmpntParam.setImpAs(name);
        appCmpntParam.setImpMethod(method);
    }

    public AppCmpntParam getAppCmpntParam() {
        return appCmpntParam;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("appDlgTxt"), message("appDlgMsg"));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
