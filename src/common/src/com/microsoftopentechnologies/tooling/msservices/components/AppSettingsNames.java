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
package com.microsoftopentechnologies.tooling.msservices.components;

/**
 * NOTE: If you add new setting names to this list, evaluate whether it should be cleared
 * when the plugin is upgraded/uninstalled and add the setting to the array "settings" in
 * the "cleanTempData" function below. Otherwise your setting will get retained across
 * upgrades which can potentially cause issues.
 */
public class AppSettingsNames {
    public static final String CURRENT_PLUGIN_VERSION = "com.microsoftopentechnologies.intellij.PluginVersion";
    public static final String EXTERNAL_STORAGE_ACCOUNT_LIST = "com.microsoftopentechnologies.intellij.ExternalStorageAccountList";

    public static final String AAD_AUTHENTICATION_RESULTS = "com.microsoftopentechnologies.tooling.msservices.AADAuthenticationResults";
    public static final String O365_USER_INFO = "com.microsoftopentechnologies.tooling.msservices.O365UserInfo";
    public static final String AZURE_SUBSCRIPTIONS = "com.microsoftopentechnologies.intellij.AzureSubscriptions";
    public static final String AZURE_USER_INFO = "com.microsoftopentechnologies.intellij.AzureUserInfo";
    public static final String AZURE_USER_SUBSCRIPTIONS = "com.microsoftopentechnologies.intellij.AzureUserSubscriptions";
}