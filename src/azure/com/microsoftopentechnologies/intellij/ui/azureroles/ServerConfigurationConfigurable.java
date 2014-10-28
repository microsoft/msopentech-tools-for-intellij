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
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.ui.JdkServerPanel;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class ServerConfigurationConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private JdkServerPanel jdkServerPanel;

    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private Module module;

    public ServerConfigurationConfigurable(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        jdkServerPanel = new JdkServerPanel(module.getProject(), waRole, waProjManager);
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        this.module = module;
    }

    @NotNull
    @Override
    public String getId() {
        return getDisplayName();
    }

    public boolean isModified() {
        return jdkServerPanel.isModified();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return message("cmhLblSrvCnfg");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jdkServerPanel.getPanel(), BorderLayout.NORTH);
        return panel;
    }

    @Override
    public void apply() throws ConfigurationException {
        jdkServerPanel.apply();
        try {
            waProjManager.save();
                    /*
					 * Delete files from approot,
					 * whose entry from component table is removed.
					 */
            List<String> fileToDel = jdkServerPanel.getApplicationsTab().getFileToDel();
            if (!fileToDel.isEmpty()) {
                for (int i = 0; i < fileToDel.size(); i++) {
                    String str = fileToDel.get(i);
                    if (str.equalsIgnoreCase("jdk")) {
                        deleteJdkDir();
                    } else if (str.equalsIgnoreCase("srv")) {
                        deleteServerFile();
                    } else {
                        File file = new File(str);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                }
            }
            fileToDel.clear();
            jdkServerPanel.setModified(false);
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module) + File.separator + message("resCLPkgXML")).refresh(true, false);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            throw new ConfigurationException(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"));
        }
    }

    /**
     * To delete jdk directory which is present inside approot
     * if JDK source path is modified.
     */
    private void deleteJdkDir() {
        String jdkPath = "";
//        try {
//            String jdkDirName = new File(finalJdkPath).getName();
//            jdkPath = String.format("%s%s%s%s%s",
//                    root.getProject(waProjManager.getProjectName()).getLocation(),
//                    File.separator, waRole.getName(), Messages.approot, jdkDirName);
//        } catch (WindowsAzureInvalidProjectOperationException e1) {
//            PluginUtil.displayErrorDialogAndLog(message("jdkPathErrTtl"), message("jdkDirErrMsg"), e1);
//        }
//        File jdkFile = new File(jdkPath);
//        if (jdkFile.exists()) {
//            WAEclipseHelperMethods.deleteDirectory(jdkFile);
////            WAEclipseHelper.refreshWorkspace(
////                    Messages.rfrshErrTtl,
////                    Messages.rfrshErrMsg);
//        }
    }

    /**
     *  To delete zip file or directory of
     *  server which is present inside approot
     *  if server name or source path is modified.
     */
    private void deleteServerFile() {
        File srvFile = null;
//        try {
//            srvFile = new File(String.format("%s%s%s%s%s",
//                    root.getProject(waProjManager.
//                            getProjectName()).getLocation(),
//                    File.separator, windowsAzureRole.getName(),
//                    Messages.approot, ProjectNatureHelper.
//                            getAsName(finalSrvPath, finalImpMethod, finalAsName)));
//        } catch (WindowsAzureInvalidProjectOperationException e) {
//            PluginUtil.displayErrorDialogAndLog(
//                    getShell(),
//                    Messages.genErrTitle,
//                    Messages.srvFileErr, e);
//        }
//        if (srvFile.exists()) {
//            if (srvFile.isFile()) {
//                srvFile.delete();
//            } else if (srvFile.isDirectory()) {
//                WAEclipseHelperMethods.deleteDirectory(srvFile);
//            }
//            WAEclipseHelper.refreshWorkspace(
//                    Messages.rfrshErrTtl,
//                    Messages.rfrshErrMsg);
//        }
    }

    @Override
    public void reset() {
        jdkServerPanel.setModified(false);
    }

    @Override
    public void disposeUIResources() {
    }
}
