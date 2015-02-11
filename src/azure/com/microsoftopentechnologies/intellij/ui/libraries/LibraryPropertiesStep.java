/**
 * Copyright 2015 Microsoft Open Technologies Inc.
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
package com.microsoftopentechnologies.intellij.ui.libraries;

import javax.swing.*;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.actions.LibraryConfigurationAction;
import com.microsoftopentechnologies.intellij.ui.components.Validatable;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class LibraryPropertiesStep extends WizardStep<AddLibraryWizardModel> implements Validatable {

    private LibraryPropertiesPanel libraryPropertiesPanel;
    private final AddLibraryWizardModel myModel;

    public LibraryPropertiesStep(String title, final AddLibraryWizardModel model) {
        super(title, message("libraryPropertiesDesc"));
        myModel = model;
    }

    @Override
    public JComponent prepare(final WizardNavigationState state) {
        libraryPropertiesPanel = new LibraryPropertiesPanel(myModel.getMyModule(), myModel.getSelectedLibrary());
        return libraryPropertiesPanel.prepare();
    }

    @Override
    public ValidationInfo doValidate() {
        if (myModel.getSelectedLibrary() == LibraryConfigurationAction.ACS_FILTER) {
            ValidationInfo result = libraryPropertiesPanel.doValidate();
            myModel.getCurrentNavigationState().FINISH.setEnabled(result == null);
        }
        return null;
//        return libraryPropertiesPanel.doValidate();
    }

    @Override
    public boolean onFinish() {
        boolean result = libraryPropertiesPanel.onFinish();
        if (result) {
            myModel.setExported(libraryPropertiesPanel.isExported());
            return super.onFinish();
        }
        return false;
    }
}
