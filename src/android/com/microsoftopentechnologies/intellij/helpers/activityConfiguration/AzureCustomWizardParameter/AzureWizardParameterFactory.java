/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.microsoftopentechnologies.intellij.helpers.activityConfiguration.AzureCustomWizardParameter;

import com.android.tools.idea.templates.Parameter;
import com.android.tools.idea.wizard.ScopedDataBinder;
import com.android.tools.idea.wizard.WizardParameterFactory;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;

public class AzureWizardParameterFactory implements WizardParameterFactory {


    @Override
    public String[] getSupportedTypes() {
        return new String[] { "azureCustomParameter" };
    }

    @Override
    public JComponent createComponent(String s, Parameter parameter) {
        return new AzureParameterPane();
    }

    @Override
    public ScopedDataBinder.ComponentBinding<String, JComponent> createBinding(JComponent jComponent, Parameter parameter) {
        return new ScopedDataBinder.ComponentBinding<String, JComponent>() {
            @Nullable
            @Override
            public String getValue(JComponent component) {
                return ((AzureParameterPane) component).getValue();
            }

            @Override
            public void setValue(@Nullable String newValue, JComponent component) {
                ((AzureParameterPane) component).setValue(newValue);
            }

            @Nullable
            @Override
            public Document getDocument(JComponent component) {
                return ((AzureParameterPane) component).getDocument();
            }
        };
    }


}
