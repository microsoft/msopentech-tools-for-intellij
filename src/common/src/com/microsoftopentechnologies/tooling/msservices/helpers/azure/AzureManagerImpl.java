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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.network.NetworkManagementClient;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoftopentechnologies.tooling.msservices.components.AppSettingsNames;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.components.PluginSettings;
import com.microsoftopentechnologies.tooling.msservices.helpers.*;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.AADManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.UserInfo;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureAADHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureCertificateHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.MobileServiceRestManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.RestServiceManager.ContentType;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.RestServiceManagerBaseImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.model.*;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.SDKRequestCallback;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.ms.*;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.vm.*;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.reflect.Type;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AzureManagerImpl implements AzureManager {
    private interface AzureSDKClientProvider<V extends Closeable> {
        @NotNull
        V getSSLClient(@NotNull Subscription subscription)
                throws Throwable;

        @NotNull
        V getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                throws Throwable;
    }

    private static class EventWaitHandleImpl implements EventWaitHandle {
        Semaphore eventSignal = new Semaphore(0, true);

        @Override
        public void waitEvent(@NotNull Runnable callback)
                throws AzureCmdException {
            try {
                eventSignal.acquire();
                callback.run();
            } catch (InterruptedException e) {
                throw new AzureCmdException("Unable to aquire permit", e);
            }
        }

        private synchronized void signalEvent() {
            if (eventSignal.availablePermits() == 0) {
                eventSignal.release();
            }
        }
    }

    private static AzureManager instance;
    private static Gson gson;

    private AADManager aadManager;

    private ReentrantReadWriteLock authDataLock = new ReentrantReadWriteLock(false);
    private Map<String, Subscription> subscriptions;
    private UserInfo userInfo;

    private ReentrantReadWriteLock subscriptionMapLock = new ReentrantReadWriteLock(false);
    private Map<String, ReentrantReadWriteLock> lockBySubscriptionId;
    private Map<String, UserInfo> userInfoBySubscriptionId;
    private Map<String, SSLSocketFactory> sslSocketFactoryBySubscriptionId;

    private ReentrantReadWriteLock userMapLock = new ReentrantReadWriteLock(false);
    private Map<UserInfo, ReentrantReadWriteLock> lockByUser;
    private Map<UserInfo, String> accessTokenByUser;

    private ReentrantReadWriteLock subscriptionsChangedLock = new ReentrantReadWriteLock(true);
    private Set<EventWaitHandleImpl> subscriptionsChangedHandles;

    private AzureManagerImpl() {
        authDataLock.writeLock().lock();

        try {
            aadManager = AADManagerImpl.getManager();

            loadSubscriptions();
            loadUserInfo();
            loadSSLSocketFactory();

            removeInvalidUserInfo();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();

            accessTokenByUser = new HashMap<UserInfo, String>();
            lockByUser = new HashMap<UserInfo, ReentrantReadWriteLock>();
            subscriptionsChangedHandles = new HashSet<EventWaitHandleImpl>();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    public static synchronized AzureManager getManager() {
        if (instance == null) {
            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            instance = new AzureManagerImpl();
        }

        return instance;
    }

    @Override
    public void authenticate() throws AzureCmdException {
        final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        final String managementUri = settings.getAzureServiceManagementUri();

        final UserInfo userInfo = aadManager.authenticate(managementUri, "Sign in to your Azure account");
        setUserInfo(userInfo);

        List<Subscription> subscriptions = requestWithToken(userInfo, new RequestCallback<List<Subscription>>() {
            @Override
            public List<Subscription> execute()
                    throws Throwable {
                String accessToken = getAccessToken(userInfo);
                String subscriptionsXML = AzureAADHelper.executeRequest(managementUri,
                        "subscriptions",
                        ContentType.Json,
                        "GET",
                        null,
                        accessToken,
                        new RestServiceManagerBaseImpl() {
                            @NotNull
                            @Override
                            public String executePollRequest(@NotNull String managementUrl,
                                                             @NotNull String path,
                                                             @NotNull ContentType contentType,
                                                             @NotNull String method,
                                                             @Nullable String postData,
                                                             @NotNull String pollPath,
                                                             @NotNull HttpsURLConnectionProvider sslConnectionProvider)
                                    throws AzureCmdException {
                                throw new NotImplementedException();
                            }
                        });

                return parseSubscriptionsXML(subscriptionsXML);
            }
        });

        for (Subscription subscription : subscriptions) {
            UserInfo subscriptionUser = new UserInfo(subscription.getTenantId(), userInfo.getUniqueName());
            aadManager.authenticate(subscriptionUser, managementUri, "Sign in to your Azure account");

            updateSubscription(subscription, subscriptionUser);
        }
    }

    @Override
    public boolean authenticated() {
        return getUserInfo() != null;
    }

    @Override
    public boolean authenticated(@NotNull String subscriptionId) {
        return !hasSSLSocketFactory(subscriptionId) && hasUserInfo(subscriptionId);
    }

    @Override
    public void clearAuthentication() {
        setUserInfo(null);
    }

    @Override
    public void importPublishSettingsFile(@NotNull String publishSettingsFilePath)
            throws AzureCmdException {
        List<Subscription> subscriptions = importSubscription(publishSettingsFilePath);

        for (Subscription subscription : subscriptions) {
            try {
                SSLSocketFactory sslSocketFactory = initSSLSocketFactory(subscription.getManagementCertificate());
                updateSubscription(subscription, sslSocketFactory);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean usingCertificate() {
        authDataLock.readLock().lock();

        try {
            return sslSocketFactoryBySubscriptionId.size() > 0;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public boolean usingCertificate(@NotNull String subscriptionId) {
        return hasSSLSocketFactory(subscriptionId);
    }

    @Override
    public void clearImportedPublishSettingsFiles() {
        authDataLock.writeLock().lock();

        try {
            sslSocketFactoryBySubscriptionId.clear();
            removeUnusedSubscriptions();
            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getFullSubscriptionList()
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<Subscription>();

            for (Subscription subscription : subscriptions.values()) {
                result.add(subscription);
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getSubscriptionList()
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<Subscription>();

            for (Subscription subscription : subscriptions.values()) {
                if (subscription.isSelected()) {
                    result.add(subscription);
                }
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws AzureCmdException {
        authDataLock.writeLock().lock();

        try {
            for (String subscriptionId : subscriptions.keySet()) {
                Subscription subscription = subscriptions.get(subscriptionId);
                subscription.setSelected(selectedList.contains(subscriptionId));
            }

            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }

        notifySubscriptionsChanged();
    }

    @NotNull
    @Override
    public EventWaitHandle registerSubscriptionsChanged()
            throws AzureCmdException {
        subscriptionsChangedLock.writeLock().lock();

        try {
            EventWaitHandleImpl handle = new EventWaitHandleImpl();

            subscriptionsChangedHandles.add(handle);

            return handle;
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }
    }

    @Override
    public void unregisterSubscriptionsChanged(@NotNull EventWaitHandle handle)
            throws AzureCmdException {
        if (!(handle instanceof EventWaitHandleImpl)) {
            throw new AzureCmdException("Invalid handle instance");
        }

        subscriptionsChangedLock.writeLock().lock();

        try {
            subscriptionsChangedHandles.remove(handle);
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }

        ((EventWaitHandleImpl) handle).signalEvent();
    }

    @NotNull
    @Override
    public List<SqlDb> getSqlDb(@NotNull String subscriptionId, @NotNull SqlServer server)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/sqlservers/servers/%s/databases?contentview=generic",
                    subscriptionId, server.getName());

            String xml = executeGetRequest(subscriptionId, path);

            List<SqlDb> res = new ArrayList<SqlDb>();
            NodeList nl = (NodeList) XmlHelper.getXMLValue(xml, "//ServiceResource", XPathConstants.NODESET);

            for (int i = 0; i != nl.getLength(); i++) {

                SqlDb sqls = new SqlDb();
                sqls.setName(XmlHelper.getChildNodeValue(nl.item(i), "Name"));
                sqls.setEdition(XmlHelper.getChildNodeValue(nl.item(i), "Edition"));
                sqls.setServer(server);
                res.add(sqls);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting database list", t);
        }
    }

    @NotNull
    @Override
    public List<SqlServer> getSqlServers(@NotNull String subscriptionId)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/sqlservers/servers", subscriptionId);
            String xml = executeGetRequest(subscriptionId, path);

            List<SqlServer> res = new ArrayList<SqlServer>();

            NodeList nl = (NodeList) XmlHelper.getXMLValue(xml, "//Server", XPathConstants.NODESET);

            for (int i = 0; i != nl.getLength(); i++) {
                SqlServer sqls = new SqlServer();

                sqls.setAdmin(XmlHelper.getChildNodeValue(nl.item(i), "AdministratorLogin"));
                sqls.setName(XmlHelper.getChildNodeValue(nl.item(i), "Name"));
                sqls.setRegion(XmlHelper.getChildNodeValue(nl.item(i), "Location"));
                res.add(sqls);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting server list", t);
        }
    }

    @NotNull
    @Override
    public List<MobileService> getMobileServiceList(@NotNull String subscriptionId)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices", subscriptionId);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<MobileServiceData>>() {
            }.getType();
            List<MobileServiceData> tempRes = new Gson().fromJson(json, type);

            List<MobileService> res = new ArrayList<MobileService>();

            for (MobileServiceData item : tempRes) {
                MobileService ser = new MobileService();

                ser.setName(item.getName());
                ser.setType(item.getType());
                ser.setState(item.getState());
                ser.setSelfLink(item.getSelflink());
                ser.setAppUrl(item.getApplicationUrl());
                ser.setAppKey(item.getApplicationKey());
                ser.setMasterKey(item.getMasterKey());
                ser.setWebspace(item.getWebspace());
                ser.setRegion(item.getRegion());
                ser.setMgmtPortalLink(item.getManagementPortalLink());
                ser.setSubcriptionId(subscriptionId);

                if (item.getPlatform() != null && item.getPlatform().equals("dotNet")) {
                    ser.setRuntime(MobileService.NET_RUNTIME);
                } else {
                    ser.setRuntime(MobileService.NODE_RUNTIME);
                }

                for (MobileServiceData.Table table : item.getTables()) {
                    Table t = new Table();
                    t.setName(table.getName());
                    t.setSelfLink(table.getSelflink());
                    ser.getTables().add(t);
                }

                res.add(ser);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting service list", t);
        }
    }

    @Override
    public void createMobileService(@NotNull String subscriptionId, @NotNull String region,
                                    @NotNull String username, @NotNull String password,
                                    @NotNull String mobileServiceName,
                                    @Nullable String server, @Nullable String database)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/applications", subscriptionId);

            String JSONParameter;

            if (database == null || server == null) {
                String zumoServerId = UUID.randomUUID().toString().replace("-", "");
                String zumoDBId = UUID.randomUUID().toString().replace("-", "");
                String dbName = mobileServiceName + "_db";

                JSONParameter = "{'SchemaVersion':'2012-05.1.0','Location':'" + region + "','ExternalResources':{},'InternalResources':{'ZumoMobileService':" +
                        "{'ProvisioningParameters':{'Name':'" + mobileServiceName + "','Location':'" + region + "'},'ProvisioningConfigParameters':{'Server':{'StringConcat':" +
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
                        + "'," + "'Type':'Microsoft.WindowsAzure.SQLAzure.Server','URI':'https://management.core.windows.net:8443/" + subscriptionId
                        + "/services/sqlservers/servers/" + server + "'}," + "'ZumoSqlDatabase_" + zumoDBId + "':{'Name':'ZumoSqlDatabase_" + zumoDBId +
                        "','Type':'Microsoft.WindowsAzure.SQLAzure.DataBase'," + "'URI':'https://management.core.windows.net:8443/" + subscriptionId
                        + "/services/sqlservers/servers/" + server + "/databases/" + database + "'}}," + "'InternalResources':{'ZumoMobileService':{'ProvisioningParameters'" +
                        ":{'Name':'" + mobileServiceName + "','Location':'" + region + "'},'ProvisioningConfigParameters':{'Server':{'StringConcat':[{'ResourceReference':'ZumoSqlServer_"
                        + zumoServerId + ".Name'}," + "'.database.windows.net']},'Database':{'ResourceReference':'ZumoSqlDatabase_" + zumoDBId + ".Name'},'AdministratorLogin':" +
                        "'" + username + "','AdministratorLoginPassword':'" + password + "'},'Version':'2012-05-21.1.0','Name':'ZumoMobileService','Type':" +
                        "'Microsoft.WindowsAzure.MobileServices.MobileService'}}}";
            }

            String xmlParameter = String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?><Application xmlns=\"http://schemas.microsoft.com/windowsazure\"><Name>%s</Name>" +
                            "<Label>%s</Label><Description>%s</Description><Configuration>%s</Configuration></Application>",
                    mobileServiceName + "mobileservice", mobileServiceName, mobileServiceName, new BASE64Encoder().encode(JSONParameter.getBytes()));

            executePollRequest(subscriptionId, path, ContentType.Xml, "POST", xmlParameter, String.format("/%s/operations/", subscriptionId));

            String xml = executeGetRequest(subscriptionId, String.format("/%s/applications/%s", subscriptionId, mobileServiceName + "mobileservice"));
            NodeList statusNode = ((NodeList) XmlHelper.getXMLValue(xml, "//Application/State", XPathConstants.NODESET));

            if (!(statusNode.getLength() > 0 && statusNode.item(0).getTextContent().equals("Healthy"))) {
                deleteMobileService(subscriptionId, mobileServiceName);

                String errors = ((String) XmlHelper.getXMLValue(xml, "//FailureCode[text()]", XPathConstants.STRING));
                String errorMessage = ((String) XmlHelper.getXMLValue(errors, "//Message[text()]", XPathConstants.STRING));
                throw new AzureCmdException("Error creating service", errorMessage);
            }
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating service", t);
        }
    }

    @Override
    public void deleteMobileService(@NotNull String subscriptionId, @NotNull String mobileServiceName) {
        String mspath = String.format("/%s/services/mobileservices/mobileservices/%s?deletedata=true",
                subscriptionId, mobileServiceName);

        try {
            executePollRequest(subscriptionId, mspath, ContentType.Json, "DELETE", null, String.format("/%s/operations/", subscriptionId));
        } catch (Throwable ignored) {
        }

        String appPath = String.format("/%s/applications/%smobileservice", subscriptionId, mobileServiceName);

        try {
            executePollRequest(subscriptionId, appPath, ContentType.Xml, "DELETE", null, String.format("/%s/operations/", subscriptionId));
        } catch (Throwable ignored) {
        }
    }

    @NotNull
    @Override
    public List<Table> getTableList(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables", subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<TableData>>() {
            }.getType();
            List<TableData> tempRes = new Gson().fromJson(json, type);

            List<Table> res = new ArrayList<Table>();

            for (TableData item : tempRes) {
                Table t = new Table();
                t.setName(item.getName());
                t.setSelfLink(item.getSelflink());

                res.add(t);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting table list", t);
        }
    }

    @Override
    public void createTable(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName,
                            @NotNull TablePermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables", subscriptionId, mobileServiceName);

            String postData = "{\"insert\":\"" + PermissionItem.getPermitionString(permissions.getInsert())
                    + "\",\"read\":\"" + PermissionItem.getPermitionString(permissions.getRead())
                    + "\",\"update\":\"" + PermissionItem.getPermitionString(permissions.getUpdate())
                    + "\",\"delete\":\"" + PermissionItem.getPermitionString(permissions.getDelete())
                    + "\",\"name\":\"" + tableName + "\",\"idType\":\"string\"}";

            executeRequest(subscriptionId, path, ContentType.Json, "POST", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating table", t);
        }
    }

    @Override
    public void updateTable(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName,
                            @NotNull TablePermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/permissions",
                    subscriptionId, mobileServiceName, tableName);

            String postData = "{\"insert\":\"" + PermissionItem.getPermitionString(permissions.getInsert())
                    + "\",\"read\":\"" + PermissionItem.getPermitionString(permissions.getRead())
                    + "\",\"update\":\"" + PermissionItem.getPermitionString(permissions.getUpdate())
                    + "\",\"delete\":\"" + PermissionItem.getPermitionString(permissions.getDelete())
                    + "\"}";

            executeRequest(subscriptionId, path, ContentType.Json, "PUT", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error updating table", t);
        }
    }

    @NotNull
    @Override
    public Table showTableDetails(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s",
                    subscriptionId, mobileServiceName, tableName);
            String json = executeGetRequest(subscriptionId, path);
            Gson gson = new Gson();
            TableData tempRes = gson.fromJson(json, TableData.class);

            Table t = new Table();
            t.setName(tempRes.getName());
            t.setSelfLink(tempRes.getSelflink());

            TablePermissionsData restTablePermissions = gson.fromJson(executeGetRequest(subscriptionId, path + "/permissions"),
                    TablePermissionsData.class);

            TablePermissions tablePermissions = new TablePermissions();
            tablePermissions.setInsert(PermissionItem.getPermitionType(restTablePermissions.getInsert()));
            tablePermissions.setUpdate(PermissionItem.getPermitionType(restTablePermissions.getUpdate()));
            tablePermissions.setRead(PermissionItem.getPermitionType(restTablePermissions.getRead()));
            tablePermissions.setDelete(PermissionItem.getPermitionType(restTablePermissions.getDelete()));
            t.setTablePermissions(tablePermissions);

            Type colType = new TypeToken<ArrayList<TableColumnData>>() {
            }.getType();
            List<TableColumnData> colList = gson.fromJson(executeGetRequest(subscriptionId, path + "/columns"),
                    colType);
            if (colList != null) {
                for (TableColumnData column : colList) {
                    Column c = new Column();
                    c.setName(column.getName());
                    c.setType(column.getType());
                    c.setSelfLink(column.getSelflink());
                    c.setIndexed(column.isIndexed());
                    c.setZumoIndex(column.isZumoIndex());

                    t.getColumns().add(c);
                }
            }

            Type scrType = new TypeToken<ArrayList<TableScriptData>>() {
            }.getType();
            List<TableScriptData> scrList = gson.fromJson(executeGetRequest(subscriptionId, path + "/scripts"),
                    scrType);

            if (scrList != null) {
                for (TableScriptData script : scrList) {
                    Script s = new Script();

                    s.setOperation(script.getOperation());
                    s.setBytes(script.getSizeBytes());
                    s.setSelfLink(script.getSelflink());
                    s.setName(String.format("%s.%s", tempRes.getName(), script.getOperation()));

                    t.getScripts().add(s);
                }
            }

            return t;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting table data", t);
        }
    }

    @Override
    public void downloadTableScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                    @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException {
        try {
            String tableName = scriptName.split("\\.")[0];
            String operation = scriptName.split("\\.")[1];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/scripts/%s/code",
                    subscriptionId, mobileServiceName, tableName, operation);
            String script = executeGetRequest(subscriptionId, path);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error download script", t);
        }
    }

    @Override
    public void uploadTableScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException {
        try {
            String tableName = scriptName.split("\\.")[0];
            String operation = scriptName.split("\\.")[1];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/tables/%s/scripts/%s/code",
                    subscriptionId, mobileServiceName, tableName, operation);
            String file = readFile(filePath);

            executeRequest(subscriptionId, path, ContentType.Text, "PUT", file);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error upload script", t);
        }
    }

    @NotNull
    @Override
    public List<CustomAPI> getAPIList(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis",
                    subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<CustomAPIData>>() {
            }.getType();
            List<CustomAPIData> tempRes = new Gson().fromJson(json, type);

            List<CustomAPI> res = new ArrayList<CustomAPI>();

            for (CustomAPIData item : tempRes) {
                CustomAPI c = new CustomAPI();
                c.setName(item.getName());
                CustomAPIPermissions permissions = new CustomAPIPermissions();
                permissions.setPutPermission(PermissionItem.getPermitionType(item.getPut()));
                permissions.setPostPermission(PermissionItem.getPermitionType(item.getPost()));
                permissions.setGetPermission(PermissionItem.getPermitionType(item.getGet()));
                permissions.setDeletePermission(PermissionItem.getPermitionType(item.getDelete()));
                permissions.setPatchPermission(PermissionItem.getPermitionType(item.getPatch()));
                c.setCustomAPIPermissions(permissions);
                res.add(c);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting API list", t);
        }
    }

    @Override
    public void downloadAPIScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException {
        try {
            String apiName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s/script",
                    subscriptionId, mobileServiceName, apiName);
            String script = executeGetRequest(subscriptionId, path);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting API list", t);
        }
    }

    @Override
    public void uploadAPIScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException {
        try {
            String apiName = scriptName.split("\\.")[0];
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s/script",
                    subscriptionId, mobileServiceName, apiName);
            String file = readFile(filePath);

            executeRequest(subscriptionId, path, ContentType.Text, "PUT", file);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error upload script", t);
        }
    }

    @Override
    public void createCustomAPI(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String tableName, @NotNull CustomAPIPermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis",
                    subscriptionId, mobileServiceName);
            String postData = "{\"get\":\"" + permissions.getGetPermission()
                    + "\",\"put\":\"" + permissions.getPutPermission()
                    + "\",\"post\":\"" + permissions.getPostPermission()
                    + "\",\"patch\":\"" + permissions.getPatchPermission()
                    + "\",\"delete\":\"" + permissions.getDeletePermission()
                    + "\",\"name\":\"" + tableName + "\"}";
            executeRequest(subscriptionId, path, ContentType.Json, "POST", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating API", t);
        }
    }

    @Override
    public void updateCustomAPI(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String tableName, @NotNull CustomAPIPermissions permissions)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/apis/%s",
                    subscriptionId, mobileServiceName, tableName);
            String postData = "{\"get\":\"" + permissions.getGetPermission()
                    + "\",\"put\":\"" + permissions.getPutPermission()
                    + "\",\"post\":\"" + permissions.getPostPermission()
                    + "\",\"patch\":\"" + permissions.getPatchPermission()
                    + "\",\"delete\":\"" + permissions.getDeletePermission()
                    + "\"}";
            executeRequest(subscriptionId, path, ContentType.Json, "PUT", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error updating API", t);
        }
    }

    @NotNull
    @Override
    public List<Job> listJobs(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs",
                    subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            Type type = new TypeToken<ArrayList<JobData>>() {
            }.getType();
            List<JobData> tempRes = new Gson().fromJson(json, type);

            List<Job> res = new ArrayList<Job>();

            for (JobData item : tempRes) {
                Job j = new Job();
                j.setAppName(item.getAppName());
                j.setName(item.getName());
                j.setEnabled(item.getStatus().equals("enabled"));
                j.setId(UUID.fromString(item.getId()));

                if (item.getIntervalPeriod() > 0) {
                    j.setIntervalPeriod(item.getIntervalPeriod());
                    j.setIntervalUnit(item.getIntervalUnit());
                }

                res.add(j);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting job list", t);
        }
    }

    @Override
    public void createJob(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String jobName,
                          int interval, @NotNull String intervalUnit, @NotNull String startDate)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs",
                    subscriptionId, mobileServiceName);
            String postData = "{\"name\":\"" + jobName + "\""
                    + (
                    intervalUnit.equals("none") ? "" : (",\"intervalUnit\":\"" + intervalUnit
                            + "\",\"intervalPeriod\":" + String.valueOf(interval)
                            + ",\"startTime\":\"" + startDate + "\""))
                    + "}";
            executeRequest(subscriptionId, path, ContentType.Json, "POST", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error creating jobs", t);
        }
    }

    @Override
    public void updateJob(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String jobName,
                          int interval, @NotNull String intervalUnit, @NotNull String startDate, boolean enabled)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s",
                    subscriptionId, mobileServiceName, jobName);
            String postData = "{"
                    + "\"status\":\"" + (enabled ? "enabled" : "disabled") + "\""
                    + (
                    intervalUnit.equals("none") ? "" : (",\"intervalUnit\":\"" + intervalUnit
                            + "\",\"intervalPeriod\":" + String.valueOf(interval)
                            + ",\"startTime\":\"" + startDate + "\""))
                    + "}";

            if (intervalUnit.equals("none")) {
                postData = "{\"status\":\"disabled\"}";
            }

            executeRequest(subscriptionId, path, ContentType.Json, "PUT", postData);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error updating job", t);
        }
    }

    @Override
    public void downloadJobScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException {
        try {
            String jobName = scriptName.split("\\.")[0];

            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s/script",
                    subscriptionId, mobileServiceName, jobName);
            String script = executeGetRequest(subscriptionId, path);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(downloadPath), "utf-8"));
            writer.write(script);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error download script", t);
        }
    }

    @Override
    public void uploadJobScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException {
        try {
            String jobName = scriptName.split("\\.")[0];
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/scheduler/jobs/%s/script",
                    subscriptionId, mobileServiceName, jobName);
            String file = readFile(filePath);

            executeRequest(subscriptionId, path, ContentType.Text, "PUT", file);
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error upload script", t);
        }
    }

    @NotNull
    @Override
    public List<LogEntry> listLog(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                                  @NotNull String runtime)
            throws AzureCmdException {
        try {
            String path = String.format("/%s/services/mobileservices/mobileservices/%s/logs?$top=10",
                    subscriptionId, mobileServiceName);
            String json = executeGetRequest(subscriptionId, path);

            LogData tempRes = new Gson().fromJson(json, LogData.class);

            List<LogEntry> res = new ArrayList<LogEntry>();

            for (LogData.LogEntry item : tempRes.getResults()) {
                LogEntry logEntry = new LogEntry();

                logEntry.setMessage(item.getMessage());
                logEntry.setSource(item.getSource());
                logEntry.setType(item.getType());

                SimpleDateFormat ISO8601DATEFORMAT;

                if (MobileService.NODE_RUNTIME.equals(runtime)) {
                    ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                } else {
                    ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
                }
                logEntry.setTimeCreated(ISO8601DATEFORMAT.parse(item.getTimeCreated()));

                res.add(logEntry);
            }

            return res;
        } catch (Throwable t) {
            if (t instanceof AzureCmdException) {
                throw (AzureCmdException) t;
            }

            throw new AzureCmdException("Error getting log", t);
        }
    }

    @NotNull
    @Override
    public List<CloudService> getCloudServices(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCloudServices(subscriptionId));
    }

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getVirtualMachines(subscriptionId));
    }

    @NotNull
    @Override
    public VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        return requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.refreshVirtualMachineInformation(vm));
    }

    @Override
    public void startVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.startVirtualMachine(vm));
    }

    @Override
    public void shutdownVirtualMachine(@NotNull VirtualMachine vm, boolean deallocate)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.shutdownVirtualMachine(vm, deallocate));
    }

    @Override
    public void restartVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.restartVirtualMachine(vm));
    }

    @Override
    public void deleteVirtualMachine(@NotNull VirtualMachine vm, boolean deleteFromStorage)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.deleteVirtualMachine(vm, deleteFromStorage));
    }

    @NotNull
    @Override
    public byte[] downloadRDP(@NotNull VirtualMachine vm) throws AzureCmdException {
        return requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.downloadRDP(vm));
    }

    @NotNull
    @Override
    public List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.getStorageAccounts(subscriptionId));
    }

    @NotNull
    @Override
    public List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getVirtualMachineImages());
    }

    @NotNull
    @Override
    public List<VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getVirtualMachineSizes());
    }

    @NotNull
    @Override
    public List<Location> getLocations(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getLocations());
    }

    @NotNull
    @Override
    public List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getAffinityGroups());
    }

    @NotNull
    @Override
    public List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestNetworkSDK(subscriptionId, AzureSDKHelper.getVirtualNetworks(subscriptionId));
    }

    @Override
    public void createStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.createStorageAccount(storageAccount));
    }

    @Override
    public void createCloudService(@NotNull CloudService cloudService)
            throws AzureCmdException {
        requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.createCloudService(cloudService));
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull StorageAccount storageAccount, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        requestComputeSDK(virtualMachine.getSubscriptionId(), AzureSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, storageAccount, virtualNetwork, username, password, certificate));
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull String mediaLocation, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        requestComputeSDK(virtualMachine.getSubscriptionId(), AzureSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, mediaLocation, virtualNetwork, username, password, certificate));
    }

    @NotNull
    @Override
    public StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(),
                AzureSDKHelper.refreshStorageAccountInformation(storageAccount));
    }

    @Override
    public String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
                                           @NotNull byte[] data, @NotNull String password)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createServiceCertificate(serviceName, data, password));
    }

    @Override
    public void deleteStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.deleteStorageAccount(storageAccount));
    }

    private void loadSubscriptions() {
        String json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
                }.getType();
                subscriptions = gson.fromJson(json, subscriptionsType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS);
            }
        } else {
            subscriptions = new HashMap<String, Subscription>();
        }

        lockBySubscriptionId = new HashMap<String, ReentrantReadWriteLock>();

        for (String subscriptionId : subscriptions.keySet()) {
            lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
        }
    }

    private void loadUserInfo() {
        String json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.AZURE_USER_INFO);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                userInfo = gson.fromJson(json, UserInfo.class);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_INFO);
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS);
            }
        } else {
            DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS);
        }

        json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type userInfoBySubscriptionIdType = new TypeToken<HashMap<String, UserInfo>>() {
                }.getType();
                userInfoBySubscriptionId = gson.fromJson(json, userInfoBySubscriptionIdType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS);
            }
        } else {
            userInfoBySubscriptionId = new HashMap<String, UserInfo>();
        }
    }

    private void loadSSLSocketFactory() {
        sslSocketFactoryBySubscriptionId = new HashMap<String, SSLSocketFactory>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            Subscription subscription = subscriptionEntry.getValue();
            String managementCertificate = subscription.getManagementCertificate();

            if (!StringHelper.isNullOrWhiteSpace(managementCertificate)) {
                try {
                    SSLSocketFactory sslSocketFactory = initSSLSocketFactory(managementCertificate);
                    sslSocketFactoryBySubscriptionId.put(subscriptionId, sslSocketFactory);
                } catch (Exception e) {
                    subscription.setManagementCertificate(null);
                }
            }
        }
    }

    private void removeInvalidUserInfo() {
        List<String> invalidSubscriptionIds = new ArrayList<String>();

        for (String subscriptionId : userInfoBySubscriptionId.keySet()) {
            if (!subscriptions.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            userInfoBySubscriptionId.remove(invalidSubscriptionId);
        }
    }

    private void removeUnusedSubscriptions() {
        List<String> invalidSubscriptionIds = new ArrayList<String>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            Subscription subscription = subscriptionEntry.getValue();

            if (!userInfoBySubscriptionId.containsKey(subscriptionId) &&
                    !sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
            } else if (!userInfoBySubscriptionId.containsKey(subscriptionId)) {
                subscription.setTenantId(null);
            } else if (!sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                subscription.setManagementCertificate(null);
                subscription.setServiceManagementUrl(null);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            lockBySubscriptionId.remove(invalidSubscriptionId);
            subscriptions.remove(invalidSubscriptionId);
        }
    }

    private void storeSubscriptions() {
        Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
        }.getType();
        String json = gson.toJson(subscriptions, subscriptionsType);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS, json);
    }

    private void storeUserInfo() {
        String json = gson.toJson(userInfo, UserInfo.class);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_USER_INFO, json);

        Type userInfoBySubscriptionIdType = new TypeToken<HashMap<String, UserInfo>>() {
        }.getType();
        json = gson.toJson(userInfoBySubscriptionId, userInfoBySubscriptionIdType);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS, json);
    }

    @NotNull
    private List<Subscription> parseSubscriptionsXML(@NotNull String subscriptionsXML)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        NodeList subscriptionList = (NodeList) XmlHelper.getXMLValue(subscriptionsXML, "//Subscription", XPathConstants.NODESET);

        ArrayList<Subscription> subscriptions = new ArrayList<Subscription>();

        for (int i = 0; i < subscriptionList.getLength(); i++) {
            Subscription subscription = new Subscription();
            subscription.setName(XmlHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionName"));
            subscription.setId(XmlHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionID"));
            subscription.setTenantId(XmlHelper.getChildNodeValue(subscriptionList.item(i), "AADTenantID"));
            subscription.setSelected(true);

            subscriptions.add(subscription);
        }

        return subscriptions;
    }

    private SSLSocketFactory initSSLSocketFactory(@NotNull String managementCertificate)
            throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        byte[] decodeBuffer = new BASE64Decoder().decodeBuffer(managementCertificate);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");

        InputStream is = new ByteArrayInputStream(decodeBuffer);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, OpenSSLHelper.PASSWORD.toCharArray());
        keyManagerFactory.init(ks, OpenSSLHelper.PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        return sslContext.getSocketFactory();
    }

    private List<Subscription> importSubscription(@NotNull String publishSettingsFilePath)
            throws AzureCmdException {
        try {
            StringBuilder sb = new StringBuilder();

            BufferedReader br = new BufferedReader(new FileReader(publishSettingsFilePath));
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }

            String subscriptionFile = OpenSSLHelper.processCertificate(sb.toString());

            NodeList subscriptionNodes = (NodeList) XmlHelper.getXMLValue(subscriptionFile, "//Subscription",
                    XPathConstants.NODESET);

            List<Subscription> subscriptions = new ArrayList<Subscription>();

            for (int i = 0; i < subscriptionNodes.getLength(); i++) {
                Node subscriptionNode = subscriptionNodes.item(i);
                Subscription subscription = new Subscription();
                subscription.setName(XmlHelper.getAttributeValue(subscriptionNode, "Name"));
                subscription.setId(XmlHelper.getAttributeValue(subscriptionNode, "Id"));
                subscription.setManagementCertificate(XmlHelper.getAttributeValue(subscriptionNode, "ManagementCertificate"));
                subscription.setServiceManagementUrl(XmlHelper.getAttributeValue(subscriptionNode, "ServiceManagementUrl"));
                subscription.setSelected(true);

                subscriptions.add(subscription);
            }

            return subscriptions;
        } catch (Exception ex) {
            if (ex instanceof AzureCmdException) {
                throw (AzureCmdException) ex;
            }

            throw new AzureCmdException("Error importing subscriptions from publish settings file", ex);
        }
    }

    private void updateSubscription(@NotNull Subscription subscription, @NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            String subscriptionId = subscription.getId();
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                if (subscriptions.containsKey(subscriptionId)) {
                    subscriptions.get(subscriptionId).setTenantId(subscription.getTenantId());
                } else {
                    subscriptions.put(subscriptionId, subscription);
                }

                setUserInfo(subscriptionId, userInfo);
                storeSubscriptions();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void updateSubscription(@NotNull Subscription subscription, @NotNull SSLSocketFactory sslSocketFactory)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            String subscriptionId = subscription.getId();
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                if (subscriptions.containsKey(subscriptionId)) {
                    Subscription existingSubscription = subscriptions.get(subscriptionId);
                    existingSubscription.setManagementCertificate(subscription.getManagementCertificate());
                    existingSubscription.setServiceManagementUrl(subscription.getServiceManagementUrl());
                } else {
                    subscriptions.put(subscriptionId, subscription);
                }

                setSSLSocketFactory(subscriptionId, sslSocketFactory);
                storeSubscriptions();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void notifySubscriptionsChanged() {
        subscriptionsChangedLock.readLock().lock();

        try {
            for (EventWaitHandleImpl handle : subscriptionsChangedHandles) {
                handle.signalEvent();
            }
        } finally {
            subscriptionsChangedLock.readLock().unlock();
        }
    }

    @Nullable
    public UserInfo getUserInfo() {
        authDataLock.readLock().lock();

        try {
            return userInfo;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setUserInfo(@Nullable UserInfo userInfo) {
        authDataLock.writeLock().lock();

        try {
            this.userInfo = userInfo;
            userInfoBySubscriptionId.clear();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    private Subscription getSubscription(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                return subscriptions.get(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasUserInfo(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return userInfoBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private UserInfo getUserInfo(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                if (!userInfoBySubscriptionId.containsKey(subscriptionId)) {
                    throw new AzureCmdException("No User Information for the specified Subscription Id");
                }

                return userInfoBySubscriptionId.get(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setUserInfo(@NotNull String subscriptionId, @NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                userInfoBySubscriptionId.put(subscriptionId, userInfo);

                storeUserInfo();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasSSLSocketFactory(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return sslSocketFactoryBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private Optional<SSLSocketFactory> getSSLSocketFactory(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                if (!sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                    return Optional.absent();
                }

                return Optional.of(sslSocketFactoryBySubscriptionId.get(subscriptionId));
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setSSLSocketFactory(@NotNull String subscriptionId, @NotNull SSLSocketFactory sslSocketFactory)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                sslSocketFactoryBySubscriptionId.put(subscriptionId, sslSocketFactory);
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasAccessToken(@NotNull UserInfo userInfo) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getUserLock(userInfo);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReadWriteLock userLock = optionalRWLock.get();
            userLock.readLock().lock();

            try {
                return accessTokenByUser.containsKey(userInfo);
            } finally {
                userLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private String getAccessToken(@NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock userLock = getUserLock(userInfo, false);
            userLock.readLock().lock();

            try {
                if (!accessTokenByUser.containsKey(userInfo)) {
                    throw new AzureCmdException("No access token for the specified User Information", "");
                }

                return accessTokenByUser.get(userInfo);
            } finally {
                userLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setAccessToken(@NotNull UserInfo userInfo,
                                @NotNull String accessToken)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
            userLock.writeLock().lock();

            try {
                accessTokenByUser.put(userInfo, accessToken);
            } finally {
                userLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private ReentrantReadWriteLock getSubscriptionLock(@NotNull String subscriptionId, boolean createOnMissing)
            throws AzureCmdException {
        Lock lock = createOnMissing ? subscriptionMapLock.writeLock() : subscriptionMapLock.readLock();
        lock.lock();

        try {
            if (!lockBySubscriptionId.containsKey(subscriptionId)) {
                if (createOnMissing) {
                    lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
                } else {
                    throw new AzureCmdException("No authentication information for the specified Subscription Id");
                }
            }

            return lockBySubscriptionId.get(subscriptionId);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    private Optional<ReentrantReadWriteLock> getSubscriptionLock(@NotNull String subscriptionId) {
        subscriptionMapLock.readLock().lock();

        try {
            if (lockBySubscriptionId.containsKey(subscriptionId)) {
                return Optional.of(lockBySubscriptionId.get(subscriptionId));
            } else {
                return Optional.absent();
            }
        } finally {
            subscriptionMapLock.readLock().unlock();
        }
    }

    @NotNull
    private ReentrantReadWriteLock getUserLock(@NotNull UserInfo userInfo, boolean createOnMissing)
            throws AzureCmdException {
        Lock lock = createOnMissing ? userMapLock.writeLock() : userMapLock.readLock();
        lock.lock();

        try {
            if (!lockByUser.containsKey(userInfo)) {
                if (createOnMissing) {
                    lockByUser.put(userInfo, new ReentrantReadWriteLock(false));
                } else {
                    throw new AzureCmdException("No access token for the specified User Information");
                }
            }

            return lockByUser.get(userInfo);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    private Optional<ReentrantReadWriteLock> getUserLock(@NotNull UserInfo userInfo) {
        userMapLock.readLock().lock();

        try {
            if (lockByUser.containsKey(userInfo)) {
                return Optional.of(lockByUser.get(userInfo));
            } else {
                return Optional.absent();
            }
        } finally {
            userMapLock.readLock().unlock();
        }
    }

    @NotNull
    private String executeGetRequest(@NotNull String subscriptionId, @NotNull String path)
            throws AzureCmdException {
        return executeRequest(subscriptionId, path, ContentType.Json, "GET", null);
    }

    @NotNull
    private String executeRequest(@NotNull String subscriptionId,
                                  @NotNull final String path,
                                  @NotNull final ContentType contentType,
                                  @NotNull final String method,
                                  @Nullable final String postData)
            throws AzureCmdException {
        Subscription subscription = getSubscription(subscriptionId);

        Optional<SSLSocketFactory> optionalSSLSocketFactory = getSSLSocketFactory(subscriptionId);

        if (optionalSSLSocketFactory.isPresent()) {
            SSLSocketFactory sslSocketFactory = optionalSSLSocketFactory.get();
            return AzureCertificateHelper.executeRequest(subscription.getServiceManagementUrl(), path, contentType,
                    method, postData, sslSocketFactory, MobileServiceRestManager.getManager());
        } else {
            final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
            final String managementUri = settings.getAzureServiceManagementUri();
            final UserInfo userInfo = getUserInfo(subscriptionId);
            return requestWithToken(userInfo, new RequestCallback<String>() {
                @Override
                public String execute()
                        throws Throwable {
                    String accessToken = getAccessToken(userInfo);
                    return AzureAADHelper.executeRequest(managementUri, path, contentType,
                            method, postData, accessToken, MobileServiceRestManager.getManager());
                }
            });
        }
    }

    @NotNull
    private String executePollRequest(@NotNull String subscriptionId,
                                      @NotNull final String path,
                                      @NotNull final ContentType contentType,
                                      @NotNull final String method,
                                      @Nullable final String postData,
                                      @NotNull final String pollPath)
            throws AzureCmdException {
        Subscription subscription = getSubscription(subscriptionId);

        Optional<SSLSocketFactory> optionalSSLSocketFactory = getSSLSocketFactory(subscriptionId);

        if (optionalSSLSocketFactory.isPresent()) {
            SSLSocketFactory sslSocketFactory = optionalSSLSocketFactory.get();
            return AzureCertificateHelper.executePollRequest(subscription.getServiceManagementUrl(), path, contentType,
                    method, postData, pollPath, sslSocketFactory, MobileServiceRestManager.getManager());
        } else {
            final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
            final String managementUri = settings.getAzureServiceManagementUri();
            final UserInfo userInfo = getUserInfo(subscriptionId);
            return requestWithToken(userInfo, new RequestCallback<String>() {
                @Override
                public String execute()
                        throws Throwable {
                    String accessToken = getAccessToken(userInfo);
                    return AzureAADHelper.executePollRequest(managementUri, path, contentType,
                            method, postData, pollPath, accessToken, MobileServiceRestManager.getManager());
                }
            });
        }
    }

    @NotNull
    private <T> T requestComputeSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, ComputeManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ComputeManagementClient>() {
            @NotNull
            @Override
            public ComputeManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getComputeManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public ComputeManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getComputeManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestStorageSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, StorageManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<StorageManagementClient>() {
            @NotNull
            @Override
            public StorageManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getStorageManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public StorageManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getStorageManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestNetworkSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, NetworkManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<NetworkManagementClient>() {
            @NotNull
            @Override
            public NetworkManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getNetworkManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public NetworkManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getNetworkManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestManagementSDK(@NotNull final String subscriptionId,
                                       @NotNull final SDKRequestCallback<T, ManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ManagementClient>() {
            @NotNull
            @Override
            public ManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public ManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T, V extends Closeable> T requestAzureSDK(@NotNull final String subscriptionId,
                                                       @NotNull final SDKRequestCallback<T, V> requestCallback,
                                                       @NotNull final AzureSDKClientProvider<V> clientProvider)
            throws AzureCmdException {
        if (hasSSLSocketFactory(subscriptionId)) {
            try {
                Subscription subscription = getSubscription(subscriptionId);
                V client = clientProvider.getSSLClient(subscription);

                try {
                    return requestCallback.execute(client);
                } finally {
                    client.close();
                }
            } catch (Throwable t) {
                if (t instanceof AzureCmdException) {
                    throw (AzureCmdException) t;
                } else if (t instanceof ExecutionException) {
                    throw new AzureCmdException(t.getCause().getMessage(), t.getCause());
                }

                throw new AzureCmdException(t.getMessage(), t);
            }
        } else {
            final UserInfo userInfo = getUserInfo(subscriptionId);
            PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

            com.microsoftopentechnologies.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
                    new com.microsoftopentechnologies.tooling.msservices.helpers.auth.RequestCallback<T>() {
                        @NotNull
                        @Override
                        public T execute(@NotNull String accessToken) throws Throwable {
                            if (!hasAccessToken(userInfo) ||
                                    !accessToken.equals(getAccessToken(userInfo))) {
                                ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                                userLock.writeLock().lock();

                                try {
                                    if (!hasAccessToken(userInfo) ||
                                            !accessToken.equals(getAccessToken(userInfo))) {
                                        setAccessToken(userInfo, accessToken);
                                    }
                                } finally {
                                    userLock.writeLock().unlock();
                                }
                            }

                            V client = clientProvider.getAADClient(subscriptionId, accessToken);

                            try {
                                return requestCallback.execute(client);
                            } finally {
                                client.close();
                            }
                        }
                    };

            return aadManager.request(userInfo,
                    settings.getAzureServiceManagementUri(),
                    "Sign in to your Azure account",
                    aadRequestCB);
        }
    }

    @NotNull
    private <T> T requestWithToken(@NotNull final UserInfo userInfo, @NotNull final RequestCallback<T> requestCallback)
            throws AzureCmdException {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

        com.microsoftopentechnologies.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
                new com.microsoftopentechnologies.tooling.msservices.helpers.auth.RequestCallback<T>() {
                    @NotNull
                    @Override
                    public T execute(@NotNull String accessToken) throws Throwable {
                        if (!hasAccessToken(userInfo) ||
                                !accessToken.equals(getAccessToken(userInfo))) {
                            ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                            userLock.writeLock().lock();

                            try {
                                if (!hasAccessToken(userInfo) ||
                                        !accessToken.equals(getAccessToken(userInfo))) {
                                    setAccessToken(userInfo, accessToken);
                                }
                            } finally {
                                userLock.writeLock().unlock();
                            }
                        }

                        return requestCallback.execute();
                    }
                };

        return aadManager.request(userInfo,
                settings.getAzureServiceManagementUri(),
                "Sign in to your Azure account",
                aadRequestCB);
    }

    @NotNull
    private static String readFile(@NotNull String filePath)
            throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

        try {
            return CharStreams.toString(in);
        } finally {
            in.close();
        }
    }
}