package com.microsoftopentechnologies.intellij.ui.libraries;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.actions.LibraryConfigurationAction;

import javax.swing.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class SelectLibraryStep extends WizardStep<AddLibraryWizardModel> {
    private JPanel rootPanel;
    private JList libraryList;
    private final AddLibraryWizardModel myModel;

    public SelectLibraryStep(final String title, final AddLibraryWizardModel model) {
        super(title, message("selectLocationDesc"));
        myModel = model;
        init();
    }

    public void init() {
        libraryList.setListData(LibraryConfigurationAction.LIBRARIES);
        libraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public JComponent prepare(final WizardNavigationState state) {
        rootPanel.revalidate();
        return rootPanel;
    }


    @Override
    public WizardStep onNext(final AddLibraryWizardModel model) {
        if (doValidate() == null) {
            model.setSelectedLibrary((AzureLibrary) libraryList.getSelectedValue());
//            ((LibraryPropertiesStep) model.getNextFor(this)).setAzureLibrary((AzureLibrary) libraryList.getSelectedValue());
            return super.onNext(model);
        } else {
            return this;
        }
    }

    //    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    ValidationInfo createValidationInfo(String message, JComponent component) {
        myModel.getCurrentNavigationState().NEXT.setEnabled(false);
        return new ValidationInfo(message, component);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

}

