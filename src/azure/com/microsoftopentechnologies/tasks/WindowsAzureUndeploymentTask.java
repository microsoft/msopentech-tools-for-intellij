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
package com.microsoftopentechnologies.tasks;

import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventListener;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.intellij.deploy.DeploymentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.wacommon.utils.WACommonException;
import org.jetbrains.annotations.NotNull;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class WindowsAzureUndeploymentTask extends Task.Backgroundable {
    private String serviceName;
    private String deploymentName;
    private String deploymentState;

    public WindowsAzureUndeploymentTask(Module module, String serviceName, String deploymentName, String deploymentState) {
        super(module.getProject(), message("deployingToAzure"), true, Backgroundable.DEAF);
        this.serviceName = serviceName;
        this.deploymentName = deploymentName;
        this.deploymentState = deploymentState;
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
//        MessageConsole console = Activator.findConsole(Activator.CONSOLE_NAME);

//        console.clearConsole();

//        final MessageConsoleStream out = console.newMessageStream();

        AzurePlugin.removeUnNecessaryListener();
        DeploymentEventListener undeployListnr = new DeploymentEventListener() {
            @Override
            public void onDeploymentStep(DeploymentEventArgs args) {
                indicator.setFraction(indicator.getFraction() + args.getDeployCompleteness() / 100.0);
                indicator.setText(message("undeployWizTitle"));
                indicator.setText2(args.toString());
            }
        };
        AzurePlugin.addDeploymentEventListener(undeployListnr);
        AzurePlugin.depEveList.add(undeployListnr);

        try {
            DeploymentManager.getInstance().undeploy(serviceName, deploymentName,deploymentState);
        }
        catch (RestAPIException e) {
            log(message("error"), e);
        } catch (InterruptedException e) {
            log(message("error"), e);
        } catch (WACommonException e) {
            log(message("error"), e);
            e.printStackTrace();
        }
    }
}
