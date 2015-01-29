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

import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.roots.ui.configuration.ModuleElementsEditor;
import com.microsoftopentechnologies.intellij.ui.AzureAbstractPanel;
import com.microsoftopentechnologies.intellij.ui.SubscriptionsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class ModuleEditor extends ModuleElementsEditor {
    private AzureAbstractPanel panel;

    public ModuleEditor(ModuleConfigurationState state, AzureAbstractPanel panel) {
        super(state);
        this.panel = panel;
    }

    @Override
    protected JComponent createComponentImpl() {
        return panel.getPanel();
    }

    @Override
    public boolean isModified() {
        return true || super.isModified();
    }

    @Override
    public void saveData() {
        panel.doOKAction();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return panel.getDisplayName();
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }
}


