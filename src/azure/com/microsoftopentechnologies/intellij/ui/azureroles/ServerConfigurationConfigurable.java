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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.ui.JdkServerPanel;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.util.WAEclipseHelperMethods;
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
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        this.module = module;
        jdkServerPanel = new JdkServerPanel(module.getProject(), waRole, waProjManager);
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
            List<String> fileToDel = jdkServerPanel.getFileToDel();
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
        String jdkDirName = new File(jdkServerPanel.getFinalJdkPath()).getName();
        String modulePath = PluginUtil.getModulePath(module);
        jdkPath = String.format("%s%s%s%s%s", modulePath, File.separator, waRole.getName(), message("approot"), jdkDirName);
        File jdkFile = new File(jdkPath);
        if (jdkFile.exists()) {
            WAEclipseHelperMethods.deleteDirectory(jdkFile);
            LocalFileSystem.getInstance().findFileByPath(modulePath).refresh(true, true);
        }
    }

    /**
     *  To delete zip file or directory of
     *  server which is present inside approot
     *  if server name or source path is modified.
     */
    private void deleteServerFile() {
        String modulePath = PluginUtil.getModulePath(module);
        File srvFile = new File(String.format("%s%s%s%s%s", modulePath, File.separator, waRole.getName(), message("approot"),
                PluginUtil.getAsName(module.getProject(), jdkServerPanel.getFinalSrvPath(), jdkServerPanel.getFinalImpMethod(), jdkServerPanel.getFinalAsName())));
        if (srvFile.exists()) {
            if (srvFile.isFile()) {
                srvFile.delete();
            } else if (srvFile.isDirectory()) {
                WAEclipseHelperMethods.deleteDirectory(srvFile);
            }
            LocalFileSystem.getInstance().findFileByPath(modulePath).refresh(true, true);
        }
    }

    @Override
    public void reset() {
        jdkServerPanel.setModified(false);
    }

    @Override
    public void disposeUIResources() {
    }
}
