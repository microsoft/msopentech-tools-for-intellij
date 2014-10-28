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

package com.microsoftopentechnologies.intellij.components;

import com.google.gson.Gson;
import com.google.gson.stream.MalformedJsonException;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.helpers.AndroidStudioHelper;
import com.microsoftopentechnologies.intellij.helpers.StringHelper;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class MSOpenTechTools extends ApplicationComponent.Adapter {
    private static MSOpenTechTools current = null;

    // TODO: This needs to be the plugin ID from plugin.xml somehow.
    public static final String PLUGIN_ID = "com.microsoftopentechnologies.intellij";

    // NOTE: If you add new setting names to this list, evaluate whether it should be cleared
    // when the plugin is upgraded/uninstalled and add the setting to the array "settings" in
    // the "cleanTempData" function below. Otherwise your setting will get retained across
    // upgrades which can potentially cause issues.
    public static class AppSettingsNames {
        public static final String O365_AUTHENTICATION_TOKEN = "com.microsoftopentechnologies.intellij.O365AuthenticationToken";
        public static final String SUBSCRIPTION_FILE = "com.microsoftopentechnologies.intellij.SubscriptionFile";
        public static final String AZURE_AUTHENTICATION_MODE = "com.microsoftopentechnologies.intellij.AzureAuthenticationMode";
        public static final String AZURE_AUTHENTICATION_TOKEN = "com.microsoftopentechnologies.intellij.AzureAuthenticationToken";
        public static final String CLEAN_TEMP_DATA = "com.microsoftopentechnologies.intellij.CleanTempData";
        public static final String CURRENT_PLUGIN_VERSION = "com.microsoftopentechnologies.intellij.PluginVersion";
    }

    private PluginSettings settings;

    public MSOpenTechTools() {
    }

    public void initComponent() {
        // save the object instance
        current = this;

        // load up the plugin settings
        try {
            loadPluginSettings();
        }
        catch (IOException e) {
            UIHelper.showException("An error occurred while attempting to load " +
                    "settings for the MSOpenTech Tools plugin.", e);
        }

        // delete android studio activity templates
        cleanTempData(PropertiesComponent.getInstance());
    }

    private void loadPluginSettings() throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                        new InputStreamReader(
                                MSOpenTechTools.class.getResourceAsStream("/settings.json")));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            settings = gson.fromJson(sb.toString(), PluginSettings.class);
        }
        finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public static MSOpenTechTools getCurrent() {
        return current;
    }

    private void cleanTempData(PropertiesComponent propComp) {
        String cleanTempData = propComp.getValue(AppSettingsNames.CLEAN_TEMP_DATA, "");

        //Setted for constant cleaning for testing
        //cleanTempData = "";

        // check the plugin version stored in the properties; if it
        // doesn't match with the current plugin version then we clear
        // all stored options
        // TODO: The authentication tokens are stored with the subscription id appended as a
        // suffix to AZURE_AUTHENTICATION_TOKEN. So clearing that requires that we enumerate the
        // current subscriptions and iterate over that list to clear the auth tokens for those
        // subscriptions.
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String currentPluginVersion = properties.getValue(AppSettingsNames.CURRENT_PLUGIN_VERSION);
        if(StringHelper.isNullOrWhiteSpace(currentPluginVersion) ||
           !getSettings().getPluginVersion().equals(currentPluginVersion)) {

            String[] settings = new String[] {
                    AppSettingsNames.AZURE_AUTHENTICATION_MODE,
                    AppSettingsNames.AZURE_AUTHENTICATION_TOKEN,
                    AppSettingsNames.O365_AUTHENTICATION_TOKEN,
                    AppSettingsNames.SUBSCRIPTION_FILE
            };

            for(String setting : settings) {
                properties.unsetValue(setting);
            }
        }

        // save the current plugin version
        properties.setValue(AppSettingsNames.CURRENT_PLUGIN_VERSION, getSettings().getPluginVersion());

        // TODO: Instead of checking for an "ok" value to decide whether the activity templates in Android Studio
        // should be deleted or not, we should version the Android Studio templates zip and check if the version
        // currently installed matches what's shipped in the plugin or not. If it doesn't match then we delete.
        if(cleanTempData.isEmpty()) {
            try {
                if(AndroidStudioHelper.isAndroidStudio())
                    AndroidStudioHelper.deleteActivityTemplates(this);

                String tmpdir = System.getProperty("java.io.tmpdir");
                StringBuilder sb = new StringBuilder();
                sb.append(tmpdir);

                if(!tmpdir.endsWith(File.separator))
                    sb.append(File.separator);

                sb.append("TempAzure");

                final VirtualFile tempFolder = LocalFileSystem.getInstance().findFileByIoFile(new File(sb.toString()));
                if(tempFolder != null && tempFolder.exists()) {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                tempFolder.delete(MSOpenTechTools.getCurrent());
                            } catch (IOException ignored) {}
                        }
                    });
                }

                propComp.setValue(AppSettingsNames.CLEAN_TEMP_DATA, "OK");

            } catch (Exception e) {
                UIHelper.showException("Error deleting older templates", e);
            }
        }
    }

    @NotNull
    public String getComponentName() {
        return "MSOpenTechTools";
    }

    public PluginSettings getSettings() {
        return settings;
    }
}
