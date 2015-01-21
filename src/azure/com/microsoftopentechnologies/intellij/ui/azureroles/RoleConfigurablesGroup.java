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
package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.SearchableConfigurable;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoleConfigurablesGroup extends SearchableConfigurable.Parent.Abstract
        implements SearchableConfigurable, ConfigurableGroup, Configurable.NoScroll {
    private static final String MY_GROUP_ID = "configurable.group.azure";
    private final Configurable[] rolesConfigurable;

    public RoleConfigurablesGroup(Module module, WindowsAzureProjectManager waProjMgr, WindowsAzureRole role, boolean isNew) {
        rolesConfigurable = new Configurable[] {new RolesConfigurable(module, waProjMgr, role, isNew)};
    }

    @Override
    protected Configurable[] buildConfigurables() {
        return rolesConfigurable;
    }

    @NotNull
    @Override
    public String getId() {
        return MY_GROUP_ID;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "azure";
    }

    @Override
    public String getShortName() {
        return getDisplayName();
    }

    @Override
    public boolean isModified() {
        return rolesConfigurable[0].isModified();
    }
}

