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

package com.microsoftopentechnologies.intellij.wizards.activityConfiguration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardDialog;

import javax.swing.*;
import java.awt.*;

public class AddServiceWizard extends WizardDialog<AddServiceWizardModel> {
    public AddServiceWizard(Project project, Module module, String activityName, boolean isMobileServiceSelected,
                            boolean isNotificationHubSelected, boolean isOutlookServices, boolean isFileServices,
                            boolean isListServices, boolean isOneNoteService) {
        super(true, true, new AddServiceWizardModel(project, module, activityName, isMobileServiceSelected,
                isNotificationHubSelected, isOutlookServices, isFileServices, isListServices, isOneNoteService));
    }

    public static void run(Project project, Module module, String activityName, boolean isMobileServiceSelected,
                           boolean isNotificationHubSelected, boolean isOutlookServices, boolean isFileServices,
                           boolean isListServices, boolean isOneNoteService) {
        new AddServiceWizard(project, module, activityName, isMobileServiceSelected, isNotificationHubSelected,
                isOutlookServices, isFileServices, isListServices, isOneNoteService).show();
    }

    @Override
    protected Dimension getWindowPreferredSize() {
        this.getWindow();
        this.setResizable(false);
        return new Dimension(767, 467);
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanelComp = super.createSouthPanel();

        if (southPanelComp instanceof JPanel) {
            final JPanel southPanel = (JPanel) southPanelComp;

            if (southPanel.getComponentCount() == 1 && southPanel.getComponent(0) instanceof JPanel) {
                JPanel panel = (JPanel) southPanel.getComponent(0);

                for (Component buttonComp : panel.getComponents()) {
                    if (buttonComp instanceof JButton) {
                        JButton button = (JButton) buttonComp;
                        String text = button.getText();

                        if (text != null) {
                            if (text.equals("Help")) {
                                panel.remove(button);
                            }
                        }
                    }
                }
            }
        }

        return southPanelComp;
    }
}