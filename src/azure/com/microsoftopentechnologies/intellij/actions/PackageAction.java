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

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.PlatformUtils;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.AzureWizardDialog;
import com.microsoftopentechnologies.intellij.ui.AzureWizardModel;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class PackageAction extends AnAction {

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT);
        if (checkSdk()) {
            AzureWizardModel model = new AzureWizardModel(project, createWaProjMgr());
            AzureWizardDialog wizard = new AzureWizardDialog(model);

            final String title = AzureBundle.message("pWizWindowTitle");
            wizard.setTitle(title);
            wizard.show();
            if (wizard.isOK()) {
//            FileContentUtil.reparseFiles();
            }
        }
    }

    private boolean checkSdk() {
        String sdkPath = null;
        if (AzurePlugin.IS_WINDOWS) {
            try {
                sdkPath = WindowsAzureProjectManager.getLatestAzureSdkDir();
            } catch (IOException e) {
                log(message("error"), e);
            }
            try {
                if (sdkPath == null) {
                    int choice = Messages.showOkCancelDialog(message("sdkInsErrMsg"), message("sdkInsErrTtl"), Messages.getQuestionIcon());
                    if (choice == Messages.OK) {
                        Desktop.getDesktop().browse(URI.create(message("sdkInsUrl")));
                    }
                    return false;
                }
            } catch (Exception ex) {
                // only logging the error in log file not showing anything to
                // end user
                log(message("error"), ex);
                return false;
            }
        } else {
            log("Not Windows OS, skipping getSDK");
        }
        return true;
    }

    private WindowsAzureProjectManager createWaProjMgr() {
        WindowsAzureProjectManager waProjMgr = null;
        try {
            String zipFile = String.format("%s%s%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.PLUGIN_ID, File.separator,
                    message("starterKitFileName"));

            //Extract the WAStarterKitForJava.zip to temp dir
            waProjMgr = WindowsAzureProjectManager.create(zipFile);
            //	By deafult - disabling remote access
            //  when creating new project
            waProjMgr.setRemoteAccessAllRoles(false);
            waProjMgr.setClassPathInPackage("azure.lib.dir", PluginUtil.getAzureLibLocation());
            WindowsAzureRole waRole = waProjMgr.getRoles().get(0);
            // remove http endpoint
            waRole.getEndpoint(AzureBundle.message("httpEp")).delete();
        } catch (IOException e) {
            PluginUtil.displayErrorDialogAndLog(AzureBundle.message("pWizErrTitle"), AzureBundle.message("pWizErrMsg"), e);
        } catch (Exception e) {
            String errorTitle = AzureBundle.message("adRolErrTitle");
            String errorMessage = AzureBundle.message("pWizErrMsgBox1") + AzureBundle.message("pWizErrMsgBox2");
            PluginUtil.displayErrorDialogAndLog(errorTitle, errorMessage, e);
        }
        return waProjMgr;
    }

    public void update(AnActionEvent event) {
        event.getPresentation().setVisible(PlatformUtils.isIdeaUltimate() || ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace()));
    }
}
