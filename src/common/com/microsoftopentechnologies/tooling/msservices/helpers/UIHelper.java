package com.microsoftopentechnologies.tooling.msservices.helpers;

import javax.swing.*;

public interface UIHelper {
    void packAndCenterJDialog(JDialog form);

    void showException(final String message, final Throwable ex);

    void showException(final String message, final Throwable ex, final String title);

    void showException(final String message,
                                     final Throwable ex,
                                     final String title,
                                     final boolean appendEx,
                                     final boolean suggestDetail);

    public void showError(String message, String title);

    ImageIcon loadIcon(String name);

    String readableFileSize(long size);
}
