/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.helpers;

import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.ErrorMessageForm;
import com.microsoftopentechnologies.intellij.forms.ImportSubscriptionForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.UIHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;

public class UIHelperImpl implements UIHelper {
    public void packAndCenterJDialog(JDialog form) {
        form.pack();
        form.setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - form.getWidth() / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - form.getHeight() / 2);
    }

    public void showException(final String message, final Throwable ex) {
        showException(message, ex, "Error");
    }

    public void showException(final String message, final Throwable ex, final String title) {

        showException(message, ex, title, !(ex instanceof AzureCmdException), false);
    }

    public void showException(final String message,
                                     final Throwable ex,
                                     final String title,
                                     final boolean appendEx,
                                     final boolean suggestDetail) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO: Verify if this side-effect is legacy code and remove it
                if (ex instanceof AzureCmdException) {
                    String errorLog = ((AzureCmdException) ex).getErrorLog();

                    //Not showing error if no account info found
                    if (errorLog != null &&
                            (errorLog.contains("No publish settings file found.") ||
                                    errorLog.contains("No account information found."))) {
                        JOptionPane.showMessageDialog(null,
                                "No account information found. Please import subscription information.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);

                        // TODO: This should probably be showing the "Manage Subscriptions" form instead since
                        // we also support A/D auth now.
                        ImportSubscriptionForm isf = new ImportSubscriptionForm();
                        DefaultLoader.getUIHelper().packAndCenterJDialog(isf);
                        isf.setVisible(true);

                        return;
                    }
                }

                String headerMessage = getHeaderMessage(message, ex, appendEx, suggestDetail);

                String details = getDetails(ex);

                ErrorMessageForm em = new ErrorMessageForm(title);
                em.setCursor(Cursor.getDefaultCursor());
                DefaultLoader.getUIHelper().packAndCenterJDialog(em);
                em.showErrorMessageForm(headerMessage, details);
                em.setVisible(true);
            }
        });
    }

    public void showError(final String message, final String title) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static String getHeaderMessage(String message, Throwable ex, boolean appendEx, boolean suggestDetail) {
        String headerMessage = message.trim();

        if (ex != null && appendEx) {
            String exMessage = (ex.getLocalizedMessage() == null || ex.getLocalizedMessage().isEmpty()) ? ex.getMessage() : ex.getLocalizedMessage();
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + exMessage;
        }

        if (suggestDetail) {
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + "Click on '" + ErrorMessageForm.advancedInfoText + "' for detailed information on the cause of the error.";
        }

        return headerMessage;
    }

    private static String getDetails(Throwable ex) {
        String details = "";

        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            details = sw.toString();

            if (ex instanceof AzureCmdException) {
                String errorLog = ((AzureCmdException) ex).getErrorLog();
                if (errorLog != null) {
                    details = errorLog;
                }
            }
        }

        return details;
    }

    public ImageIcon loadIcon(String name) {
        java.net.URL url = UIHelperImpl.class.getResource("/com/microsoftopentechnologies/intellij/icons/" + name);
        return new ImageIcon(url);
    }

    public String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}