package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;

import javax.swing.*;

public class CloudServiceStep extends WizardStep<CreateVMWizardModel> {

    private Project project;
    private CreateVMWizardModel model;

    public CloudServiceStep(CreateVMWizardModel mModel, Project project) {

        this.project = project;
        this.model = mModel;
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        return null;
    }
}
