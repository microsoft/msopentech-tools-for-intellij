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

package com.microsoftopentechnologies.intellij.helpers;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.microsoftopentechnologies.intellij.components.MSOpenTechTools;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.model.*;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.misc.BASE64Encoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class AzureRestAPIManager implements AzureManager {
    // singleton API manager instance
    private static AzureRestAPIManager apiManager = null;

    // This is the authentication token.
    // TODO: Should we store this encrypted in memory?
    // TODO: Implement offline encrypted caching so that user doesn't have to re-authenticate every time they run.
    private AuthenticationResult authenticationToken;
    private ReentrantLock authenticationTokenLock = new ReentrantLock();

    // list of azure subscriptions
    private ArrayList<Subscription> subscriptions;
    private ReentrantLock subscriptionsLock = new ReentrantLock();

    // cache of authentication tokens by azure subscription ID
    private Map<String, AuthenticationResult> authenticationTokenSubscriptionMap =
            new HashMap<String, AuthenticationResult>();
    private ReentrantLock authenticationTokenSubscriptionMapLock = new ReentrantLock();

    private AzureRestAPIManager() {
    }

    public static AzureManager getManager() {
        if(apiManager == null) {
            apiManager = new AzureRestAPIManager();
        }

        return apiManager;
    }

    @Override
    public AzureAuthenticationMode getAuthenticationMode() {
        return AzureAuthenticationMode.valueOf(
                PropertiesComponent.getInstance().getValue(
                        MSOpenTechTools.AppSettingsNames.AZURE_AUTHENTICATION_MODE,
                        AzureAuthenticationMode.Unknown.toString()));
    }

    @Override
    public void setAuthenticationMode(AzureAuthenticationMode azureAuthenticationMode) {
        PropertiesComponent.getInstance().setValue(
                MSOpenTechTools.AppSettingsNames.AZURE_AUTHENTICATION_MODE,
                azureAuthenticationMode.toString());
    }

    public AuthenticationResult getAuthenticationTokenForSubscription(String subscriptionId) {
        // build key for the properties cache
        String key = MSOpenTechTools.AppSettingsNames.AZURE_AUTHENTICATION_TOKEN + "_" + subscriptionId;

        // check if the token is already available in our cache
        if(authenticationTokenSubscriptionMap.containsKey(key)) {
            return authenticationTokenSubscriptionMap.get(key);
        }

        String json = PropertiesComponent.getInstance().getValue(key);
        if(!StringHelper.isNullOrWhiteSpace(json)) {
            Gson gson = new Gson();
            AuthenticationResult token = gson.fromJson(json, AuthenticationResult.class);

            // save the token to the cache
            authenticationTokenSubscriptionMapLock.lock();
            try {
                authenticationTokenSubscriptionMap.put(key, token);
            }
            finally {
                authenticationTokenSubscriptionMapLock.unlock();
            }
        }

        return authenticationTokenSubscriptionMap.get(key);
    }

    public void setAuthenticationTokenForSubscription(
            String subscriptionId,
            AuthenticationResult authenticationToken) {
        // build key for the properties cache
        String key = MSOpenTechTools.AppSettingsNames.AZURE_AUTHENTICATION_TOKEN + "_" + subscriptionId;

        authenticationTokenSubscriptionMapLock.lock();
        try {
            // update the token in the cache
            if(authenticationToken == null) {
                if(authenticationTokenSubscriptionMap.containsKey(key)) {
                    authenticationTokenSubscriptionMap.remove(key);
                }
            } else {
                authenticationTokenSubscriptionMap.put(key, authenticationToken);
            }

            // save the token in persistent storage
            String json = "";
            if(authenticationToken != null) {
                Gson gson = new Gson();
                json = gson.toJson(authenticationToken, AuthenticationResult.class);
            }
            PropertiesComponent.getInstance().setValue(key, json);
        }
        finally {
            authenticationTokenSubscriptionMapLock.unlock();
        }
    }

    @Override
    public AuthenticationResult getAuthenticationToken() {
        if(authenticationToken == null) {
            String json = PropertiesComponent.getInstance().getValue(MSOpenTechTools.AppSettingsNames.AZURE_AUTHENTICATION_TOKEN);
            if(!StringHelper.isNullOrWhiteSpace(json)) {
                Gson gson = new Gson();
                authenticationTokenLock.lock();
                try {
                    authenticationToken = gson.fromJson(json, AuthenticationResult.class);
                }
                finally {
                    authenticationTokenLock.unlock();
                }
            }
        }

        return authenticationToken;
    }

    @Override
    public void setAuthenticationToken(AuthenticationResult authenticationToken) {
        authenticationTokenLock.lock();

        try {
            this.authenticationToken = authenticationToken;
            String json = "";
            if (this.authenticationToken != null) {
                Gson gson = new Gson();
                json = gson.toJson(this.authenticationToken, AuthenticationResult.class);
            }

            PropertiesComponent.getInstance().setValue(MSOpenTechTools.AppSettingsNames.AZURE_AUTHENTICATION_TOKEN, json);
        }
        finally {
            authenticationTokenLock.unlock();
        }
    }

    @Override
    public void clearSubscriptions() throws AzureCmdException {
        PropertiesComponent.getInstance().unsetValue(MSOpenTechTools.AppSettingsNames.SUBSCRIPTION_FILE);
        subscriptionsLock.lock();
        try {
            if(subscriptions != null) {
                subscriptions.clear();
                subscriptions = null;
            }
        }
        finally {
            subscriptionsLock.unlock();
        }
    }

    @Override
    public void clearAuthenticationTokens() {
        if(subscriptions != null) {
            for(Subscription subscription : subscriptions) {
                setAuthenticationTokenForSubscription(subscription.getId().toString(), null);
            }
        }
        setAuthenticationToken(null);
    }

    @Override
    public ArrayList<Subscription> getSubscriptionList() throws AzureCmdException {
        try {
            AzureAuthenticationMode mode = getAuthenticationMode();
            if(mode == AzureAuthenticationMode.SubscriptionSettings) {
                return getSubscriptionListFromCert();
            } else if(mode == AzureAuthenticationMode.ActiveDirectory) {
                return getSubscriptionListFromToken();
            }

            return null;
        } catch (Exception e) {
            throw new AzureCmdException("Error getting subscription list", e);
        }
    }

    public ArrayList<Subscription> getSubscriptionListFromCert() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        String subscriptionFile = PropertiesComponent.getInstance().getValue(MSOpenTechTools.AppSettingsNames.SUBSCRIPTION_FILE, "");
        if(subscriptionFile.trim().isEmpty()) {
            return null;
        }
        NodeList subscriptionList = (NodeList) AzureRestAPIHelper.getXMLValue(subscriptionFile, "//Subscription", XPathConstants.NODESET);

        ArrayList<Subscription> list = new ArrayList<Subscription>();
        for (int i = 0; i < subscriptionList.getLength(); i++) {
            Subscription subscription = new Subscription();
            subscription.setName(AzureRestAPIHelper.getAttributeValue(subscriptionList.item(i), "Name"));
            subscription.setId(UUID.fromString(AzureRestAPIHelper.getAttributeValue(subscriptionList.item(i), "Id")));

            list.add(subscription);
        }

        return list;
    }

    public void refreshSubscriptionListFromToken() throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, ExecutionException, ParserConfigurationException, InterruptedException, AzureCmdException, SAXException, NoSubscriptionException, KeyStoreException, XPathExpressionException, KeyManagementException {
        String subscriptionXml = AzureRestAPIHelper.getRestApiCommand("subscriptions", null);
        PropertiesComponent.getInstance().setValue(MSOpenTechTools.AppSettingsNames.SUBSCRIPTION_FILE, subscriptionXml);
        NodeList subscriptionList = (NodeList) AzureRestAPIHelper.getXMLValue(
                subscriptionXml, "//Subscription", XPathConstants.NODESET);

        subscriptionsLock.lock();
        try {
            subscriptions = new ArrayList<Subscription>();
            for (int i = 0; i < subscriptionList.getLength(); i++) {
                Subscription subscription = new Subscription();
                subscription.setName(AzureRestAPIHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionName"));
                subscription.setId(UUID.fromString(AzureRestAPIHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionID")));
                subscription.setTenantId(AzureRestAPIHelper.getChildNodeValue(subscriptionList.item(i), "AADTenantID"));

                subscriptions.add(subscription);
            }
        }
        finally {
            subscriptionsLock.unlock();
        }
    }

    public ArrayList<Subscription> getSubscriptionListFromToken() throws AzureCmdException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, ExecutionException, ParserConfigurationException, InterruptedException, SAXException, NoSubscriptionException, KeyStoreException, XPathExpressionException, KeyManagementException {
        if(subscriptions == null) {
            refreshSubscriptionListFromToken();
            assert subscriptions != null;
        }

        return subscriptions;
    }

    public Subscription getSubscriptionFromId(final String subscriptionId) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, ExecutionException, InterruptedException, KeyManagementException, KeyStoreException, AzureCmdException, NoSubscriptionException {
        ArrayList<Subscription> subscriptions = null;
        AzureAuthenticationMode mode = getAuthenticationMode();
        if(mode == AzureAuthenticationMode.SubscriptionSettings) {
            subscriptions = getSubscriptionListFromCert();
        } else if(mode == AzureAuthenticationMode.ActiveDirectory) {
            subscriptions = getSubscriptionListFromToken();
        }

        if(subscriptions == null) {
            return null;
        }

        final UUID sid = UUID.fromString(subscriptionId);
        return Iterables.find(subscriptions, new Predicate<Subscription>() {
            @Override
            public boolean apply(Subscription subscription) {
                return subscription.getId().compareTo(sid) == 0;
            }
        });
    }

    @Override
    public void loadSubscriptionFile(String subscriptionFile) throws AzureCmdException {
        // update the auth mode and clear out the subscriptions xml
        setAuthenticationMode(AzureAuthenticationMode.SubscriptionSettings);
        apiManager.clearSubscriptions();

        AzureRestAPIHelper.importSubscription(new File(subscriptionFile));
    }

    @Override
    public void removeSubscription(String subscriptionId) throws AzureCmdException {
        try {
            AzureRestAPIHelper.removeSubscription(subscriptionId);
        } catch (Exception e) {
            throw new AzureCmdException("Error removing subscription", e);
        }
    }

    @Override
    public List<Service> getServiceList(UUID subscriptionId) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices", subscriptionId.toString());

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            CustomJsonSlurper slurper = new CustomJsonSlurper();
            List<Map<String, Object>> tempRes = (List<Map<String, Object>>) slurper.parseText(json);

            List<Service> res = new ArrayList<Service>();
            for (Map<String, Object> item : tempRes) {
                Service ser = new Service();

                ser.setName((String) item.get("name"));
                ser.setType((String) item.get("type"));
                ser.setState((String) item.get("state"));
                ser.setSelfLink((String) item.get("selflink"));
                ser.setAppUrl((String) item.get("applicationUrl"));
                ser.setAppKey((String) item.get("applicationKey"));
                ser.setMasterKey((String) item.get("masterKey"));
                ser.setWebspace((String) item.get("webspace"));
                ser.setRegion((String) item.get("region"));
                ser.setMgmtPortalLink((String) item.get("managementPortalLink"));
                ser.setSubcriptionId(subscriptionId);

                for (Map<String, String> table : (List<Map<String, String>>) item.get("tables")) {
                    Table t = new Table();
                    t.setName(table.get("name"));
                    t.setSelfLink(table.get("selflink"));
                    ser.getTables().add(t);
                }
                res.add(ser);
            }

            return res;

        } catch (Exception e) {
            throw new AzureCmdException("Error getting service list", e);
        }
    }

    @Override
    public List<String> getLocations(UUID subscriptionId) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/regions", subscriptionId.toString());

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            CustomJsonSlurper slurper = new CustomJsonSlurper();
            List<Map<String, String>> tempRes = (List<Map<String, String>>) slurper.parseText(json);

            List<String> res = new ArrayList<String>();
            for (Map<String, String> item : tempRes) {
                res.add(item.get("region"));
            }

            return res;
        } catch (Exception e) {
            throw new AzureCmdException("Error getting region list", e);
        }
    }

    @Override
    public List<SqlDb> getSqlDb(UUID subscriptionId, SqlServer server) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/sqlservers/servers/%s/databases?contentview=generic", subscriptionId.toString(), server.getName());
            String xml = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            List<SqlDb> res = new ArrayList<SqlDb>();
            NodeList nl = (NodeList) AzureRestAPIHelper.getXMLValue(xml, "//ServiceResource", XPathConstants.NODESET);
            for (int i = 0; i != nl.getLength(); i++) {

                SqlDb sqls = new SqlDb();
                sqls.setName(AzureRestAPIHelper.getChildNodeValue(nl.item(i), "Name"));
                sqls.setEdition(AzureRestAPIHelper.getChildNodeValue(nl.item(i), "Edition"));
                sqls.setServer(server);
                res.add(sqls);
            }

            return res;

        } catch (Exception e) {
            throw new AzureCmdException("Error getting database list", e);
        }
    }

    @Override
    public List<SqlServer> getSqlServers(UUID subscriptionId) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/sqlservers/servers", subscriptionId.toString());
            String xml = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            List<SqlServer> res = new ArrayList<SqlServer>();

            NodeList nl = (NodeList) AzureRestAPIHelper.getXMLValue(xml, "//Server", XPathConstants.NODESET);
            for (int i = 0; i != nl.getLength(); i++) {
                SqlServer sqls = new SqlServer();

                sqls.setAdmin(AzureRestAPIHelper.getChildNodeValue(nl.item(i), "AdministratorLogin"));
                sqls.setName(AzureRestAPIHelper.getChildNodeValue(nl.item(i), "Name"));
                sqls.setRegion(AzureRestAPIHelper.getChildNodeValue(nl.item(i), "Location"));
                res.add(sqls);
            }

            return res;

        } catch (Exception e) {
            throw new AzureCmdException("Error getting server list", e);
        }
    }

    @Override
    public void createService(UUID subscriptionId, String region, String username, String password, String serviceName, String server, String database) throws AzureCmdException {
        try {


            String path = String.format("/%s/applications", subscriptionId.toString());

            String JSONParameter = null;
            if (database == null || server == null) {

                String zumoServerId = UUID.randomUUID().toString().replace("-", "");
                String zumoDBId = UUID.randomUUID().toString().replace("-", "");
                String dbName = serviceName + "_db";

                JSONParameter = "{'SchemaVersion':'2012-05.1.0','Location':'" + region + "','ExternalResources':{},'InternalResources':{'ZumoMobileService':" +
                        "{'ProvisioningParameters':{'Name':'" + serviceName + "','Location':'" + region + "'},'ProvisioningConfigParameters':{'Server':{'StringConcat':" +
                        "[{'ResourceReference':'ZumoSqlServer_" + zumoServerId + ".Name'},'.database.windows.net']},'Database':{'ResourceReference':'ZumoSqlDatabase_" +
                        zumoDBId + ".Name'},'AdministratorLogin':'" + username + "','AdministratorLoginPassword':'" + password + "'},'Version':'2012-05-21.1.0'," +
                        "'Name':'ZumoMobileService','Type':'Microsoft.WindowsAzure.MobileServices.MobileService'},'ZumoSqlServer_" + zumoServerId +
                        "':{'ProvisioningParameters':{'AdministratorLogin':'" + username + "','AdministratorLoginPassword':'" + password + "','Location':'" + region +
                        "'},'ProvisioningConfigParameters':{'FirewallRules':[{'Name':'AllowAllWindowsAzureIps','StartIPAddress':'0.0.0.0','EndIPAddress':'0.0.0.0'}]}," +
                        "'Version':'1.0','Name':'ZumoSqlServer_" + zumoServerId + "','Type':'Microsoft.WindowsAzure.SQLAzure.Server'},'ZumoSqlDatabase_" + zumoDBId +
                        "':{'ProvisioningParameters':{'Name':'" + dbName + "','Edition':'WEB','MaxSizeInGB':'1','DBServer':{'ResourceReference':'ZumoSqlServer_" +
                        zumoServerId + ".Name'},'CollationName':'SQL_Latin1_General_CP1_CI_AS'},'Version':'1.0','Name':'ZumoSqlDatabase_" + zumoDBId +
                        "','Type':'Microsoft.WindowsAzure.SQLAzure.DataBase'}}}";
            } else {

                String zumoServerId = UUID.randomUUID().toString().replace("-", "");
                String zumoDBId = UUID.randomUUID().toString().replace("-", "");

                JSONParameter = "{'SchemaVersion':'2012-05.1.0','Location':'West US','ExternalResources':{'ZumoSqlServer_" + zumoServerId + "':{'Name':'ZumoSqlServer_" + zumoServerId
                        + "'," + "'Type':'Microsoft.WindowsAzure.SQLAzure.Server','URI':'https://management.core.windows.net:8443/" + subscriptionId.toString()
                        + "/services/sqlservers/servers/" + server + "'}," + "'ZumoSqlDatabase_" + zumoDBId + "':{'Name':'ZumoSqlDatabase_" + zumoDBId +
                        "','Type':'Microsoft.WindowsAzure.SQLAzure.DataBase'," + "'URI':'https://management.core.windows.net:8443/" + subscriptionId.toString()
                        + "/services/sqlservers/servers/" + server + "/databases/" + database + "'}}," + "'InternalResources':{'ZumoMobileService':{'ProvisioningParameters'" +
                        ":{'Name':'" + serviceName + "','Location':'" + region + "'},'ProvisioningConfigParameters':{'Server':{'StringConcat':[{'ResourceReference':'ZumoSqlServer_"
                        + zumoServerId + ".Name'}," + "'.database.windows.net']},'Database':{'ResourceReference':'ZumoSqlDatabase_" + zumoDBId + ".Name'},'AdministratorLogin':" +
                        "'" + username + "','AdministratorLoginPassword':'" + password + "'},'Version':'2012-05-21.1.0','Name':'ZumoMobileService','Type':" +
                        "'Microsoft.WindowsAzure.MobileServices.MobileService'}}}";
            }

            String xmlParameter = String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?><Application xmlns=\"http://schemas.microsoft.com/windowsazure\"><Name>%s</Name>" +
                            "<Label>%s</Label><Description>%s</Description><Configuration>%s</Configuration></Application>",
                    serviceName + "mobileservice", serviceName, serviceName, new BASE64Encoder().encode(JSONParameter.getBytes()));

            AzureRestAPIHelper.postRestApiCommand(path, xmlParameter, subscriptionId.toString(), String.format("/%s/operations/", subscriptionId.toString()), false);

            String xml = AzureRestAPIHelper.getRestApiCommand(String.format("/%s/applications/%s", subscriptionId.toString(), serviceName + "mobileservice"), subscriptionId.toString());
            NodeList statusNode = ((NodeList) AzureRestAPIHelper.getXMLValue(xml, "//Application/State", XPathConstants.NODESET));

            if (statusNode.getLength() > 0 && statusNode.item(0).getTextContent().equals("Healthy")) {
                return;
            } else {

                String errors = ((String) AzureRestAPIHelper.getXMLValue(xml, "//FailureCode[text()]", XPathConstants.STRING));

                throw new AzureCmdException("Error creating service", errors);
            }
        } catch (Exception e) {
            if (e instanceof AzureCmdException)
                throw (AzureCmdException) e;
            else
                throw new AzureCmdException("Error creating service", e);
        }
    }


    @Override
    public List<Table> getTableList(UUID subscriptionId, String serviceName) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables", subscriptionId.toString(), serviceName);

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            CustomJsonSlurper slurper = new CustomJsonSlurper();
            List<Map<String, String>> tempRes = (List<Map<String, String>>) slurper.parseText(json);

            List<Table> res = new ArrayList<Table>();
            for (Map<String, String> item : tempRes) {
                Table t = new Table();
                t.setName(item.get("name"));
                t.setSelfLink(item.get("selflink"));

                res.add(t);
            }

            return res;

        } catch (Exception e) {
            throw new AzureCmdException("Error getting table list", e);
        }
    }

    @Override
    public void createTable(UUID subscriptionId, String serviceName, String tableName, TablePermissions permissions) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables", subscriptionId.toString(), serviceName);

            String postData = "{\"insert\":\"" + PermissionItem.getPermitionString(permissions.getInsert())
                    + "\",\"read\":\"" + PermissionItem.getPermitionString(permissions.getRead())
                    + "\",\"update\":\"" + PermissionItem.getPermitionString(permissions.getUpdate())
                    + "\",\"delete\":\"" + PermissionItem.getPermitionString(permissions.getDelete())
                    + "\",\"name\":\"" + tableName + "\",\"idType\":\"string\"}";


            AzureRestAPIHelper.postRestApiCommand(path, postData, subscriptionId.toString(), null, true);
        } catch (Exception e) {
            throw new AzureCmdException("Error creating table", e);
        }
    }

    @Override
    public void updateTable(UUID subscriptionId, String serviceName, String tableName, TablePermissions permissions) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/permissions", subscriptionId.toString(), serviceName, tableName);

            String postData = "{\"insert\":\"" + PermissionItem.getPermitionString(permissions.getInsert())
                    + "\",\"read\":\"" + PermissionItem.getPermitionString(permissions.getRead())
                    + "\",\"update\":\"" + PermissionItem.getPermitionString(permissions.getUpdate())
                    + "\",\"delete\":\"" + PermissionItem.getPermitionString(permissions.getDelete())
                    + "\"}";

            AzureRestAPIHelper.putRestApiCommand(path, postData, subscriptionId.toString(), null, true);
        } catch (Exception e) {
            throw new AzureCmdException("Error updating table", e);
        }
    }

    @Override
    public Table showTableDetails(UUID subscriptionId, String serviceName, String tableName) throws AzureCmdException {

        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s",
                    subscriptionId.toString(), serviceName, tableName);

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());
            CustomJsonSlurper slurper = new CustomJsonSlurper();

            Table t = new Table();

            Map<String, Object> tableData = (Map<String, Object>) slurper.parseText(json);
            t.setName(tableData.get("name").toString());
            t.setSelfLink(tableData.get("selflink").toString());

            Map<String, String> per = (Map<String, String>) slurper.parseText(AzureRestAPIHelper.getRestApiCommand(path + "/permissions", subscriptionId.toString()));

            TablePermissions tablePermissions = new TablePermissions();
            tablePermissions.setInsert(PermissionItem.getPermitionType(per.get("insert")));
            tablePermissions.setUpdate(PermissionItem.getPermitionType(per.get("update")));
            tablePermissions.setRead(PermissionItem.getPermitionType(per.get("read")));
            tablePermissions.setDelete(PermissionItem.getPermitionType(per.get("delete")));
            t.setTablePermissions(tablePermissions);

            for (Map<String, Object> column : (List<Map<String, Object>>) slurper.parseText(AzureRestAPIHelper.getRestApiCommand(path + "/columns", subscriptionId.toString()))) {
                Column c = new Column();
                c.setName(column.get("name").toString());
                c.setType(column.get("type").toString());
                c.setSelfLink(column.get("selflink").toString());
                c.setIndexed((Boolean) column.get("indexed"));
                c.setZumoIndex((Boolean) column.get("zumoIndex"));

                t.getColumns().add(c);
            }

            for (Map<String, Object> script : (List<Map<String, Object>>) slurper.parseText(AzureRestAPIHelper.getRestApiCommand(path + "/scripts", subscriptionId.toString()))) {
                Script s = new Script();

                s.setOperation(script.get("operation").toString());
                s.setBytes((Integer) script.get("sizeBytes"));
                s.setSelfLink(script.get("selflink").toString());
                s.setName(String.format("%s.%s", tableData.get("name"), script.get("operation").toString()));

                t.getScripts().add(s);
            }

            return t;

        } catch (Exception e) {
            throw new AzureCmdException("Error getting table data", e);
        }

    }

    @Override
    public void downloadTableScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException {
        try {
            String tableName = scriptName.split("\\.")[0];
            String operation = scriptName.split("\\.")[1];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/scripts/%s/code", subscriptionId.toString(), serviceName, tableName, operation);
            String script = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            //On error, create script for template
        }
    }


    @Override
    public void uploadTableScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException {
        try {
            String tableName = scriptName.split("\\.")[0];
            String operation = scriptName.split("\\.")[1];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/scripts/%s/code", subscriptionId.toString(), serviceName, tableName, operation);

            AzureRestAPIHelper.uploadScript(path, filePath, subscriptionId.toString());

        } catch (Exception e) {
            throw new AzureCmdException("Error upload script", e);
        }

    }

    @Override
    public List<CustomAPI> getAPIList(UUID subscriptionId, String serviceName) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis", subscriptionId.toString(), serviceName);

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            CustomJsonSlurper slurper = new CustomJsonSlurper();
            List<Map<String, String>> tempRes = (List<Map<String, String>>) slurper.parseText(json);

            List<CustomAPI> res = new ArrayList<CustomAPI>();
            for (Map<String, String> item : tempRes) {
                CustomAPI c = new CustomAPI();
                c.setName(item.get("name"));
                CustomAPIPermissions permissions = new CustomAPIPermissions();
                permissions.setPutPermission(PermissionItem.getPermitionType(item.get("put")));
                permissions.setPostPermission(PermissionItem.getPermitionType(item.get("post")));
                permissions.setGetPermission(PermissionItem.getPermitionType(item.get("get")));
                permissions.setDeletePermission(PermissionItem.getPermitionType(item.get("delete")));
                permissions.setPatchPermission(PermissionItem.getPermitionType(item.get("patch")));
                c.setCustomAPIPermissions(permissions);
                res.add(c);
            }

            return res;
        } catch (Exception e) {
            throw new AzureCmdException("Error getting API list", e);
        }

    }

    @Override
    public void downloadAPIScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException {
        try {
            String apiName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s/script", subscriptionId.toString(), serviceName, apiName);
            String script = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            throw new AzureCmdException("Error getting API list", e);
        }
    }

    @Override
    public void uploadAPIScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException {
        try {
            String apiName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s/script", subscriptionId.toString(), serviceName, apiName);

            AzureRestAPIHelper.uploadScript(path, filePath, subscriptionId.toString());

        } catch (Exception e) {
            throw new AzureCmdException("Error upload script", e);
        }
    }

    @Override
    public void createCustomAPI(UUID subscriptionId, String serviceName, String tableName, CustomAPIPermissions permissions) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis", subscriptionId.toString(), serviceName);

            String postData = "{\"get\":\"" + permissions.getGetPermission()
                    + "\",\"put\":\"" + permissions.getPutPermission()
                    + "\",\"post\":\"" + permissions.getPostPermission()
                    + "\",\"patch\":\"" + permissions.getPatchPermission()
                    + "\",\"delete\":\"" + permissions.getDeletePermission()
                    + "\",\"name\":\"" + tableName + "\"}";


            AzureRestAPIHelper.postRestApiCommand(path, postData, subscriptionId.toString(), null, true);

        } catch (Exception e) {
            throw new AzureCmdException("Error creating API", e);
        }
    }

    @Override
    public void updateCustomAPI(UUID subscriptionId, String serviceName, String tableName, CustomAPIPermissions permissions) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s", subscriptionId.toString(), serviceName, tableName);

            String postData = "{\"get\":\"" + permissions.getGetPermission()
                    + "\",\"put\":\"" + permissions.getPutPermission()
                    + "\",\"post\":\"" + permissions.getPostPermission()
                    + "\",\"patch\":\"" + permissions.getPatchPermission()
                    + "\",\"delete\":\"" + permissions.getDeletePermission()
                    + "\"}";


            AzureRestAPIHelper.putRestApiCommand(path, postData, subscriptionId.toString(), null, true);

        } catch (Exception e) {
            throw new AzureCmdException("Error updating API", e);
        }

    }

    @Override
    public List<Job> listJobs(UUID subscriptionId, String serviceName) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs", subscriptionId.toString(), serviceName);

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            CustomJsonSlurper slurper = new CustomJsonSlurper();
            List<Map<String, Object>> tempRes = (List<Map<String, Object>>) slurper.parseText(json);

            List<Job> res = new ArrayList<Job>();
            for (Map<String, Object> item : tempRes) {
                Job j = new Job();
                j.setAppName(item.get("appName").toString());
                j.setName(item.get("name").toString());
                j.setEnabled(item.get("status").equals("enabled"));
                j.setId(UUID.fromString(item.get("id").toString()));

                if (item.get("intervalPeriod") != null) {
                    j.setIntervalPeriod((Integer) item.get("intervalPeriod"));
                    j.setIntervalUnit(item.get("intervalUnit").toString());
                }

                res.add(j);
            }

            return res;
        } catch (Exception e) {
            throw new AzureCmdException("Error getting job list", e);
        }
    }

    @Override
    public void createJob(UUID subscriptionId, String serviceName, String jobName, int interval, String intervalUnit, String startDate) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs", subscriptionId.toString(), serviceName);

            String postData = "{\"name\":\"" + jobName
                    + "\",\"intervalUnit\":\"" + intervalUnit
                    + "\",\"intervalPeriod\":" + String.valueOf(interval)
                    + ",\"startTime\":\"" + startDate + "\""
                    + "}";


            AzureRestAPIHelper.postRestApiCommand(path, postData, subscriptionId.toString(), null, true);

        } catch (Exception e) {
            throw new AzureCmdException("Error creating jobs", e);
        }

    }

    @Override
    public void updateJob(UUID subscriptionId, String serviceName, String jobName, int interval, String intervalUnit, String startDate, boolean enabled) throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s", subscriptionId.toString(), serviceName, jobName);

            String postData = "{\"name\":\"" + jobName
                    + "\",\"intervalUnit\":\"" + intervalUnit
                    + "\",\"intervalPeriod\":" + String.valueOf(interval)
                    + ",\"status\":\"" + (enabled ? "enabled" : "disabled")
                    + "\",\"startTime\":\"" + startDate
                    + "\"}";

            if (intervalUnit.equals("none")) {
                postData = "{\"status\":\"disabled\"}";
            }

            AzureRestAPIHelper.putRestApiCommand(path, postData, subscriptionId.toString(), null, true);

        } catch (Exception e) {
            throw new AzureCmdException("Error updating job", e);
        }
    }

    @Override
    public void downloadJobScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException {
        try {
            String jobName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s/script", subscriptionId.toString(), serviceName, jobName);
            String script = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
            //On error, create script for template
        }
    }


    @Override
    public void uploadJobScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException {
        try {
            String jobName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s/script", subscriptionId.toString(), serviceName, jobName);

            AzureRestAPIHelper.uploadScript(path, filePath, subscriptionId.toString());

        } catch (Exception e) {
            throw new AzureCmdException("Error upload script", e);
        }
    }

    @Override
    public List<LogEntry> listLog(UUID subscriptionId, String serviceName) throws AzureCmdException, ParseException {

        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/logs?$top=10", subscriptionId.toString(), serviceName);

            String json = AzureRestAPIHelper.getRestApiCommand(path, subscriptionId.toString());

            CustomJsonSlurper slurper = new CustomJsonSlurper();

            Map<String, Object> results = (Map<String, Object>) slurper.parseText(json);
            List<Map<String, String>> tempRes = (List<Map<String, String>>) results.get("results");

            List<LogEntry> res = new ArrayList<LogEntry>();
            for (Map<String, String> item : tempRes) {
                LogEntry logEntry = new LogEntry();

                logEntry.setMessage(item.get("message"));
                logEntry.setSource(item.get("source"));
                logEntry.setType(item.get("type"));

                SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                logEntry.setTimeCreated(ISO8601DATEFORMAT.parse(item.get("timeCreated")));

                res.add(logEntry);
            }

            return res;

        } catch (Exception e) {
            throw new AzureCmdException("Error getting log", e);
        }
    }
}
