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
package com.microsoftopentechnologies.intellij.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.microsoftopentechnologies.intellij.ui.RolesPanel;
import com.microsoftopentechnologies.intellij.ui.SubscriptionsPanel;
import com.microsoftopentechnologies.intellij.ui.WARemoteAccessPanel;

public class AzureModuleConfigurationEditorProvider implements ModuleConfigurationEditorProvider {
    @Override
    public ModuleConfigurationEditor[] createEditors(final ModuleConfigurationState state) {
        Module module = state.getRootModel().getModule();
        if (!AzureModuleType.isAzureModule(module)) {
            return ModuleConfigurationEditor.EMPTY;
        }
        return new ModuleConfigurationEditor[]{new ModuleEditor(state, new AzureModulePanel(state.getRootModel().getModule())),
                new ModuleEditor(state, new WARemoteAccessPanel(state.getRootModel().getModule(), false, null, null, null)),
                new ModuleEditor(state, new RolesPanel(state.getRootModel().getModule())),
                new ModuleEditor(state, new SubscriptionsPanel(state.getRootModel().getModule().getProject()))};
    }
}

