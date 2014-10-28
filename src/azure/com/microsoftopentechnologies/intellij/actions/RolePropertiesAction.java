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
package com.microsoftopentechnologies.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.ui.azureroles.RoleConfigurablesGroup;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

import java.io.File;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class RolePropertiesAction extends AnAction {

    public void actionPerformed(AnActionEvent event) {
        // Get selected WA module
        final Module module = event.getData(LangDataKeys.MODULE);
        WindowsAzureProjectManager projMngr;
        try {
            VirtualFile vFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
            projMngr = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(module)));
            WindowsAzureRole role = projMngr.roleFromPath(new File(vFile.getPath()));
            if (role != null) {
                ShowSettingsUtil.getInstance().showSettingsDialog(module.getProject(),
                        new ConfigurableGroup[] {new RoleConfigurablesGroup(module, projMngr, role, false)} );
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(message("error"), e);
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        event.getPresentation().setVisible(module != null && PluginUtil.isRoleFolder(selectedFile, module));
    }
}
