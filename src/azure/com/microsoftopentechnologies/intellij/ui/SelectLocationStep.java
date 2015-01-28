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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.ui.components.AzureWizardStep;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class SelectLocationStep extends AzureWizardStep {
    private JPanel rootPanel;
    private JTextField projectName;
    private JCheckBox useDefaultLocation;
    private TextFieldWithBrowseButton projectLocation;
    private JLabel locationLabel;
    private final AzureWizardModel myModel;

    public SelectLocationStep(final String title, final AzureWizardModel model) {
        super(title, AzureBundle.message("wizPageDesc"));
        myModel = model;
        init();
    }

    public void init() {
        projectName.requestFocusInWindow();
        projectLocation.addActionListener(UIUtils.createFileChooserListener(projectLocation, myModel.getMyProject(),
                FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        projectLocation.setText(myModel.getMyProject().getBasePath());
        useDefaultLocation.addItemListener(createDefaultLocationListener());
        useDefaultLocation.setSelected(true);
    }

    @Override
    public JComponent prepare(final WizardNavigationState state) {
        rootPanel.revalidate();
        return rootPanel;
    }

    private ItemListener createDefaultLocationListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (useDefaultLocation.isSelected()) {
                    locationLabel.setEnabled(false);
                    projectLocation.setText(myModel.getMyProject().getBasePath());
                    projectLocation.setEnabled(false);
//                    buttonBrowse.setEnabled(false);
//                    setErrorMessage(null);
//                    setPageComplete(true);
                } else {
                    locationLabel.setEnabled(true);
                    projectLocation.setEnabled(true);
                    projectLocation.setText(""); //$NON-NLS-1$
//                    buttonBrowse.setEnabled(true);
//                    setDescription(message("wizPageEnterLoc"));
//                    setPageComplete(false);
                }
            }
        };
    }

    @Override
    public WizardStep onNext(final AzureWizardModel model) {
        if (doValidate() == null) {
            return super.onNext(model);
        } else {
            return this;
        }
    }

    @Override
    public ValidationInfo doValidate() {
        String projName = projectName.getText();
        if (projName.isEmpty()) {
            myModel.getCurrentNavigationState().NEXT.setEnabled(false);
            return null;
        } /*else if (!projNameStatus.isOK()) {
            setErrorMessage(projNameStatus.getMessage());
            setPageComplete(false);
        } */else if (ModuleManager.getInstance(myModel.getMyProject()).findModuleByName(projName) != null) {
            return createValidationInfo(message("wizPageErrMsg1"), projectName);
        } else if (!(new File(projectLocation.getText()).exists())) {
            return createValidationInfo(message("wizPageErrPath"), projectLocation);
        } else {
            myModel.getCurrentNavigationState().NEXT.setEnabled(true);
            return null;
        }
    }

    ValidationInfo createValidationInfo(String message, JComponent component) {
        myModel.getCurrentNavigationState().NEXT.setEnabled(false);
        return new ValidationInfo(message, component);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return projectName;
    }

    public String getProjectName() {
        return projectName.getText();
    }

    public String getProjectLocation() {
        return projectLocation.getText();
    }

    public boolean isUseDefaultLocation() {
        return useDefaultLocation.isSelected();
    }
}
