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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.pipeline.apache.ApacheConfigurationProperties;
import com.microsoft.windowsazure.core.utils.Base64;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.ManagementService;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementService;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoft.windowsazure.management.network.NetworkManagementClient;
import com.microsoft.windowsazure.management.network.NetworkManagementService;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoft.windowsazure.management.storage.StorageManagementService;
import com.microsoftopentechnologies.tooling.msservices.components.AppSettingsNames;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.OpenSSLHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.XmlHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class AzureSDKHelper {
    private static class SubscriptionInfo {
        public String base64Certificate;
        public String managementURI;
    }

    public static ComputeManagementClient getComputeManagementClient(String subscriptionId)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException, ParserConfigurationException, SAXException {
        Configuration configuration = getConfiguration(subscriptionId);

        if (configuration == null) {
            return null;
        }

        ComputeManagementClient client = ComputeManagementService.create(configuration);

        // add a request filter for tacking on the A/D auth token if the current authentication
        // mode is active directory
        if (AzureRestAPIManagerImpl.getManager().getAuthenticationMode() == AzureAuthenticationMode.ActiveDirectory) {
            return client.withRequestFilterFirst(new AuthTokenRequestFilter(subscriptionId));
        }

        return client;
    }

    public static StorageManagementClient getStorageManagementClient(String subscriptionId)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException, ParserConfigurationException, SAXException {
        Configuration configuration = getConfiguration(subscriptionId);

        if (configuration == null) {
            return null;
        }

        StorageManagementClient client = StorageManagementService.create(configuration);

        // add a request filter for tacking on the A/D auth token if the current authentication
        // mode is active directory
        if (AzureRestAPIManagerImpl.getManager().getAuthenticationMode() == AzureAuthenticationMode.ActiveDirectory) {
            return client.withRequestFilterFirst(new AuthTokenRequestFilter(subscriptionId));
        }

        return client;
    }

    public static NetworkManagementClient getNetworkManagementClient(String subscriptionId)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException, ParserConfigurationException, SAXException {
        Configuration configuration = getConfiguration(subscriptionId);

        if (configuration == null) {
            return null;
        }

        NetworkManagementClient client = NetworkManagementService.create(configuration);

        // add a request filter for tacking on the A/D auth token if the current authentication
        // mode is active directory
        if (AzureRestAPIManagerImpl.getManager().getAuthenticationMode() == AzureAuthenticationMode.ActiveDirectory) {
            return client.withRequestFilterFirst(new AuthTokenRequestFilter(subscriptionId));
        }

        return client;
    }

    public static ManagementClient getManagementClient(String subscriptionId)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException, ParserConfigurationException, SAXException {
        Configuration configuration = getConfiguration(subscriptionId);

        if (configuration == null) {
            return null;
        }

        ManagementClient client = ManagementService.create(configuration);

        // add a request filter for tacking on the A/D auth token if the current authentication
        // mode is active directory
        if (AzureRestAPIManagerImpl.getManager().getAuthenticationMode() == AzureAuthenticationMode.ActiveDirectory) {
            return client.withRequestFilterFirst(new AuthTokenRequestFilter(subscriptionId));
        }

        return client;
    }


    public static CloudStorageAccount getCloudStorageAccount(StorageAccount storageAccount)
            throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(String.format(StorageAccount.CONN_STR_TEMPLATE,
                storageAccount.getName(),
                storageAccount.getPrimaryKey()));
    }

    private static Configuration getConfiguration(String subscriptionId)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, XPathExpressionException,
            SAXException, ParserConfigurationException, IOException {
        switch (AzureRestAPIManagerImpl.getManager().getAuthenticationMode()) {
            case SubscriptionSettings:
                return getConfigurationFromPublishSettings(subscriptionId);
            case ActiveDirectory:
                return getConfigurationFromAuthToken(subscriptionId);
        }

        return null;
    }

    private static Configuration getConfigurationFromAuthToken(String subscriptionId)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // NOTE: This implementation has to be considered as somewhat hacky. It relies on certain
        // internal implementation details of the Azure SDK for Java. For example we supply null
        // values for the key store location and password and specify a key store type value
        // though it will not be used. We also supply a no-op "credential provider". Ideally we want
        // the SDK to directly support the scenario we need.

        String azureServiceManagementUri = DefaultLoader.getPluginComponent().getSettings().getAzureServiceManagementUri();

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(AzureSDKHelper.class.getClassLoader());

        try {
            // create a default configuration object
            Configuration configuration = ManagementConfiguration.configure(
                    URI.create(azureServiceManagementUri),
                    subscriptionId, null, null, KeyStoreType.pkcs12);

            if (configuration != null) {
                // replace the credential provider with a custom one that does nothing
                configuration.setProperty(
                        ManagementConfiguration.SUBSCRIPTION_CLOUD_CREDENTIALS,
                        new EmptyCloudCredentials(subscriptionId));

                // remove the SSL connection factory in case one was added; this is needed
                // in the case when the user switches from subscription based auth to A/D
                // sign-in because in that scenario the CertificateCloudCredentials class
                // would have added an SSL connection factory object to the configuration
                // object which would then be used when making the SSL call to the Azure
                // service management API. This tells us that the configuration object is
                // reused across calls to ManagementConfiguration.configure. The SSL connection
                // factory object so configured will attempt to use certificate based auth
                // which will fail since we don't have a certificate handy when using A/D auth.
                configuration.getProperties().remove(ApacheConfigurationProperties.PROPERTY_SSL_CONNECTION_SOCKET_FACTORY);
            }

            return configuration;
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private static Configuration getConfigurationFromPublishSettings(String subscriptionId)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        SubscriptionInfo subscriptionInfo = getSubscriptionInfoFromPublishSettings(subscriptionId);

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

    private static SubscriptionInfo getSubscriptionInfoFromPublishSettings(String subscriptionId)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        String publishSettings = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.SUBSCRIPTION_FILE, "");

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

    private static void initKeyStore(String base64Certificate, String certificatePwd, String keyStorePath, String keyStorePwd)
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