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

package com.microsoftopentechnologies.intellij.helpers.azure;

import com.microsoftopentechnologies.intellij.helpers.NoSubscriptionException;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.model.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public interface AzureManager {
    void clearSubscriptions() throws AzureCmdException;

    void clearAuthenticationTokens();

    ArrayList<Subscription> getSubscriptionList() throws AzureCmdException;

    ArrayList<Subscription> getFullSubscriptionList() throws AzureCmdException;

    AzureAuthenticationMode getAuthenticationMode();

    void setAuthenticationMode(AzureAuthenticationMode azureAuthenticationMode);

    AuthenticationResult getAuthenticationToken();

    void setAuthenticationToken(AuthenticationResult authenticationToken);

    AuthenticationResult getAuthenticationTokenForSubscription(String subscriptionId);

    public void setAuthenticationTokenForSubscription(String subscriptionId, AuthenticationResult authenticationToken);

    public Subscription getSubscriptionFromId(final String subscriptionId) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, ExecutionException, InterruptedException, KeyManagementException, KeyStoreException, AzureCmdException, NoSubscriptionException;

    void loadSubscriptionFile(String subscriptionFile) throws AzureCmdException;

    void removeSubscription(String subscriptionId) throws AzureCmdException;

    List<SqlDb> getSqlDb(UUID subscriptionId, SqlServer server) throws AzureCmdException;

    List<SqlServer> getSqlServers(UUID subscriptionId) throws AzureCmdException;

    void createService(UUID subscriptionId, String region, String username, String password, String serviceName, String server, String database) throws AzureCmdException;

    List<Service> getServiceList(UUID subscriptionId) throws AzureCmdException;

    List<Table> getTableList(UUID subscriptionId, String serviceName) throws AzureCmdException;

    void createTable(UUID subscriptionId, String serviceName, String tableName, TablePermissions permissions) throws AzureCmdException;

    void updateTable(UUID subscriptionId, String serviceName, String tableName, TablePermissions permissions) throws AzureCmdException;

    Table showTableDetails(UUID subscriptionId, String serviceName, String tableName) throws AzureCmdException;

    void downloadTableScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException;

    void uploadTableScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException;

    List<CustomAPI> getAPIList(UUID subscriptionId, String serviceName) throws AzureCmdException;

    void downloadAPIScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException;

    void uploadAPIScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException;

    void createCustomAPI(UUID subscriptionId, String serviceName, String tableName, CustomAPIPermissions permissions) throws AzureCmdException;

    void updateCustomAPI(UUID subscriptionId, String serviceName, String tableName, CustomAPIPermissions permissions) throws AzureCmdException;

    List<Job> listJobs(UUID subscriptionId, String serviceName) throws AzureCmdException;

    void createJob(UUID subscriptionId, String serviceName, String jobName, int interval, String intervalUnit, String startDate) throws AzureCmdException;

    void updateJob(UUID subscriptionId, String serviceName, String jobName, int interval, String intervalUnit, String startDate, boolean enabled) throws AzureCmdException;

    void downloadJobScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException;

    void uploadJobScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException;

    List<LogEntry> listLog(UUID subscriptionId, String serviceName, String runtime) throws AzureCmdException, ParseException;

    List<String> getLocations(UUID subscriptionId) throws AzureCmdException;

    void setSelectedSubscriptions(List<UUID> selectedList) throws AzureCmdException;
}
