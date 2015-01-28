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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.michaelbaranov.microba.calendar.DatePicker;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.wacommonutil.CerPfxUtil;
import com.microsoftopentechnologies.azurecommons.wacommonutil.EncUtilHelper;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.wacommon.commoncontrols.NewCertificateDialogData;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class WARemoteAccessPanel implements AzureAbstractPanel {

    private static final String BASE_PATH = "${basedir}";

    private JPanel contentPane;
    private JCheckBox remoteChkBtn;
    private JTextField txtUserName;
    private JLabel confirmPwdLbl;
    private JPasswordField txtConfirmPwd;
    private JPasswordField txtPassword;
    private JLabel expiryDateLabel;
    private DatePicker txtExpiryDate;
    private JLabel pathLabel;
    private JTextField txtPath;
    private JLabel noteLabel;
    private JButton fileSystemButton;
    private JButton newButton;
    private JLabel userNameLabel;
    private JLabel passwordLabel;

    private boolean isPwdChanged;
    private boolean isInconsistent;
    private boolean isFrmEncLink;
    private Module myModule;
    private WindowsAzureProjectManager waProjManager;

    public WARemoteAccessPanel(Module module, boolean isFrmEncLink, String uname, String pwd, String cnfPwd) {
        this.myModule = module;
        this.isFrmEncLink = isFrmEncLink;
        loadProject();
        init(uname, pwd, cnfPwd);
    }

    protected void init(String uname, String pwd, String cnfPwd) {
        remoteChkBtn.addItemListener(createRemoteChkBtnListener());
        newButton.addActionListener(createNewButtonListener());
        fileSystemButton.addActionListener(UIUtils.createFileChooserListener(txtPath, myModule.getProject(),
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        try {
            remoteChkBtn.setSelected(waProjManager.getRemoteAccessAllRoles());
        } catch (WindowsAzureInvalidProjectOperationException e2) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErAllRoles"), e2);
        }
        try {
            txtUserName.setText(waProjManager.getRemoteAccessUsername());
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            txtUserName.setText("");
            remoteChkBtn.setSelected(false);
            /*
             * When user data is not consistent we are making
             * isInconsistent as true and later on we are checking the status
             * of this variable and throwing the error message to user.
             */
            isInconsistent = true;
            log(message("remAccErUserName"), e1);
        }

        createPasswordComponent();

        createConfPwdComponent();

        createExpiryDateComponent();

        try {
            txtPath.setText(waProjManager.getRemoteAccessCertificatePath());
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            txtPath.setText("");
            remoteChkBtn.setSelected(false);
            /*
             * When user data is not consistent we are making
             * isInconsistent as true and later on we are checking the status
             * of this variable and throwing the error message to user.
             */
            isInconsistent = true;
            log(message("remAccErCertPath"), e1);
        }
/*
         * Check if we are coming from Publish wizard link,
         */
        if (isFrmEncLink) {
            if (uname.isEmpty()) {
                // disable remote access
                remoteChkBtn.setSelected(false);
                makeAllTextBlank();
            } else {
        		/*
        		 * enable remote access and
        		 * show values given on publish wizard
        		 */
                remoteChkBtn.setSelected(true);
                txtUserName.setText(uname);
                txtPassword.setText(pwd);
                txtConfirmPwd.setText(cnfPwd);
                try {
                    if (!waProjManager.getRemoteAccessEncryptedPassword().equals(pwd)) {
                        isPwdChanged = true;
                    }
                } catch (WindowsAzureInvalidProjectOperationException e) {
                    log(message("remAccErPwd"), e);
                }
                isFrmEncLink =  true;
            }
        } else {
            if (remoteChkBtn.isSelected()) {
                getDefaultValues();
            } else {
                makeAllTextBlank();
            }
        }
        setComponentStatus(remoteChkBtn.isSelected());
        /*
         * Here we are checking the isInconsistent value
         * and showing the error message to user on UI.
         */
        if (isInconsistent) {
            PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccDataInc"));
        }
        /*
         * Non windows OS then disable components,
         * but keep values as it is
         */
        if (!AzurePlugin.IS_WINDOWS) {
            setComponentStatus(false);
            if (!remoteChkBtn.isSelected()) {
                remoteChkBtn.setEnabled(false);
            }
        }
    }

    public String getDisplayName() {
        return message("cmhLblRmtAces");
    }

    public String getSelectedValue() {
        return null;
    }

    private void createPasswordComponent() {
        txtPassword.addFocusListener(new FocusListener() {
            /**
             * making text box blank on focus gained.
             */
            @Override
            public void focusGained(FocusEvent e) {
                txtPassword.setText("");
            }

            @Override
            public void focusLost(FocusEvent e) {
                UIUtils.checkRdpPwd(isPwdChanged, txtPassword, waProjManager, !isFrmEncLink, txtConfirmPwd);
            }
        });
        /*
         * Listener for key event when user click on password text box
         * it will set flag for entering the new values.
         */
        txtPassword.addKeyListener(createTxtPasswordKeyListener());
        try {
            txtPassword.setText(waProjManager.getRemoteAccessEncryptedPassword());
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            txtPassword.setText("");
            remoteChkBtn.setSelected(false);
            /*
             * When user data is not consistent we are making
             * isInconsistent as true and later on we are checking the status
             * of this variable and throwing the error message to user.
             */
            isInconsistent = true;
            log(message("remAccErPwd"), e1);
        }
    }

    /**
     * Creates label and text box for confirm password.
     */
    private void createConfPwdComponent() {
        try {
            txtConfirmPwd.setText(waProjManager.getRemoteAccessEncryptedPassword());
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            txtConfirmPwd.setText("");
            remoteChkBtn.setSelected(false);
            /*
             * When user data is not consistent we are making
             * isInconsistent as true and later on we are checking the status
             * of this variable and throwing the error message to user.
             */
            isInconsistent = true;
            log(message("remAccErPwd"), e1);
        }
        txtConfirmPwd.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent event) {
                try {
                    if (!isPwdChanged) {
                        if (txtPassword.getPassword().length == 0) {
                            txtConfirmPwd.setText("");
                        } else {
                            txtConfirmPwd.setText(waProjManager.getRemoteAccessEncryptedPassword());
                        }
                    }
                } catch (WindowsAzureInvalidProjectOperationException e1) {
                    PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErPwd"), e1);
                }
            }

            @Override
            public void focusGained(FocusEvent event) {
                txtConfirmPwd.setText("");
            }
        });
    }

    private void createExpiryDateComponent() {
        txtExpiryDate.setDateFormat(new SimpleDateFormat(message("remAccDateFormat"), Locale.getDefault()));
        try {
            Date date = waProjManager.getRemoteAccessAccountExpiration();
            if (date == null) {
                GregorianCalendar currentCal = new GregorianCalendar();
                currentCal.add(Calendar.YEAR, 1);
                Date today = currentCal.getTime();
                if (txtExpiryDate.isEnabled()) {
                    txtExpiryDate.setDate(today);
                }
            } else {
                txtExpiryDate.setDate(date);
            }
        } catch (WindowsAzureInvalidProjectOperationException e1) {
//            txtExpiryDate.setText("");
            remoteChkBtn.setSelected(false);
            /*
             * When user data is not consistent we are making
             * isInconsistent as true and later on we are checking the status
             * of this variable and throwing the error message to user.
             */
            isInconsistent = true;
            log(message("remAccErExpDate"), e1);
        } catch (PropertyVetoException e) {
            log(message("remAccErExpDate"), e);
        }
    }

    private ItemListener createRemoteChkBtnListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setComponentStatus(remoteChkBtn.isSelected());
                if (remoteChkBtn.isSelected()) {
                    getDefaultValues();
                } else {
                    makeAllTextBlank();
                }
            }
        };
    }

    private KeyListener createTxtPasswordKeyListener() {
        return new KeyListener() {
            @Override
            public void keyReleased(KeyEvent event) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
                isPwdChanged = true;
            }

            @Override
            public void keyPressed(KeyEvent event) {
            }
        };
    }

    private ActionListener createNewButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCertificateDialogData data = new NewCertificateDialogData();
                NewCertificateDialog dialog = new NewCertificateDialog(data, WAEclipseHelperMethods.findJdkPathFromRole(waProjManager), myModule.getProject());
                dialog.show();
                if (dialog.isOK()) {
                    String certPath = data.getCerFilePath();
                    if (certPath != null && certPath.contains(PluginUtil.getModulePath(myModule) + File.separator)) {
                        String workspacePath = PluginUtil.getModulePath(myModule); // todo: ??
                        String replaceString = certPath;
                        String subString = certPath.substring(certPath.indexOf(workspacePath), workspacePath.length());
                        replaceString = replaceString.replace(subString, BASE_PATH);
                        txtPath.setText(replaceString);
                    } else {
                        txtPath.setText(certPath);
                    }
                }
            }
        };
    }

    /**
     * This method will set all fields to blank,
     * if remote check button is disabled.
     */
    private void makeAllTextBlank() {
        txtUserName.setText("");
        txtPassword.setText("");
        txtConfirmPwd.setText("");
//        txtExpiryDate.setDate(null);
        txtPath.setText("");
    }

    /**
     *  This method will set default values to all fields.
     */
    private void getDefaultValues() {
        try {
            txtUserName.setText(waProjManager.getRemoteAccessUsername());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErUserName"), e);
        }
        try {
            txtPassword.setText(waProjManager.getRemoteAccessEncryptedPassword());
            txtConfirmPwd.setText(waProjManager.getRemoteAccessEncryptedPassword());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErPwd"), e);
        }
        try {
            txtPath.setText(waProjManager.getRemoteAccessCertificatePath());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErCertPath"), e);
        }
        try {
            Date date = waProjManager.getRemoteAccessAccountExpiration();
            if (date == null) {
                GregorianCalendar currentCal = new GregorianCalendar();
                currentCal.add(Calendar.YEAR, 1);
                Date today = currentCal.getTime();
                if (txtExpiryDate.isEnabled()) {
                    txtExpiryDate.setDate(today);
                }
            } else {
                txtExpiryDate.setDate(date);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErExpDate"), e);
        } catch (PropertyVetoException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErExpDate"), e);
        }
    }

    /**
     * This method will disable/enable all fields based on
     * the remote check button status.
     *
     * @param status : status of the remote check check box.
     */
    private void setComponentStatus(boolean status) {
        userNameLabel.setEnabled(status);
        passwordLabel.setEnabled(status);
        confirmPwdLbl.setEnabled(status);
        expiryDateLabel.setEnabled(status);
        txtUserName.setEnabled(status);
        txtPassword.setEnabled(status);
        txtConfirmPwd.setEnabled(status);
        txtExpiryDate.setEnabled(status);
        txtPath.setEnabled(status);
        pathLabel.setEnabled(status);
        noteLabel.setEnabled(status);
        newButton.setEnabled(status);
//        workspaceButton.setEnabled(status);
        fileSystemButton.setEnabled(status);
//        cal.setEnabled(status);
    }

    @Nullable
    @Override
    public ValidationInfo doValidate() {
        String userName = txtUserName.getText();
        String newPath = txtPath.getText();
//        String expDate = txtExpiryDate.getText();
        if (newPath.startsWith(BASE_PATH)) {
            newPath = newPath.substring(newPath.indexOf("}") + 1, newPath.length());
            newPath = String.format("%s%s", PluginUtil.getModulePath(myModule), newPath);
        }
        File cerFile = new File(newPath);
        boolean isRemoteEnabled = remoteChkBtn.isSelected();
        if (isRemoteEnabled && userName.isEmpty()) {
            return new ValidationInfo(message("remAccNameNull"), txtUserName);
        } /*else if (isRemoteEnabled && txtExpiryDate.isEmpty()) {
            return new ValidationInfo(message("remAccExpDateNull"), txtExpiryDate);
        } */else if (isRemoteEnabled && (!cerFile.exists() || (!newPath.endsWith(".cer")))) {
            return new ValidationInfo(message("remAccInvldPath"), txtPath);
        } else {
            return null;
        }
    }

    /**
     * Validates the expiry date.
     * Expiry date should be greater than current date.
     *
     * @param expDate
     * @return
     * @throws ParseException
     * @throws WindowsAzureInvalidProjectOperationException
     */
    private boolean validateExpDate(Date expDate) throws ParseException, WindowsAzureInvalidProjectOperationException {
        boolean isValid = true;
        long todaySeconds, userDateSeconds;
        userDateSeconds = expDate.getTime();
        GregorianCalendar todayCal = new GregorianCalendar();
        todaySeconds = todayCal.getTimeInMillis();
        if ((userDateSeconds - todaySeconds) < 0) {
            PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccDateWrong"));
            isValid = false;
        } else {
            waProjManager.setRemoteAccessAccountExpiration(expDate);
        }
        return isValid;
    }

    public boolean doOKAction() {
        try {
            loadProject();
            if (remoteChkBtn.isSelected()) {
                waProjManager.setRemoteAccessAllRoles(true);
                String userName = txtUserName.getText();
                String pwd = txtPassword.getText();
                String cnfPwd = txtConfirmPwd.getText();
                String newPath = txtPath.getText();
                Date expDate = txtExpiryDate.getDate();
                String tempPath = newPath;
                boolean isPathChanged = false;
                waProjManager.setRemoteAccessUsername(userName);
                if (!newPath.equals(waProjManager.getRemoteAccessCertificatePath()) && !newPath.isEmpty()) {
                    isPathChanged = true;
                    /*
                     * check If certificate file path has changed,
                     * If yes then prompt user
                     * for changing the password as well,
                     * if that is not changed.
                     * Because we have to encrypt the new password
                     * and then we will generate certificate
                     * based on that.
                     * Case 1 :- If user has changed the path
                     * and password is old then it
                     * will prompt for new password or re-enter the password.
                     * If user changes the password
                     * then it will generate certificate based
                     * on that new password.
                     * Case 2 :- If user set the blank password
                     * even after displaying that
                     * password change prompt, in that case
                     * we will display warning messages
                     * to user that whether he want to continue
                     * with empty password, If yes
                     * then we will consider that blank password
                     * else use will have to enter
                     * new password.
                     */
                    if (pwd.equals(waProjManager.getRemoteAccessEncryptedPassword()) && !pwd.isEmpty()) {
                        txtPassword.setText("");
                        txtConfirmPwd.setText("");
                        PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccPwdMstChng"));
                        return false;
                    }
                }
                if (pwd.isEmpty()) {
                    int choice = Messages.showOkCancelDialog(message("remAccErTxtTitle"), message("remAccWarnPwd"), Messages.getQuestionIcon());
                    if (!(choice == Messages.OK)) {
                        return false;
                    }
                }
                if (expDate == null) {
                    PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccExpDateNull"));
                    return false;
                } else {
                    boolean status =  validateExpDate(expDate);
                    if (!status) {
                        return false;
                    }
                }
                if (newPath.equalsIgnoreCase("")) {
                    PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccPathNull"));
                    return false;
                }
                /*
                 * Check for displaying the relative path
                 * in case when user select the certificate file path
                 * as workspace or of current project.
                 * We will be showing relative path in that case on UI.
                 */
                if (tempPath.startsWith(BASE_PATH)) {
                    tempPath = tempPath.substring(tempPath.indexOf("}") + 1, tempPath.length());
                    tempPath = String.format("%s%s", PluginUtil.getModulePath(myModule), tempPath);
                }
                File file = new File(tempPath);
                //if path is not correct.display error message for that.
                if (file.exists() && tempPath.endsWith(".cer")) {
                    waProjManager.setRemoteAccessCertificatePath(newPath);
                } else {
                    PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccInvldPath"));
                    return false;
                }
                try {
                    if (isPathChanged) {
                        String thumbprint = CerPfxUtil.getThumbPrint(tempPath);
                        if (waProjManager.isRemoteAccessTryingToUseSSLCert(thumbprint)) {
                            PluginUtil.displayErrorDialog(message("remAccSyntaxErr"), message("usedBySSL"));
                            return false;
                        } else {
                            waProjManager.setRemoteAccessCertificateFingerprint(thumbprint);
                        }
                    }
                } catch (Exception e) {
                    PluginUtil.displayErrorDialogAndLog(message("remAccSyntaxErr"), message("remAccErTmbPrint"), e);
                    return false;
                }
                if (cnfPwd.equals(pwd)) {
                    try {
                        /*
                         * Encrypting the password
                         * if it is not dummy & blank from xml
                         * and isPwdChanged is true that means
                         * user has changes the password.
                         */
                        String modifiedPwd = message("remAccDummyPwd");
                        if (!pwd.equals(modifiedPwd) && !pwd.isEmpty() && isPwdChanged) {
                            String encryptedPwd = EncUtilHelper.encryptPassword(pwd, tempPath, AzurePlugin.encFolder);
                            waProjManager.setRemoteAccessEncryptedPassword(encryptedPwd);
                        } else {
                            waProjManager.setRemoteAccessEncryptedPassword(pwd);
                        }
                    } catch (Exception e) {
                        PluginUtil.displayErrorDialogAndLog(message("remAccSyntaxErr"), message("remAccErPwd"), e);
                        return false;
                    }
                } else {
                    PluginUtil.displayErrorDialog(message("remAccErTxtTitle"), message("remAccPwdNotMatch"));
                    return false;
                }
            } else {
                waProjManager.setRemoteAccessAllRoles(false);
            }
            waProjManager.save();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccSyntaxErr"), message("proPageErrMsgBox1") + message("proPageErrMsgBox2"), e);
        } catch (ParseException e) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErDateParse"), e);
            return false;
        }
//        WAEclipseHelper.refreshWorkspace(Messages.remAccWarning, Messages.remAccWarnMsg);
        isFrmEncLink = false;
        return true;
    }

    public JComponent getPanel() {
        return contentPane;
    }

    /**
     * This method loads the projects available in workspace.
     * selProject variable will contain value of current selected project.
     */
    private void loadProject() {
        try {
            waProjManager = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(myModule)));
        } catch (Exception e) {
            PluginUtil.displayErrorDialog( message("remAccSyntaxErr"), message("proPageErrMsgBox1") + message("proPageErrMsgBox2"));
            log(message("remAccErProjLoad"), e);
        }
    }

    @Override
    public String getHelpTopic() {
        return "windows_azure_project_remote_access_property";
    }

}


