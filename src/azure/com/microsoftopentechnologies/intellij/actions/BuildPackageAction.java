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
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoftopentechnologies.azurecommons.wacommonutil.PreferenceSetUtil;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.util.AntHelper;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.wacommon.utils.WACommonException;

import java.io.File;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class BuildPackageAction extends AnAction {

    public void actionPerformed(AnActionEvent event) {
        // Get selected WA module
        final Module module = event.getData(LangDataKeys.MODULE);
        final String modulePath = PluginUtil.getModulePath(module);
        try {
            WindowsAzureProjectManager waProjManager = WindowsAzureProjectManager.load(new File(modulePath));

            if (waProjManager.getPackageType().equals(WindowsAzurePackageType.LOCAL)) {
                waProjManager.setPackageType(WindowsAzurePackageType.CLOUD);
            }
            try {
                String pluginInstLoc = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.PLUGIN_ID);
                String prefFile = String.format("%s%s%s", pluginInstLoc, File.separator, AzureBundle.message("prefFileName"));
                String prefSetUrl = PreferenceSetUtil.getSelectedPortalURL(PreferenceSetUtil.getSelectedPreferenceSetName(prefFile), prefFile);
				/*
				 * Don't check if URL is empty or null.
				 * As if it is then we remove "portalurl" attribute
				 * from package.xml.
				 */
                waProjManager.setPortalURL(prefSetUrl);
            } catch (WACommonException e1) {
                PluginUtil.displayErrorDialog(message("errTtl"), message("getPrefUrlErMsg"));
            }
            waProjManager.save();
            WindowsAzureProjectManager.load(new File(modulePath)); // to verify correctness

            AntHelper.runAntBuild(event.getDataContext(), module, AntHelper.createBuildPackageListener(module));
        } catch (final Exception e) {
            PluginUtil.displayErrorDialogInAWTAndLog(message("bldCldErrTtl"), String.format("%s %s", message("bldCldErrMsg"), module.getName()), e);
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        event.getPresentation().setEnabled(module != null && AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
