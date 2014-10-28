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
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class CertificatesConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private CertificatesPanel certificatesPanel;

    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;

    public CertificatesConfigurable(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        certificatesPanel = new CertificatesPanel(module, waProjManager, waRole);
    }

    @NotNull
    @Override
    public String getId() {
        return getDisplayName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return message("cmhLblCert");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return certificatesPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        return certificatesPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            waProjManager.save();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            throw new ConfigurationException(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"));
        }
        certificatesPanel.setModified(false);
    }

    @Override
    public void reset() {
        certificatesPanel.setModified(false);
    }

    @Override
    public void disposeUIResources() {
    }
}
