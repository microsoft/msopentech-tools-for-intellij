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

package com.microsoftopentechnologies.intellij.helpers.activityConfiguration.office365CustomWizardParameter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OneNoteConfigForm extends DialogWrapper {
    private JPanel rootPanel;
    private JTextField clientIdTextField;

    public OneNoteConfigForm(Project project) {
        super(project, true);

        setTitle("Configure OneNote Service");

        init();
    }


    public String getClientId() {
        return clientIdTextField.getText();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return (clientIdTextField.getText().length() == 0) ?
                new ValidationInfo("Client ID should no be empty", clientIdTextField)
                : super.doValidate();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootPanel;
    }

}
