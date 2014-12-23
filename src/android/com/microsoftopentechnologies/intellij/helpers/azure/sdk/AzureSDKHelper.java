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
package com.microsoftopentechnologies.intellij.helpers.azure.sdk;

import com.intellij.ide.util.PropertiesComponent;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.utils.Base64;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementService;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoftopentechnologies.intellij.components.MSOpenTechTools;
import com.microsoftopentechnologies.intellij.helpers.OpenSSLHelper;
import com.microsoftopentechnologies.intellij.helpers.XmlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class AzureSDKHelper {
    private static class SubscriptionInfo {
        public String base64Certificate;
        public String managementURI;
    }

    @Nullable
    public static ComputeManagementClient getComputeManagementClient(@NotNull String subscriptionId)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException, ParserConfigurationException, SAXException {
        Configuration configuration = getConfiguration(subscriptionId);

        if (configuration == null) {
            return null;
        }

        return ComputeManagementService.create(configuration);
    }

    @Nullable
    public static ManagementClient getManagementClient(@NotNull String subscriptionId)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException, ParserConfigurationException, SAXException {
        Configuration configuration = getConfiguration(subscriptionId);

        if (configuration == null) {
            return null;
        }

        return configuration.create(ManagementClient.class);
    }

    @Nullable
    private static Configuration getConfiguration(@NotNull String subscriptionId)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        SubscriptionInfo subscriptionInfo = getSubscriptionInfo(subscriptionId);

        if (subscriptionInfo == null) {
            return null;
        }

        String keyStorePath = File.createTempFile("azk", null).getPath();

        initKeyStore(
                subscriptionInfo.base64Certificate != null ? subscriptionInfo.base64Certificate : "",
                OpenSSLHelper.PASSWORD,
                keyStorePath,
                OpenSSLHelper.PASSWORD);

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(AzureSDKHelper.class.getClassLoader());

        try {
            return ManagementConfiguration.configure(URI.create(subscriptionInfo.managementURI), subscriptionId, keyStorePath, OpenSSLHelper.PASSWORD, KeyStoreType.pkcs12);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Nullable
    private static SubscriptionInfo getSubscriptionInfo(@NotNull String subscriptionId)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        String publishSettings = PropertiesComponent.getInstance().getValue(MSOpenTechTools.AppSettingsNames.SUBSCRIPTION_FILE, "");

        if (publishSettings.isEmpty()) {
            return null;
        }

        Node node = null;

        NodeList subsList = (NodeList) XmlHelper.getXMLValue(publishSettings, "//PublishData/PublishProfile/Subscription", XPathConstants.NODESET);

        for (int i = 0; i < subsList.getLength(); i++) {
            String id = XmlHelper.getAttributeValue(subsList.item(i), "Id");

            if (id.equals(subscriptionId)) {
                node = subsList.item(i);
                break;
            }
        }

        if (node == null) {
            return null;
        }

        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.base64Certificate = XmlHelper.getAttributeValue(node, "ManagementCertificate");
        subscriptionInfo.managementURI = XmlHelper.getAttributeValue(node, "ServiceManagementUrl");

        return subscriptionInfo;
    }

    private static void initKeyStore(@NotNull String base64Certificate, @NotNull String certificatePwd, @NotNull String keyStorePath, @NotNull String keyStorePwd)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStorePath);

        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, null);

            final byte[] decode = Base64.decode(base64Certificate);
            InputStream sslInputStream = new ByteArrayInputStream(decode);
            store.load(sslInputStream, certificatePwd.toCharArray());

            // we need to a create a physical key store as well here
            store.store(keyStoreOutputStream, keyStorePwd.toCharArray());
        } finally {
            keyStoreOutputStream.close();
        }
    }
}