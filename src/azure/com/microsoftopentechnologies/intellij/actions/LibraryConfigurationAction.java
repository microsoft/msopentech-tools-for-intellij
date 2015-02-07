/**
 * Copyright 2015 Microsoft Open Technologies Inc.
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
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.roots.ModuleRootManager;
import com.microsoftopentechnologies.intellij.ui.libraries.*;

import java.util.ArrayList;
import java.util.List;

public class LibraryConfigurationAction extends AnAction {
    public static AzureLibrary ACS_FILTER = new AzureLibrary("Azure Access Control Services Filter (by MS Open Tech)",
            "ACSAuthFilter.jar");
    static AzureLibrary QPID_CLIENT = new AzureLibrary("Package for Apache Qpid Client Libraries for JMS (by MS Open Tech)",
            "com.microsoftopentechnologies.qpid-0.19.0-SNAPSHOT");
    static AzureLibrary AZURE_LIBRARIES = new AzureLibrary("Package for Microsoft Azure Libraries for Java (by MS Open Tech)",
            "com.microsoftopentechnologies.windowsazure.tools.sdk");
    public static AzureLibrary[] LIBRARIES = new AzureLibrary[]{ACS_FILTER, QPID_CLIENT, AZURE_LIBRARIES};

    public void actionPerformed(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        List<AzureLibrary> currentLibs = new ArrayList<AzureLibrary>();
        for (AzureLibrary azureLibrary : LIBRARIES) {
            if (ModuleRootManager.getInstance(module).getModifiableModel().getModuleLibraryTable().getLibraryByName(azureLibrary.getName()) != null) {
                currentLibs.add(azureLibrary);
            }
        }
        LibrariesConfigurationDialog configurationDialog = new LibrariesConfigurationDialog(module, currentLibs);
        configurationDialog.show();
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        event.getPresentation().setEnabledAndVisible(module != null && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
