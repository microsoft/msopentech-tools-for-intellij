/**
 * Copyright 2015 Microsoft Open Technologies, Inc.
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
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.ui.azureroles.RoleConfigurablesGroup;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.intellij.util.WAHelper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AddRoleAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            // Get selected module
            final Module module = event.getData(LangDataKeys.MODULE);
            final String modulePath = PluginUtil.getModulePath(module);
            WindowsAzureProjectManager waProjManager = WindowsAzureProjectManager.load(new File(modulePath));
            List<WindowsAzureRole> listRoles = waProjManager.getRoles();
            WindowsAzureRole windowsAzureRole = WAHelper.prepareRoleToAdd(waProjManager);
            RoleConfigurablesGroup group = new RoleConfigurablesGroup(module, waProjManager, windowsAzureRole, true);
            ShowSettingsUtil.getInstance().showSettingsDialog(module.getProject(), new ConfigurableGroup[]{group});
            if (group.isModified()) { // Cancel was clicked, so changes should be reverted
                listRoles.remove(windowsAzureRole);
            }
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("rolsDlgErr"), message("rolsDlgErrMsg"), ex);
        }
    }

    public void update(@NotNull AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        event.getPresentation().setEnabled(module != null && AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
