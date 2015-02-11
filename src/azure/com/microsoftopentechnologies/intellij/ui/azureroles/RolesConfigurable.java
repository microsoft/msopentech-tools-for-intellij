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
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

import javax.swing.*;

public class RolesConfigurable extends SearchableConfigurable.Parent.Abstract {
    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole windowsAzureRole;

    private AzureRolePanel panel;

    public RolesConfigurable(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole windowsAzureRole, boolean isNew) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.windowsAzureRole = windowsAzureRole;
        panel = new AzureRolePanel(module, waProjManager, windowsAzureRole, isNew);
    }

    @Override
    public boolean hasOwnContent() {
        return true;
    }

    @Override
    protected Configurable[] buildConfigurables() {
        Project project = module.getProject();
        return new Configurable[] {
                new CachingPanel(module, waProjManager, windowsAzureRole),
                new CertificatesPanel(module, waProjManager, windowsAzureRole),
                new ComponentsPanel(project, waProjManager, windowsAzureRole), new DebuggingPanel(project, waProjManager, windowsAzureRole),
                new RoleEndpointsPanel(module, waProjManager, windowsAzureRole),
                new EnvVarsPanel(module, waProjManager, windowsAzureRole),
                new LoadBalancingPanel(waProjManager, windowsAzureRole),
                new LocalStoragePanel(module, waProjManager, windowsAzureRole),
                new ServerConfigurationConfigurable(module, waProjManager, windowsAzureRole),
                new SSLOffloadingPanel(module, waProjManager, windowsAzureRole)};
    }

    @NotNull
    @Override
    public String getId() {
        return getDisplayName();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return message("cmhLblGeneral");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return panel.getHelpTopic();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return panel.createComponent();
    }

    @Override
    public boolean isModified() {
        return panel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        panel.apply();
    }

    @Override
    public void reset() {
        panel.reset();
    }
}
