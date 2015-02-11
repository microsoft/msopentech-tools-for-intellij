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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

public class AzurePopupGroup extends DefaultActionGroup implements DumbAware {

    public void update(AnActionEvent e) {
        Module module = LangDataKeys.MODULE.getData(e.getDataContext());
        if (module == null) {
            e.getPresentation().setEnabledAndVisible(false);
        } else {
            VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
            e.getPresentation().setEnabledAndVisible(PluginUtil.isModuleRoot(selectedFile, module) &&
                    AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))
                    || PluginUtil.isRoleFolder(selectedFile, module)/* ||
                    ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))*/);
        }
    }
}
