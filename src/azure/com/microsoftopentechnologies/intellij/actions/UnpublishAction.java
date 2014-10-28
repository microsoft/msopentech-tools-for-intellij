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
import com.intellij.openapi.module.Module;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.ui.UndeployWizardDialog;
import com.microsoftopentechnologies.tasks.WindowsAzureUndeploymentTask;
import com.microsoft.windowsazure.management.compute.models.HostedServiceGetDetailedResponse.Deployment;


public class UnpublishAction extends AnAction {
    public void actionPerformed(AnActionEvent event) {
        Module module = event.getData(LangDataKeys.MODULE);
        UndeployWizardDialog deployDialog = new UndeployWizardDialog(module);
        deployDialog.show();
        if (deployDialog.isOK()) {
            Deployment deployment = deployDialog.getDeployment();
            WindowsAzureUndeploymentTask undeploymentTask =
                    new WindowsAzureUndeploymentTask(module, deployDialog.getServiceName(), deployment.getName(), deployment.getDeploymentSlot().toString());
            undeploymentTask.queue();
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        event.getPresentation().setEnabled(module != null && AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
