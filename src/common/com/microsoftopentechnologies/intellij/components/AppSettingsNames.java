package com.microsoftopentechnologies.intellij.components;

/** NOTE: If you add new setting names to this list, evaluate whether it should be cleared
  * when the plugin is upgraded/uninstalled and add the setting to the array "settings" in
  * the "cleanTempData" function below. Otherwise your setting will get retained across
  * upgrades which can potentially cause issues.
  */
public class AppSettingsNames {
    public static final String O365_AUTHENTICATION_TOKEN = "com.microsoftopentechnologies.intellij.O365AuthenticationToken";
    public static final String SUBSCRIPTION_FILE = "com.microsoftopentechnologies.intellij.SubscriptionFile";
    public static final String SELECTED_SUBSCRIPTIONS = "com.microsoftopentechnologies.intellij.SelectedSubscriptions";
    public static final String AZURE_AUTHENTICATION_MODE = "com.microsoftopentechnologies.intellij.AzureAuthenticationMode";
    public static final String AZURE_AUTHENTICATION_TOKEN = "com.microsoftopentechnologies.intellij.AzureAuthenticationToken";
    public static final String CURRENT_PLUGIN_VERSION = "com.microsoftopentechnologies.intellij.PluginVersion";
}
