package com.microsoftopentechnologies.tooling.msservices.helpers;

public interface UIHelper {
    void showException(final String message, final Throwable ex);

    void showException(final String message, final Throwable ex, final String title);

    void showException(final String message,
                                     final Throwable ex,
                                     final String title,
                                     final boolean appendEx,
                                     final boolean suggestDetail);

    public void showError(String message, String title);
}
