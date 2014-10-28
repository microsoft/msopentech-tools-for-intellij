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
package com.microsoftopentechnologies.intellij.ui.components;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoftopentechnologies.intellij.ui.AzureAbstractPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class DefaultDialogWrapper extends DialogWrapper {
    private AzureAbstractPanel contentPanel;

    public DefaultDialogWrapper(Project project, AzureAbstractPanel panel) {
        super(project, true);
        this.contentPanel = panel;
        init();
    }

    @Override
    protected void init() {
        setTitle(contentPanel.getDisplayName());
        super.init();
    }

    @Override
    protected void doOKAction() {
        if (contentPanel.doOKAction()) {
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return contentPanel.doValidate();
    }

    public String getSelectedValue() {
        return contentPanel.getSelectedValue();
    }

    @Override
    protected JComponent createTitlePane() {
        JLabel header = new JLabel(contentPanel.getDisplayName());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14));
        return header;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel.getPanel();
    }
}

