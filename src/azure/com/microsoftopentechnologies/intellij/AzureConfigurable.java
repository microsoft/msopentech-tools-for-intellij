/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoftopentechnologies.intellij;

import com.intellij.application.options.OptionsContainingConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.ui.AzureAbstractPanel;
import com.microsoftopentechnologies.intellij.ui.ServiceEndpointsPanel;
import com.microsoftopentechnologies.intellij.ui.StorageAccountPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AzureConfigurable extends SearchableConfigurable.Parent.Abstract implements OptionsContainingConfigurable {
    public static final String AZURE_PLUGIN_NAME = "MSOpenTech Tools";
    public static final String AZURE_PLUGIN_ID = "com.microsoftopentechnologies.intellij";

    private java.util.List<Configurable> myPanels;
    private final Project myProject;

    public AzureConfigurable(Project project) {
        myProject = project;
    }

    @Override
    protected Configurable[] buildConfigurables() {
        myPanels = new ArrayList<Configurable>();
        if (AzurePlugin.IS_WINDOWS) {
            myPanels.add(new AzureAbstractConfigurable(new ServiceEndpointsPanel()));
            myPanels.add(new AzureAbstractConfigurable(new StorageAccountPanel(myProject)));
        }
        return myPanels.toArray(new Configurable[myPanels.size()]);
    }

    @NotNull
    @Override
    public String getId() {
        return AZURE_PLUGIN_ID;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return AZURE_PLUGIN_NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        JLabel label = new JLabel(message("winAzMsg"), SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    @Override
    public boolean hasOwnContent() {
        return true;
    }

    @Override
    public Set<String> processListOptions() {
        return new HashSet<String>();
    }

    @Override
    public boolean isVisible() {
        return AzurePlugin.IS_WINDOWS;
    }

    public class AzureAbstractConfigurable implements SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
        private AzureAbstractPanel myPanel;

        public AzureAbstractConfigurable(AzureAbstractPanel myPanel) {
            this.myPanel = myPanel;
        }

        @Nls
        @Override
        public String getDisplayName() {
            return myPanel.getDisplayName();
        }

        @Nullable
        @Override
        public String getHelpTopic() {
            return null;
        }

        @Override
        public Set<String> processListOptions() {
            return null;
        }

        @Nullable
        @Override
        public JComponent createComponent() {
            return myPanel.getPanel();
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public void apply() throws ConfigurationException {

        }

        @Override
        public void reset() {

        }

        @Override
        public void disposeUIResources() {

        }

        @NotNull
        @Override
        public String getId() {
            return "preferences.sourceCode." + getDisplayName();
        }

        @Nullable
        @Override
        public Runnable enableSearch(String option) {
            return null;
        }
    }
}
