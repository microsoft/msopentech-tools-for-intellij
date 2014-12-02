package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.exception.AzureCommonsException;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.roleoperations.WAEnvVarDialogUtilMethods;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class EnvVarDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JTextField txtValue;

    private Map<String, String> mapEnvVar;
    private WindowsAzureRole waRole;
    private boolean isEditVariable = false;
    private String varName;
    private String newVarName;

    public EnvVarDialog(Map<String, String> mapEnvVar, WindowsAzureRole windowsAzureRole, String key, boolean isEditVariable) {
        super(false);
        this.mapEnvVar = mapEnvVar;
        this.waRole = windowsAzureRole;
        this.isEditVariable = isEditVariable;
        this.varName = key;
        init();
    }

    protected void init() {
        if (isEditVariable) {
            // populate the environment variable name and it's value
            // in case of this dialog is opened for editing the variable.
            populateData();
            setTitle(message("evEditTitle"));
        } else {
            setTitle(message("evNewTitle"));
        }
        super.init();
    }

    /**
     * Populates the variable name and value text fields with the corresponding
     * attributes of environment variable selected for editing.
     *
     */
    private void populateData() {
        try {
            txtName.setText(varName);
            txtValue.setText(mapEnvVar.get(varName));
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public String getNewVarName() {
        return newVarName;
    }

    @Override
    protected void doOKAction() {
        String value = txtValue.getText().trim();
        String name = txtName.getText().trim();
        name = name.replaceAll("[\\s]+", "_");
        try {
            if (isEditVariable && !varName.equals(txtName.getText())) {
                // Here the comparison is case sensitive to handle
                // the scenario where user edits the name such that
                // only case (upper/lower) of the letters changes.

                // renames the variable name
                waRole.renameRuntimeEnv(varName, name);
            }
            waRole.setRuntimeEnv(name, value);
            newVarName = name;
            super.doOKAction();
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
        }
    }

    protected ValidationInfo doValidate() {
        try {
            return validateName();
        } catch (Exception ex) {
            log(message("rolsErr") + message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            return new ValidationInfo(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"));
        }
    }

    /**
     * Validates the variable name so that it should not be empty
     * or an existing one.
     *
     * @return true if the variable name is valid, else false
     */
    private ValidationInfo validateName() {
        try {
            WAEnvVarDialogUtilMethods.validateName(txtName.getText().trim(), mapEnvVar, isEditVariable, varName, waRole);
        } catch (AzureCommonsException e) {
            return new ValidationInfo(e.getMessage());
        }
        return null;
    }
}
