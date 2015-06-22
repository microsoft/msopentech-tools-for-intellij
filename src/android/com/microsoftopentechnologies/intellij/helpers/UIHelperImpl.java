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
import com.microsoftopentechnologies.intellij.forms.ErrorMessageForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.Nullable;
import com.microsoftopentechnologies.tooling.msservices.helpers.UIHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;

public class UIHelperImpl implements UIHelper {
    @Override
    public void showException(@NotNull final String message, @Nullable final Throwable ex) {
        showException(message, ex, "Error");
    }

    @Override
    public void showException(@NotNull final String message, @Nullable final Throwable ex, @NotNull final String title) {
        showException(message, ex, title, !(ex instanceof AzureCmdException), false);
    }

    @Override
    public void showException(@NotNull final String message,
                              @Nullable final Throwable ex,
                              @NotNull final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                String headerMessage = getHeaderMessage(message, ex, appendEx, suggestDetail);

                String details = getDetails(ex);

                ErrorMessageForm em = new ErrorMessageForm(title);
                em.setCursor(Cursor.getDefaultCursor());
                packAndCenterJDialog(em);
                em.showErrorMessageForm(headerMessage, details);
                em.setVisible(true);
            }
        });
    }

    @Override
    public void showError(@NotNull final String message, @NotNull final String title) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void packAndCenterJDialog(@NotNull JDialog form) {
        form.pack();
        form.setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - form.getWidth() / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - form.getHeight() / 2);
    }

    @NotNull
    private static String getHeaderMessage(@NotNull String message, @Nullable Throwable ex,
                                           boolean appendEx, boolean suggestDetail) {
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

    @NotNull
    private static String getDetails(@Nullable Throwable ex) {
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

    @NotNull
    public static ImageIcon loadIcon(@Nullable String name) {
        java.net.URL url = UIHelperImpl.class.getResource("/com/microsoftopentechnologies/intellij/icons/" + name);
        return new ImageIcon(url);
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}