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

package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.wizards.activityConfiguration.AddServiceWizard;

import javax.swing.*;
import java.awt.event.*;

public class AzureServicesSelectionForm extends JDialog {
    private JPanel mainPanel;
    private JCheckBox mobileServicesCbx;
    private JCheckBox notificationHubsCbx;
    private JButton okButton;
    private JButton cancelButton;
    private Project project;
    private Module module;

    public AzureServicesSelectionForm() {
        this.setResizable(false);
        this.setModal(true);
        this.setTitle("Configure Microsoft Azure Services");
        getRootPane().setDefaultButton(okButton);
        setContentPane(this.mainPanel);

        final AzureServicesSelectionForm form = this;

        this.mobileServicesCbx.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                validateCheckbox();
            }
        });

        this.notificationHubsCbx.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                validateCheckbox();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                form.setVisible(false);
                form.dispose();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        AddServiceWizard.run(form.project, form.module, form.mobileServicesCbx.isSelected(), form.notificationHubsCbx.isSelected(), false, false, false);
                    }
                });

                form.setVisible(false);
                form.dispose();
            }
        });

        // call onCancel() on ESCAPE
        getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton.doClick();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    private void validateCheckbox() {
        this.okButton.setEnabled(this.mobileServicesCbx.isSelected() || this.notificationHubsCbx.isSelected());
    }
}