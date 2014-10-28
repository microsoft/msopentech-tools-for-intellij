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

public class AzureModuleConfigurationEditorProvider implements ModuleConfigurationEditorProvider {
    @Override
    public ModuleConfigurationEditor[] createEditors(final ModuleConfigurationState state) {
        Module module = state.getRootModel().getModule();
        if (!AzureModuleType.isAzureModule(module)) {
            return ModuleConfigurationEditor.EMPTY;
        }
        return new ModuleConfigurationEditor[]{new RemoteAccessEditor(state), new RolesEditor(state), new SubscriptionsEditor(state)};
    }
}

