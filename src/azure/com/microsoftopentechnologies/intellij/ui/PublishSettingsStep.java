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
package com.microsoftopentechnologies.intellij.ui;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.ui.components.AzureWizardStep;

import javax.swing.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class PublishSettingsStep extends AzureWizardStep {

    private JdkServerPanel jdkServerPanel;
    private final AzureWizardModel myModel;

    public PublishSettingsStep(final String title, final AzureWizardModel model) {
        super(title, message("dplPageJdkMsg"));
        myModel = model;
        jdkServerPanel = new JdkServerPanel(model.getMyProject(), model.getWaRole(), null);
    }

    @Override
    public WizardStep onNext(final AzureWizardModel model) {
        int currentTab = jdkServerPanel.getSelectedIndex();
        if (currentTab == 2) {
            return super.onNext(model);
        } else {
            jdkServerPanel.setSelectedIndex(++currentTab);
            return this;
        }
    }

    @Override
    public WizardStep onPrevious(final AzureWizardModel model) {
        int currentTab = jdkServerPanel.getSelectedIndex();
        if (currentTab == 0) {
            return super.onPrevious(model);
        } else {
            jdkServerPanel.setSelectedIndex(--currentTab);
            return this;
        }
    }

    @Override
    public JComponent prepare(final WizardNavigationState state) {
//        rootPanel.revalidate();
        state.FINISH.setEnabled(true);
        return jdkServerPanel.getPanel();
    }

    @Override
    public ValidationInfo doValidate() {
        return jdkServerPanel.doValidate();
    }

    public JdkServerPanel getJdkServerPanel() {
        return jdkServerPanel;
    }
}
