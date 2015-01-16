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

package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardDialog;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.VMServiceModule;

import javax.swing.*;
import java.awt.*;

public class CreateVMWizard extends WizardDialog<CreateVMWizardModel> {
    public CreateVMWizard(VMServiceModule node) {
        super(true, true, new CreateVMWizardModel(node));
    }

    @Override
    protected Dimension getWindowPreferredSize() {
        this.getWindow();
        this.setResizable(false);
        return new Dimension(790, 467);
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
