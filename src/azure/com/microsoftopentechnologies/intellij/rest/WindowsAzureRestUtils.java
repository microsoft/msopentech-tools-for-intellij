/**
 * Copyright 2014 Microsoft Open Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoft.windowsazure.management.configuration.PublishSettingsLoader;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

/**
 * this is a temporary class;
 * to be removed when classloader issue fixed (in Azure Java SDK or AzureManagementUtil)
 */
public class WindowsAzureRestUtils {
    public static Configuration getConfiguration(File file, String subscriptionId) throws IOException {
        try {
            // Get current context class loader
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            // Change context classloader to class context loader
            Thread.currentThread().setContextClassLoader(WindowsAzureRestUtils.class.getClassLoader());
            Configuration configuration = PublishSettingsLoader.createManagementConfiguration(file.getPath(), subscriptionId);
            // Call Azure API and reset back the context loader
            Thread.currentThread().setContextClassLoader(contextLoader);
            log("Created configuration for subscriptionId: " + subscriptionId);
            return configuration;
        } catch (IOException ex) {
            log(message("error"), ex);
            throw ex;
        }
    }

    public static Configuration loadConfiguration(String subscriptionId, String url) throws IOException {
        String keystore = System.getProperty("user.home") + File.separator + ".azure" + File.separator + subscriptionId + ".out";
        URI mngUri = URI.create(url);
        // Get current context class loader
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        // Change context classloader to class context loader
        Thread.currentThread().setContextClassLoader(WindowsAzureRestUtils.class.getClassLoader());
        Configuration configuration = ManagementConfiguration.configure(null, Configuration.load(), mngUri, subscriptionId, keystore, "", KeyStoreType.pkcs12);
        // Call Azure API and reset back the context loader
        Thread.currentThread().setContextClassLoader(contextLoader);
        return configuration;
    }
}
