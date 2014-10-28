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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Deprecated
public class AzureCommandManager implements AzureManager {

    private AzureCommandManager() {
    }

    public static AzureManager getManager() {
        return new AzureCommandManager();
    }

    public String getVersion() throws AzureCmdException {
        String[] cmd = new String[] {
            "--version"
        };

        String res = null;
        try {
            res = AzureCommandHelper.getInstance().consoleExec(cmd);
        } catch (AzureCmdException e) {
            e.printStackTrace();
        }

        return res;
    }

    @Override
    public void clearSubscriptions() throws AzureCmdException {
        String[] cmd = new String[] {
                "account",
                "clear",
                "-q"
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void clearAuthenticationTokens() {
        throw new UnsupportedOperationException("clearAuthenticationTokens");
    }

    @Override
    public ArrayList<Subscription> getSubscriptionList() throws AzureCmdException {

        String[] cmd = new String[] {
                "account",
                "list",
                "--json"
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,Object>> tempRes = (List<Map<String,Object>>) slurper.parseText(json);

        ArrayList<Subscription> res = new ArrayList<Subscription>();
        for(Map<String,Object> item : tempRes) {
            Subscription s = new Subscription();
            s.setId(UUID.fromString(item.get("id").toString()));
            s.setName(item.get("name").toString());
            s.setCurrent((Boolean) item.get("isDefault"));

            res.add(s);
        }

        return res;
    }

    @Override
    public AzureAuthenticationMode getAuthenticationMode() {
        throw new UnsupportedOperationException("getAuthenticationMode");
    }

    @Override
    public void setAuthenticationMode(AzureAuthenticationMode azureAuthenticationMode) {
        throw new UnsupportedOperationException("setAuthenticationMode");
    }

    @Override
    public AuthenticationResult getAuthenticationToken() {
        throw new UnsupportedOperationException("getAuthenticationToken");
    }

    @Override
    public void setAuthenticationToken(AuthenticationResult authenticationToken) {
        throw new UnsupportedOperationException("setAuthenticationToken");
    }

    @Override
    public AuthenticationResult getAuthenticationTokenForSubscription(String subscriptionId) {
        throw new UnsupportedOperationException("getAuthenticationTokenForSubscription");
    }

    @Override
    public void setAuthenticationTokenForSubscription(String subscriptionId, AuthenticationResult authenticationToken) {
        throw new UnsupportedOperationException("setAuthenticationTokenForSubscription");
    }

    @Override
    public Subscription getSubscriptionFromId(String subscriptionId) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, ExecutionException, InterruptedException, KeyManagementException, KeyStoreException, AzureCmdException, NoSubscriptionException {
        throw new UnsupportedOperationException("getSubscriptionFromId");
    }

    @Override
    public void loadSubscriptionFile(String subscriptionFile) throws AzureCmdException {
        String[] cmd = new String[] {
                "account",
                "import",
                "--json",
                "-v",
                subscriptionFile
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void removeSubscription(String subscriptionId) throws AzureCmdException {

    }

    public void setSubscription(String subscriptionName) throws AzureCmdException  {
        String[] cmd = new String[] {
                "account",
                "set",
                "-v",
                subscriptionName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }


    public List<String> getLocations(UUID subscriptionId) throws AzureCmdException  {
        String[] cmd = new String[] {
                "mobile",
                "locations",
                "--json"
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,String>> tempRes = (List<Map<String,String>>) slurper.parseText(json);

        List<String> res = new ArrayList<String>();
        for(Map<String,String> item : tempRes) {
            res.add(item.get("region"));
        }

        return res;
    }


    @Override
    public List<SqlDb> getSqlDb(UUID subscriptionId, SqlServer server) throws AzureCmdException {
        return null;
    }

    public List<SqlDb> getSqlDb(UUID subscriptionId, String serverName, String admin, String adminPassword) throws AzureCmdException {
        String[] cmd = new String[] {
            "sql",
            "db",
            "list",
            "--json",
            "-s",
            subscriptionId.toString(),
            serverName,
            admin,
            adminPassword
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,String>> tempRes = (List<Map<String,String>>) slurper.parseText(json);

        List<SqlDb> res = new ArrayList<SqlDb>();
        for(Map<String,String> item : tempRes) {

            SqlDb sqls = new SqlDb();
            sqls.setName(item.get("Name"));
            sqls.setEdition(item.get("Edition"));
            res.add(sqls);
        }

        return res;
    }

    @Override
    public List<SqlServer> getSqlServers(UUID subscriptionId) throws AzureCmdException {
        String[] cmd = new String[] {
                "sql",
                "server",
                "list",
                "--json",
                "-s",
                subscriptionId.toString()
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,String>> tempRes = (List<Map<String,String>>) slurper.parseText(json);

        List<SqlServer> res = new ArrayList<SqlServer>();
        for(Map<String,String> item : tempRes) {

            SqlServer sqls = new SqlServer();
            sqls.setAdmin(item.get("administratorUserName"));
            sqls.setName(item.get("name"));
            sqls.setRegion(item.get("location"));
            res.add(sqls);
        }

        return res;
    }

    @Override
    public void createService(UUID subscriptionId, String region, String username, String password, String serviceName, String server, String database) throws AzureCmdException {
        String[] cmd;
        if (database == null || server == null)
            cmd = new String[] {
                "mobile",
                "create",
                "--json",
                "-s",
                subscriptionId.toString(),
                "-l",
                region,
                serviceName,
                username,
                password
            };
        else
            cmd = new String[] {
                "mobile",
                "create",
                "--json",
                "-s",
                subscriptionId.toString(),
                "-l",
                region,
                "-r",
                server,
                "-d",
                database,
                serviceName,
                username,
                password
            };

        AzureCommandHelper.getInstance().consoleExec(cmd);

    }

    @Override
    public List<Service> getServiceList(UUID subscriptionId) throws AzureCmdException  {

        String[] cmd = new String[] { "mobile", "list", "--json","-s", subscriptionId.toString()};

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,Object>> tempRes = (List<Map<String,Object>>) slurper.parseText(json);

        List<Service> res = new ArrayList<Service>();
        for(Map<String,Object> item : tempRes) {
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

            for(Map<String,String> table : (List<Map<String,String>>) item.get("tables")) {
                Table t = new Table();
                t.setName(table.get("name"));
                t.setSelfLink(table.get("selflink"));
                ser.getTables().add(t);
            }
            res.add(ser);
        }

        return res;
    }

    @Override
    public List<Table> getTableList(UUID subscriptionId, String serviceName) throws AzureCmdException  {

        String[] cmd = new String[] {
            "mobile",
            "table",
            "list",
            "--json",
            "-s",
            subscriptionId.toString(),
            serviceName
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,String>> tempRes = (List<Map<String,String>>) slurper.parseText(json);

        List<Table> res = new ArrayList<Table>();
        for(Map<String,String> item : tempRes) {
            Table t = new Table();
            t.setName(item.get("name"));
            t.setSelfLink(item.get("selflink"));

            res.add(t);
        }

        return res;
    }

    @Override
    public void createTable(UUID subscriptionId, String serviceName, String tableName, TablePermissions permissions) throws AzureCmdException {
        String[] cmd;
        if (permissions == null)
            cmd = new String[] {
                "mobile",
                "table",
                "create",
                "-v",
                "-s",
                subscriptionId.toString(),
                serviceName,
                tableName
            };
        else
            cmd = new String[] {
                "mobile",
                "table",
                "create",
                "-v",
                "-p",
                permissions.toString(),
                "-s",
                subscriptionId.toString(),
                serviceName,
                tableName
            };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void updateTable(UUID subscriptionId, String serviceName, String tableName, TablePermissions permissions) throws AzureCmdException {
        String[] cmd = new String[] {
            "mobile",
            "table",
            "update",
            "-v",
            "-p",
            permissions.toString(),
            "-s",
            subscriptionId.toString(),
            serviceName,
            tableName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }



    @Override
    public Table showTableDetails(UUID subscriptionId, String serviceName, String tableName) throws AzureCmdException   {
        String[] cmd = new String[] {
            "mobile",
            "table",
            "show",
            "--json",
            "-s",
            subscriptionId.toString(),
            serviceName,
            tableName
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        Map<String,Object> tempRes = (Map<String,Object>) slurper.parseText(json);

        Table t = new Table();

        Map<String, Object> tableData = (Map<String, Object>) tempRes.get("table");
        t.setName(tableData.get("name").toString());
        t.setSelfLink(tableData.get("selflink").toString());

        Map<String,String> per = (Map<String,String>) tempRes.get("permissions");

        TablePermissions tablePermissions = new TablePermissions();
        tablePermissions.setInsert(PermissionItem.getPermitionType(per.get("insert")));
        tablePermissions.setUpdate(PermissionItem.getPermitionType(per.get("update")));
        tablePermissions.setRead(PermissionItem.getPermitionType(per.get("read")));
        tablePermissions.setDelete(PermissionItem.getPermitionType(per.get("delete")));
        t.setTablePermissions(tablePermissions);

        for(Map<String, Object> column : (List<Map<String, Object>>) tempRes.get("columns")) {
            Column c = new Column();
            c.setName(column.get("name").toString());
            c.setType(column.get("type").toString());
            c.setSelfLink(column.get("selflink").toString());
            c.setIndexed((Boolean) column.get("indexed"));
            c.setZumoIndex((Boolean) column.get("zumoIndex"));

            t.getColumns().add(c);
        }

        for(Map<String, Object> script : (List<Map<String, Object>>) tempRes.get("scripts")) {
            Script s = new Script();

            s.setOperation(script.get("operation").toString());
            s.setBytes((Integer) script.get("sizeBytes"));
            s.setSelfLink(script.get("selflink").toString());
            s.setName(String.format("%s.%s",tableData.get("name"), script.get("operation").toString()));

            t.getScripts().add(s);
        }



        return t;
    }

    @Override
    public void downloadTableScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException {
        String[] cmd = new String[] {
            "mobile",
            "script",
            "download",
            "--json",
            "-o",
            "-s",
            subscriptionId.toString(),
            "-f",
            downloadPath,
            serviceName,
            "table/"+ scriptName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void uploadTableScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException {
        String[] cmd = new String[] {
            "mobile",
            "script",
            "upload",
            "--json",
            "-s",
            subscriptionId.toString(),
            "-f",
            filePath,
            serviceName,
            "table/" + scriptName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public List<CustomAPI> getAPIList(UUID subscriptionId, String serviceName) throws AzureCmdException  {

        String[] cmd = new String[] {
                "mobile",
                "api",
                "list",
                "--json",
                "-s",
                subscriptionId.toString(),
                serviceName
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,String>> tempRes = (List<Map<String,String>>) slurper.parseText(json);

        List<CustomAPI> res = new ArrayList<CustomAPI>();
        for(Map<String,String> item : tempRes) {
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
    }

    @Override
    public void downloadAPIScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException {
        String[] cmd = new String[] {
                "mobile",
                "script",
                "download",
                "--json",
                "-o",
                "-s",
                subscriptionId.toString(),
                "-f",
                downloadPath,
                serviceName,
                "api/"+ scriptName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void uploadAPIScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException {
        String[] cmd = new String[] {
                "mobile",
                "script",
                "upload",
                "--json",
                "-s",
                subscriptionId.toString(),
                "-f",
                filePath,
                serviceName,
                "api/" + scriptName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void createCustomAPI(UUID subscriptionId, String serviceName, String tableName, CustomAPIPermissions permissions) throws AzureCmdException {

        String[] cmd;
        if (permissions == null)
            cmd = new String[] {
                    "mobile",
                    "api",
                    "create",
                    "-s",
                    subscriptionId.toString(),
                    serviceName,
                    tableName
            };
        else
            cmd = new String[] {
                    "mobile",
                    "api",
                    "create",
                    "-p",
                    permissions.toString(),
                    "-s",
                    subscriptionId.toString(),
                    serviceName,
                    tableName
            };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void updateCustomAPI(UUID subscriptionId, String serviceName, String tableName, CustomAPIPermissions permissions) throws AzureCmdException {

        String[] cmd;

        cmd = new String[] {
                "mobile",
                "api",
                "update",
                "-p",
                permissions.toString(),
                "-s",
                subscriptionId.toString(),
                serviceName,
                tableName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public List<Job> listJobs(UUID subscriptionId, String serviceName) throws AzureCmdException {
        String[] cmd = new String[] {
                "mobile",
                "job",
                "list",
                "--json",
                "-s",
                subscriptionId.toString(),
                serviceName,
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();
        List<Map<String,Object>> tempRes = (List<Map<String,Object>>) slurper.parseText(json);

        List<Job> res = new ArrayList<Job>();
        for(Map<String,Object> item : tempRes) {
            Job j = new Job();
            j.setAppName(item.get("appName").toString());
            j.setName(item.get("name").toString());
            j.setEnabled(item.get("status").equals("enabled"));
            j.setId(UUID.fromString(item.get("id").toString()));

            if(item.get("intervalPeriod") != null) {
                j.setIntervalPeriod((Integer) item.get("intervalPeriod"));
                j.setIntervalUnit(item.get("intervalUnit").toString());
            }

            res.add(j);
        }

        return res;
    }

    @Override
    public void createJob(UUID subscriptionId, String serviceName, String jobName, int interval, String intervalUnit, String startDate) throws AzureCmdException {
        String[] cmd = null;

        if(startDate == null)
            cmd = new String[] {
                    "mobile",
                    "job",
                    "create",
                    "-s",
                    subscriptionId.toString(),
                    "-i",
                    String.valueOf(interval),
                    "-u",
                    intervalUnit,
                    serviceName,
                    jobName
            };
        else
            cmd = new String[] {
                "mobile",
                "job",
                "create",
                "-s",
                subscriptionId.toString(),
                "-i",
                String.valueOf(interval),
                "-u",
                intervalUnit,
                "-t",
                startDate,
                serviceName,
                jobName
            };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void updateJob(UUID subscriptionId, String serviceName, String jobName, int interval, String intervalUnit, String startDate, boolean enabled) throws AzureCmdException {
        String[] cmd = null;

        if(startDate == null)
            cmd = new String[] {
                    "mobile",
                    "job",
                    "update",
                    "-s",
                    subscriptionId.toString(),
                    "-i",
                    String.valueOf(interval),
                    "-u",
                    intervalUnit,
                    "-a",
                    enabled ? "enabled" : "disabled",
                    serviceName,
                    jobName
            };
        else
            cmd = new String[] {
                    "mobile",
                    "job",
                    "update",
                    "-s",
                    subscriptionId.toString(),
                    "-i",
                    String.valueOf(interval),
                    "-u",
                    intervalUnit,
                    "-a",
                    enabled ? "enabled" : "disabled",
                    "-t",
                    startDate,
                    serviceName,
                    jobName
            };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }


    @Override
    public void downloadJobScript(UUID subscriptionId, String serviceName, String scriptName, String downloadPath) throws AzureCmdException {
        String[] cmd = new String[] {
                "mobile",
                "script",
                "download",
                "--json",
                "-o",
                "-s",
                subscriptionId.toString(),
                "-f",
                downloadPath,
                serviceName,
                "scheduler/"+ scriptName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    @Override
    public void uploadJobScript(UUID subscriptionId, String serviceName, String scriptName, String filePath) throws AzureCmdException {
        String[] cmd = new String[] {
                "mobile",
                "script",
                "upload",
                "--json",
                "-s",
                subscriptionId.toString(),
                "-f",
                filePath,
                serviceName,
                "scheduler/" + scriptName
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }


    @Override
    public List<LogEntry> listLog(UUID subscriptionId, String serviceName) throws AzureCmdException, ParseException {
        String[] cmd = new String[] {
                "mobile",
                "log",
                "--json",
                "-s",
                subscriptionId.toString(),
                serviceName,
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);

        CustomJsonSlurper slurper = new CustomJsonSlurper();

        Map<String,Object> results = (Map<String,Object>) slurper.parseText(json);
        List<Map<String,String>> tempRes = (List<Map<String,String>>) results.get("results");

        List<LogEntry> res = new ArrayList<LogEntry>();
        for(Map<String,String> item : tempRes) {
            LogEntry logEntry = new LogEntry();

            logEntry.setMessage(item.get("message"));
            logEntry.setSource(item.get("source"));
            logEntry.setType(item.get("type"));

            SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
            logEntry.setTimeCreated(ISO8601DATEFORMAT.parse(item.get("timeCreated")));

            res.add(logEntry);
        }

        return res;
    }

    public void addFirewallIP(String serverName, String IPAddress) throws AzureCmdException {
        String[] cmd = new String[] {
                "sql",
                "firewallrule",
                "create",
                "--serverName",
                serverName,
                "--ruleName",
                "IntelliJPluginLocalIP",
                "--startIPAddress",
                IPAddress,
                "--endIPAddress",
                IPAddress
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    public void deleteFirewallIP(String serverName, String name) throws AzureCmdException {
        String[] cmd = new String[] {
                "sql",
                "firewallrule",
                "delete",
                "-q",
                "--serverName",
                serverName,
                "--ruleName",
                name
        };

        AzureCommandHelper.getInstance().consoleExec(cmd);
    }

    public List<String> listFirewallIP(String serverName) throws AzureCmdException {
        String[] cmd = new String[] {
                "sql",
                "firewallrule",
                "list",
                "--json",
                "--serverName",
                serverName,
        };

        String json = AzureCommandHelper.getInstance().consoleExec(cmd);
        CustomJsonSlurper slurper = new CustomJsonSlurper();

        List<Map<String,Object>> tempRes = (List<Map<String,Object>>) slurper.parseText(json);

        List<String> res = new ArrayList<String>();
        for(Map<String,Object> item : tempRes)
            res.add(item.get("name").toString());

        return res;
    }
}