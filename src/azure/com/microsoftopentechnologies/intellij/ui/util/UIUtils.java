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

package com.microsoftopentechnologies.intellij.ui.util;

import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.roleoperations.JdkSrvConfigUtilMethods;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class UIUtils {

    public static ActionListener createFileChooserListener(final TextFieldWithBrowseButton parent, final @Nullable Project project,
                                                           final FileChooserDescriptor descriptor) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createAllButJarContentsDescriptor();
//                DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
                final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parent, project,
                        (project == null) && !parent.getText().isEmpty() ? LocalFileSystem.getInstance().findFileByPath(parent.getText()) : null);
                if (files.length > 0) {
                    final StringBuilder builder = new StringBuilder();
                    for (VirtualFile file : files) {
                        if (builder.length() > 0) {
                            builder.append(File.pathSeparator);
                        }
                        builder.append(FileUtil.toSystemDependentName(file.getPath()));
                    }
                    parent.setText(builder.toString());
                }
            }
        };
    }

    public static ActionListener createFileChooserListener(final JTextField parent, final @Nullable Project project,
                                                           final FileChooserDescriptor descriptor) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parent, project,
                        (project == null) && !parent.getText().isEmpty() ? LocalFileSystem.getInstance().findFileByPath(parent.getText()) : null);
                if (files.length > 0) {
                    final StringBuilder builder = new StringBuilder();
                    for (VirtualFile file : files) {
                        if (builder.length() > 0) {
                            builder.append(File.pathSeparator);
                        }
                        builder.append(FileUtil.toSystemDependentName(file.getPath()));
                    }
                    parent.setText(builder.toString());
                }
            }
        };
    }

    public static ActionListener createFileChooserListener(final TextFieldWithBrowseButton parent, final @Nullable Project project,
                                                           final FileChooserDescriptor descriptor, final Consumer<List<VirtualFile>> consumer) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileChooser.chooseFiles(descriptor, project, parent,
                        parent.getText().isEmpty() ? null : LocalFileSystem.getInstance().findFileByPath(parent.getText()), consumer);
            }
        };
    }

    /**
     * Method validates remote access password.
     *
     * @param isPwdChanged
     *            : flag to monitor whether password is changed or not
     * @param txtPassword
     *            : Object of password text box
     * @param waProjManager
     *            : WindowsAzureProjectManager object
     * @param isRAPropPage
     *            : flag to monitor who has called this method Encryption link
     *            or normal property page call.
     * @param txtConfirmPassword
     *            : Object of confirm password text box
     */
    public static void checkRdpPwd(boolean isPwdChanged, JPasswordField txtPassword,
                                   WindowsAzureProjectManager waProjManager, boolean isRAPropPage,
                                   JPasswordField txtConfirmPassword) {
        Pattern pattern = Pattern
                .compile("(?=^.{6,}$)(?=.*\\d)(?=.*[A-Z])(?!.*\\s)(?=.*[a-z]).*$|"
                        + "(?=^.{6,}$)(?=.*\\d)(?!.*\\s)(?=.*[a-z])(?=.*\\p{Punct}).*$|"
                        + "(?=^.{6,}$)(?=.*\\d)(?!.*\\s)(?=.*[A-Z])(?=.*\\p{Punct}).*$|"
                        + "(?=^.{6,}$)(?=.*[A-Z])(?=.*[a-z])(?!.*\\s)(?=.*\\p{Punct}).*$");
        Matcher match = pattern.matcher(String.valueOf(txtPassword.getPassword()));
        try {
			/*
			 * checking if user has changed the password and that field is not
			 * blank then check for strong password else set the old password.
			 */
            if (isPwdChanged) {
                if (!(txtPassword.getPassword().length == 0) && !match.find()) {
                    PluginUtil.displayErrorDialog(message("remAccErPwdNtStrg"), message("remAccPwdNotStrg"));
                    txtConfirmPassword.setText("");
                    txtPassword.requestFocusInWindow();
                }
            } else {
                String pwd = waProjManager.getRemoteAccessEncryptedPassword();
				/*
				 * Remote access property page accessed via context menu
				 */
                if (isRAPropPage) {
                    txtPassword.setText(pwd);
                } else {
					/*
					 * Remote access property page accessed via encryption link
					 */
                    if (!pwd.equals(message("remAccDummyPwd"))) {
                        txtPassword.setText(pwd);
                    }
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e1) {
            PluginUtil.displayErrorDialogAndLog(message("remAccErrTitle"), message("remAccErPwd"), e1);
        }
    }

    /**
     * Method extracts data from publish settings file
     * and create Publish data object.
     * @param file
     * @return
     */
    public static PublishData createPublishDataObj(File file) {
        PublishData data;
        try {
            data = com.microsoftopentechnologies.azurecommons.deploy.util.UIUtils.parse(file);
        } catch (JAXBException e) {
            PluginUtil.displayErrorDialogAndLog(message("importDlgTitle"), String.format(message("importDlgMsg"), file.getName(), message("failedToParse")), e);
            return null;
        }
        String subscriptionId = data.getSubscriptionIds().get(0);
        if (WizardCacheManager.findPublishDataBySubscriptionId(subscriptionId) != null) {
            PluginUtil.displayInfoDialog(message("loadingCred"), message("credentialsExist"));
        }
        data.setCurrentSubscription(data.getPublishProfile().getSubscriptions().get(0));
        return data;
    }

    /**
     * Method populates subscription names into subscription
     * combo box.
     * @param combo
     * @return
     */
    public static JComboBox populateSubscriptionCombo(JComboBox combo) {
        ElementWrapper<?> currentSelection = (ElementWrapper<?>) combo.getSelectedItem();
        combo.removeAllItems();
        Collection<PublishData> publishes = WizardCacheManager.getPublishDatas();
        if (publishes.size() > 0) {
            for (PublishData pd : publishes) {
                for (Subscription sub : pd.getPublishProfile().getSubscriptions()) {
                    combo.addItem(new ElementWrapper<PublishData>(sub.getName(), pd));
                }
            }
            if (currentSelection != null) {
                selectByText(combo, currentSelection.getKey());
            }
        }
        return combo;
    }

    /**
     * Set current subscription and publish data
     * as per subscription selected in combo box.
     * @param combo
     * @return
     */
    public static PublishData changeCurrentSubAsPerCombo(JComboBox combo) {
        PublishData publishData = null;
        ElementWrapper<PublishData> currentEntry = (ElementWrapper<PublishData>) combo.getSelectedItem();
        String subscriptionName = currentEntry == null ? null : currentEntry.getKey();
        if (subscriptionName != null && !subscriptionName.isEmpty()) {
            publishData = currentEntry.getValue();
            Subscription sub = WizardCacheManager.findSubscriptionByName(subscriptionName);
            if (publishData != null) {
                publishData.setCurrentSubscription(sub);
                WizardCacheManager.setCurrentPublishData(publishData);
            }
        }
        return publishData;
    }

    /**
     * Method populates storage account name associated
     * with the component's access key.
     * @param key
     * @param combo
     */
    public static void populateStrgNameAsPerKey(String key, JComboBox combo) {
        combo.setSelectedItem(JdkSrvConfigUtilMethods.getNameToSetAsPerKey(key, StorageRegistryUtilMethods.getStorageAccountNames(false)));
    }

    /**
     * Select item from combo box as per item name.
     * By finding out selection index as per name.
     * @param combo
     * @param name
     * @return
     */
    public static JComboBox selectByText(JComboBox combo, String name) {
        if (combo.getItemCount() > 0 && name != null && !name.isEmpty()) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                String itemText = ((ElementWrapper) combo.getItemAt(i)).getKey();
                if (name.equals(itemText)) {
                    combo.setSelectedIndex(i);
                    return combo;
                }
            }
        }
        combo.setSelectedIndex(0);
        return combo;
    }

    public static class ElementWrapper<T> {
        private String key;
        private T value;

        public ElementWrapper(String key, T value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
