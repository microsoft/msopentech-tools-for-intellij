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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoftopentechnologies.intellij.forms.OpenSSLFinderForm;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import java.io.*;

public class OpenSSLHelper {
    public static final String PASSWORD = "Java6NeedsPwd";

    public static String processCertificate(String xmlPublishSettings) throws AzureCmdException {
        try {
            Node publishProfileNode = ((NodeList) XmlHelper.getXMLValue(xmlPublishSettings, "/PublishData/PublishProfile", XPathConstants.NODESET)).item(0);
            String version = XmlHelper.getAttributeValue(publishProfileNode, "SchemaVersion");

            boolean isFirstVersion = (version == null || Float.parseFloat(version) < 2);

            NodeList subscriptionList = (NodeList) XmlHelper.getXMLValue(xmlPublishSettings, "//Subscription", XPathConstants.NODESET);

            Document ownerDocument = null;

            for (int i = 0; i != subscriptionList.getLength(); i++) {
                //Gets the pfx info
                Element node = (Element) subscriptionList.item(i);
                ownerDocument = node.getOwnerDocument();
                String pfx = XmlHelper.getAttributeValue(isFirstVersion ? publishProfileNode : node, "ManagementCertificate");
                byte[] decodedBuffer = new BASE64Decoder().decodeBuffer(pfx);

                //Create pfxFile
                File tmpPath = new File(System.getProperty("java.io.tmpdir") + File.separator + "tempAzureCert");
                tmpPath.mkdirs();
                tmpPath.setWritable(true);

                File pfxFile = new File(tmpPath.getPath() + File.separator + "temp.pfx");

                pfxFile.createNewFile();
                pfxFile.setWritable(true);

                FileOutputStream pfxOutputStream = new FileOutputStream(pfxFile);
                pfxOutputStream.write(decodedBuffer);
                pfxOutputStream.flush();
                pfxOutputStream.close();

                String path = getOpenSSLPath();
                if (path == null || path.isEmpty()) {
                    throw new Exception("Please configure a valid OpenSSL executable location.");
                } else if (!path.endsWith(File.separator)) {
                    path = path + File.separator;
                }

                //Export to pem with OpenSSL
                runCommand(new String[]{path + "openssl", "pkcs12", "-in", "temp.pfx", "-out", "temp.pem", "-nodes", "-password", "pass:"}, tmpPath);

                //Export to pfx again and change password
                runCommand(new String[]{path + "openssl", "pkcs12", "-export", "-out", "temppwd.pfx", "-in", "temp.pem", "-password", "pass:" + PASSWORD}, tmpPath);

                //Read file and replace pfx with password protected pfx
                File pwdPfxFile = new File(tmpPath.getPath() + File.separator + "temppwd.pfx");
                byte[] buf = new byte[(int) pwdPfxFile.length()];
                FileInputStream pfxInputStream = new FileInputStream(pwdPfxFile);
                pfxInputStream.read(buf, 0, (int) pwdPfxFile.length());
                pfxInputStream.close();

                FileUtil.delete(tmpPath);

                node.setAttribute("ManagementCertificate", new BASE64Encoder().encode(buf).replace("\r", "").replace("\n", ""));

                if (isFirstVersion) {
                    node.setAttribute("ServiceManagementUrl", XmlHelper.getAttributeValue(publishProfileNode, "Url"));
                }
            }

            if (ownerDocument == null) {
                return null;
            }

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            Writer out = new StringWriter();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.transform(new DOMSource(ownerDocument), new StreamResult(out));

            return out.toString();
        } catch (Exception ex) {
            throw new AzureCmdException("Error processing publish settings file.", ex.getMessage());
        }
    }

    private static String getOpenSSLPath() {
        PropertiesComponent pc = PropertiesComponent.getInstance();
        String opendSSLlPath = pc.getValue("MSOpenSSLPath", "");

        if (!validOpenSSLPath(opendSSLlPath)) {
            opendSSLlPath = getOpenSSLPathFromEnvironment();

            if (!validOpenSSLPath(opendSSLlPath)) {
                opendSSLlPath = promptForOpenSSLPath(pc);

                if (!validOpenSSLPath(opendSSLlPath)) {
                    opendSSLlPath = null;
                }
            }
        }

        return opendSSLlPath;
    }

    private static String getOpenSSLPathFromEnvironment() {
        String osslPath = null;

        try {
            String mOsVersion = System.getProperty("os.name");
            String osName = mOsVersion.split(" ")[0];

            String cmd = osName.equals("Windows") ? "where openssl" : "which openssl";
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            osslPath = new File(reader.readLine()).getParent();
        } catch (Throwable ignored) {
        }

        return osslPath;
    }

    private static String promptForOpenSSLPath(PropertiesComponent pc) {
        OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm();
        openSSLFinderForm.setModal(true);
        UIHelper.packAndCenterJDialog(openSSLFinderForm);
        openSSLFinderForm.setVisible(true);

        return pc.getValue("MSOpenSSLPath", "");
    }

    private static boolean validOpenSSLPath(String osslPath) {
        boolean result = false;

        if (osslPath != null && !osslPath.isEmpty()) {
            try {
                result = new File(osslPath).exists();
            } catch (Throwable ignored) {
            }
        }

        return result;
    }

    private static void runCommand(String[] cmd, File path) throws AzureCmdException, IOException, InterruptedException {
        final Process p;

        Runtime runtime = Runtime.getRuntime();
        p = runtime.exec(
                cmd,
                null, //new String[] {"PRECOMPILE_STREAMLINE_FILES=1"},
                path);

        String errResponse = new String(FileUtil.adaptiveLoadBytes(p.getErrorStream()));

        if (p.waitFor() != 0) {
            AzureCmdException ex = new AzureCmdException("Error executing OpenSSL command \n", errResponse);
            ex.printStackTrace();

            throw ex;
        }
    }
}