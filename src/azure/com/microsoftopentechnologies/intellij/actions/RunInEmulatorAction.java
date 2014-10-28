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

import com.intellij.lang.ant.config.AntBuildFile;
import com.intellij.lang.ant.config.AntBuildFileBase;
import com.intellij.lang.ant.config.AntBuildListener;
import com.intellij.lang.ant.config.AntConfiguration;
import com.intellij.lang.ant.config.execution.ExecutionHandler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

import java.io.File;
import java.util.ArrayList;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class RunInEmulatorAction extends AnAction {
    private String errorTitle;
    private String errorMessage;

    public void actionPerformed(final AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        if (module == null || !AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
            return;
        }
        try {
            final String modulePath = PluginUtil.getModulePath(module);
            WindowsAzureProjectManager waProjManager = WindowsAzureProjectManager.load(new File(modulePath));

            if (waProjManager.getPackageType().equals(WindowsAzurePackageType.CLOUD)) {
                waProjManager.setPackageType(WindowsAzurePackageType.LOCAL);
            }
            waProjManager.save();
            try {
                final WindowsAzureProjectManager waProjMgr = WindowsAzureProjectManager.load(new File(modulePath));
                PluginUtil.runAntBuild(event.getDataContext(), module, new AntBuildListener() {
                    @Override
                    public void buildFinished(int state, int errorCount) {
                        if (state == AntBuildListener.FINISHED_SUCCESSFULLY) {
                            try {
                                waProjMgr.deployToEmulator();
                            } catch (WindowsAzureInvalidProjectOperationException e) {
                                errorTitle = String.format("%s%s%s", message("waEmulator"), " ", message("runEmltrErrTtl"));
                                errorMessage = String.format("%s %s%s%s", message("runEmltrErrMsg"), module.getName(), " in ", message("waEmulator"));
                                PluginUtil.displayErrorDialogInAWTAndLog(errorTitle, errorMessage, e);
                            }
                        } else if (state == AntBuildListener.FAILED_TO_RUN) {
                            PluginUtil.displayErrorDialog(message("bldErrTtl"), message("bldErrMsg"));
                        }
                    }
                });
            } catch (WindowsAzureInvalidProjectOperationException e) {
                errorTitle = String.format("%s%s%s", message("waEmulator"), " ", message("runEmltrErrTtl"));
                errorMessage = String.format("%s %s%s%s", message("runEmltrErrMsg"), module.getName(), " in ", message("waEmulator"));
                PluginUtil.displayErrorDialogInAWTAndLog(errorTitle, errorMessage, e);
            } catch (Exception ex) {
                errorTitle = message("bldErrTtl");
                errorMessage = message("bldErrMsg");
                PluginUtil.displayErrorDialogInAWTAndLog(errorTitle, errorMessage, ex);
            }
        } catch (Exception e) {
            errorTitle = String.format("%s%s%s", message("waEmulator"), " ", message("runEmltrErrTtl"));
            errorMessage = String.format("%s %s%s%s", message("runEmltrErrMsg"), module.getName(), " in ", message("waEmulator"));
            PluginUtil.displayErrorDialogAndLog(errorTitle, errorMessage, e);
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        event.getPresentation().setEnabled(module != null && AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}