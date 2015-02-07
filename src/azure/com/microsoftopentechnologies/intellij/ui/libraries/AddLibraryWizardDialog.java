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

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class AddLibraryWizardDialog extends WizardDialog<AddLibraryWizardModel> {

    private AddLibraryWizardModel model;
    //todo:
    private String errorTitle;
    private String errorMessage;

    public AddLibraryWizardDialog(AddLibraryWizardModel model) {
        super(model.getMyModule().getProject(), true, model);
        this.model = model;
    }

    @Override
    public void onWizardGoalAchieved() {
        super.onWizardGoalAchieved();
    }

    @Override
    protected Dimension getWindowPreferredSize() {
        return new Dimension(400, 400);
    }

    public void setCancelText(String text) {
        myModel.getCurrentNavigationState().CANCEL.setName(text);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return myModel.doValidate();
    }

    protected boolean postponeValidation() {
        return false;
    }

    @Override
    protected void doOKAction() {
//        validateInput();
        if (isOKActionEnabled() && performFinish()) {
            super.doOKAction();
        }
    }

    /**
     * This method gets called when wizard's finish button is clicked.
     *
     * @return True, if project gets created successfully; else false.
     */
    private boolean performFinish() {
        return true;
    }
}
