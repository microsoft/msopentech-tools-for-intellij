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
package com.microsoftopentechnologies.tooling.msservices.helpers;

import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URL;
import java.util.Scanner;

public class ServiceCodeReferenceHelper {
    private static final String AZURESDK_URL = "http://zumo.blob.core.windows.net/sdk/azuresdk-android-1.1.5.zip";
    private static final String TEMPLATES_URL = "/com/microsoftopentechnologies/intellij/templates/";
    public static final String NOTIFICATIONHUBS_PATH = "notificationhubs/";
    public static final String STRINGS_XML = "src/main/res/values/strings.xml";

    public ServiceCodeReferenceHelper() {
    }

    public static InputStream getTemplateResource(String libTemplate) {
        return ServiceCodeReferenceHelper.class.getResourceAsStream(TEMPLATES_URL + libTemplate);
    }

    public void addNotificationHubsLibs(Object module)
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException {
        //Downloads libraries
        String path = System.getProperty("java.io.tmpdir");

        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }

        path = path + "TempAzure";

        File pathFile = new File(path);

        if (pathFile.exists() || pathFile.mkdirs()) {
            path = path + File.separator + "androidAzureSDK.zip";

            File zipFile = new File(path);

            if (!zipFile.exists()) {
                saveUrl(path, AZURESDK_URL);
            }
            DefaultLoader.getIdeHelper().copyJarFiles2Module(module, zipFile);
        }
    }

    public void fillMobileServiceResource(String activityName, String appUrl, String appKey, Object module) throws IOException {
        DefaultLoader.getIdeHelper().replaceInFile(module, new ImmutablePair<String, String>(">$APPURL_" + activityName + "<", ">" + appUrl + "<"),
                new ImmutablePair<String, String>(">$APPKEY_" + activityName + "<", ">" + appKey + "<"));
    }

    public void fillNotificationHubResource(String activityName, String senderId, String connStr, String hubName, Object module) {
        DefaultLoader.getIdeHelper().replaceInFile(module,new ImmutablePair<String, String>(">$SENDERID_" + activityName + "<", ">" + senderId + "<"),
                new ImmutablePair<String, String>(">$CONNSTR_" + activityName + "<", ">" + connStr + "<"),
                new ImmutablePair<String, String>(">$HUBNAME_" + activityName + "<", ">" + hubName + "<"));
    }

    public void fillOffice365Resource(String activityName, String appId, String name, Object module) {
        DefaultLoader.getIdeHelper().replaceInFile(module, new ImmutablePair<String, String>(">$O365_APP_ID_" + activityName + "<", ">" + appId + "<"),
                new ImmutablePair<String, String>(">$O365_NAME_" + activityName + "<", ">" + name + "<"));
    }

    private static void saveUrl(String filename, String urlString)
            throws IOException {
        InputStream in = null;
        FileOutputStream fout = null;

        try {
            in = new URL(urlString).openStream();
            fout = new FileOutputStream(filename);

            byte data[] = new byte[1024];
            int count;

            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            if (in != null) {
                in.close();
            }

            if (fout != null) {
                fout.close();
            }
        }
    }

    public static String getStringAndCloseStream(InputStream is) throws IOException {
        //Using the trick described in this link to read whole streams in one operation:
        //http://stackoverflow.com/a/5445161
        try {
            Scanner s = new Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
        finally {
            is.close();
        }
    }


    public static String getStringAndCloseStream(InputStream is, String charsetName) throws IOException {
        //Using the trick described in this link to read whole streams in one operation:
        //http://stackoverflow.com/a/5445161
        try {
            Scanner s = new Scanner(is, charsetName).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
        finally {
            is.close();
        }
    }
}