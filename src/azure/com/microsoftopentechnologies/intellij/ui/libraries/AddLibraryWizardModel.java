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
package com.microsoftopentechnologies.intellij.ui.libraries;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardModel;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AddLibraryWizardModel extends WizardModel {
    private SelectLibraryStep selectLibraryStep;
    private LibraryPropertiesStep libraryPropertiesStep;
    private Module myModule;
    private AzureLibrary selectedLibrary;

    public AddLibraryWizardModel(final Module module) {
        super(message("addLibraryTitle"));
        myModule = module;
        selectLibraryStep = new SelectLibraryStep(this.getTitle(), this);
        libraryPropertiesStep = new LibraryPropertiesStep(this.getTitle(), this);
        add(selectLibraryStep);
        add(libraryPropertiesStep);
    }

    public void setSelectedLibrary(AzureLibrary selectedLibrary) {
        this.selectedLibrary = selectedLibrary;
    }

    public AzureLibrary getSelectedLibrary() {
        return selectedLibrary;
    }

    public Module getMyModule() {
        return myModule;
    }

    public ValidationInfo doValidate() {
        return null;
//        return ((AzureWizardStep) getCurrentStep()).doValidate();
    }
}
