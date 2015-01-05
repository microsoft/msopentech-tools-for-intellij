package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TitlePanel;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureLocalStorage;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.azurecommons.exception.AzureCommonsException;
import com.microsoftopentechnologies.azurecommons.roleoperations.LocalStrgResDialogUtilMethods;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class LocalStorageResourceDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField txtResource;
    private JTextField txtSize;
    private JCheckBox btnClean;
    private JTextField txtVar;
    private JLabel lblNote;

    private WindowsAzureRole waRole;
    private int maxSize;
    private Map<String,WindowsAzureLocalStorage> lclStgMap;
    private String resName;
    private boolean isResEdit;

    public LocalStorageResourceDialog(WindowsAzureRole waRole, Map<String, WindowsAzureLocalStorage> mapLclStg) {
        super(true);
        this.waRole = waRole;
        this.maxSize = WindowsAzureProjectManager.getMaxLocalStorageSize(waRole.getVMSize());
        this.lclStgMap = mapLclStg;
        init();
    }

    /**
     * Constructor to be called for editing an local storage resource.
     */
    public LocalStorageResourceDialog(WindowsAzureRole waRole, Map<String, WindowsAzureLocalStorage> mapLclStg, String key) {
        this(waRole, mapLclStg);
        this.isResEdit = true;
        this.resName = key;
        populateData();
    }

    @Override
    protected void init() {
        setTitle(message("lclStrTtl"));
        lblNote.setText(String.format("%s%s%s%s", message("rangeNote1"), " ", maxSize, message("rangeNote2")));
        super.init();
    }

    /**
     * Populates the resource name and value text fields with the corresponding
     * attributes of local storage resource selected for editing.
     *
     */
    private void populateData() {
        try {
            WindowsAzureLocalStorage stg = lclStgMap.get(resName);
            txtResource.setText(stg.getName());
            txtSize.setText(String.valueOf(stg.getSize()));
            btnClean.setSelected(stg.getCleanOnRecycle());
            txtVar.setText(stg.getPathEnv());
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSetErrTtl"), message("lclStgSetErrMsg"), e);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("lclStrTxt"), message("lclStrMsg"));
    }

    public String getResName() {
        return resName;
    }

    protected void doOKAction() {
        boolean retVal = true;
        try {
            if (isResEdit && txtVar.getText().equalsIgnoreCase(lclStgMap.get(resName).getPathEnv())) {
                retVal =  isValidName(txtResource.getText()) && isValidSize(txtSize.getText());
            } else {
                retVal = isValidName(txtResource.getText()) && isValidSize(txtSize.getText()) && isValidPath(txtVar.getText());
            }
            if (!isResEdit && retVal) {
                waRole.addLocalStorage(txtResource.getText(), Integer.parseInt(txtSize.getText()), btnClean.isSelected(), txtVar.getText());
                resName = txtResource.getText();
            } else if (isResEdit && retVal) {
                lclStgMap.get(resName).setName(txtResource.getText());
                if (!resName.equalsIgnoreCase(txtResource.getText())) {
                    resName = txtResource.getText();
                }
                lclStgMap.get(resName).setSize(Integer.parseInt(txtSize.getText()));
                lclStgMap.get(resName).setPathEnv(txtVar.getText());
                lclStgMap.get(resName).setCleanOnRecycle(btnClean.isSelected());
            }
        } catch (NumberFormatException e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSizeErrTtl"), message("lclStgSizeErrMsg"), e);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSetErrTtl"), message("lclStgSetErrMsg"), e);
        }
        if (retVal) {
            super.doOKAction();
        }
    }

    /**
     * Validates the resource name of local storage.
     *
     * @param name : name to be validated.
     * @return retVal : true if name is valid else false
     */
    private boolean isValidName(String name) {
        try {
            return LocalStrgResDialogUtilMethods.isValidName(name, lclStgMap, isResEdit, resName);
        } catch (AzureCommonsException e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), e.getMessage());
            return false;
        }
    }

    /**
     * Validates the size of VM.
     *
     * @param size : user entered size
     * @return isValidSize : true if size is valid else false.
     */
    private boolean isValidSize(String size) {
        boolean isValidSize;
        try {
            int value = Integer.parseInt(size);
            if (value <= 0) {
                PluginUtil.displayErrorDialog(message("lclStgSizeErrTtl"), message("lclStgSizeErrMsg"));
                isValidSize = false;
            } else if (value > maxSize) {
                int choice = Messages.showYesNoDialog(String.format("%s%s%s", message("lclStgMxSizeMsg1") , maxSize, message("lclStgMxSizeMsg2")),
                        message("lclStgMxSizeTtl"), Messages.getQuestionIcon());
                /*
                 * If user selects No
                 * then keep dialog open.
                 */
                isValidSize = choice == Messages.YES;
            } else {
                isValidSize = true;
            }
        } catch (NumberFormatException e) {
            PluginUtil.displayErrorDialogAndLog(message("lclStgSizeErrTtl"), message("lclStgSizeErrMsg"), e);
            isValidSize = false;
        }
        return isValidSize;
    }

    /**
     * Validates the environment path.
     *
     * @param path : user given path
     * @return : true if valid path else false
     */
    private boolean isValidPath(String path) {
        try {
            return LocalStrgResDialogUtilMethods.isValidPath(path, lclStgMap, waRole);
        } catch (AzureCommonsException e) {
            PluginUtil.displayErrorDialogAndLog(message("genErrTitle"), e.getMessage(), e);
            return false;
        }
    }
}
