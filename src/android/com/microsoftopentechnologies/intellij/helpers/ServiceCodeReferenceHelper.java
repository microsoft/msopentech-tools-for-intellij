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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ServiceCodeReferenceHelper {
    private static final String AZURESDK_URL = "http://zumo.blob.core.windows.net/sdk/azuresdk-android-1.1.5.zip";

    private static final String TEMPLATES_URL = "/com/microsoftopentechnologies/intellij/templates/";

    private static final String NOTIFICATIONHELPER_PATH = "notifications/";
    private static final String NOTIFICATIONHUBS_PATH = "notificationhubs/";
    private static final String NOTIFICATIONHUBS_LIBTEMPLATE = "notification.xml";
    private static final String NOTIFICATIONHUBS_LIBNAME = "notification";

    private static final String STRINGS_XML = "src/main/res/values/strings.xml";

    private Project project;
    private Module module;
    private String sourcePath;

    public ServiceCodeReferenceHelper(Project project, Module module) {
        this.project = project;
        this.module = module;
    }

    public static InputStream getTemplateResource(String libTemplate) {
        return ServiceCodeReferenceHelper.class.getResourceAsStream(TEMPLATES_URL + libTemplate);
    }

    public void addNotificationHubsLibs()
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException {
        addReferences(NOTIFICATIONHUBS_PATH, NOTIFICATIONHUBS_LIBTEMPLATE, NOTIFICATIONHUBS_LIBNAME);

    }

    public void addOutlookServicesLibs() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        updateSourceAndModule();
    }

    public void addFileServicesLibs() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        updateSourceAndModule();
    }

    public void addListServicesLibs() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        updateSourceAndModule();
    }

    public void addOutlookServicesClass(String packageLiteral, String endpointUrl) {
        if (sourcePath != null) {
            InputStream is = ServiceCodeReferenceHelper.class.getResourceAsStream("/com/microsoftopentechnologies/intellij/templates/OutlookServicesClient.codetemplate");
            String template = getString(is);

            template = template.replace("$PACKAGE", packageLiteral);
            template = template.replace("$ENDPOINTURL", endpointUrl);

            final String code = template;

            String basePath = sourcePath.replace("file://", "");

            if (!basePath.endsWith(File.separator)) {
                basePath = basePath + File.separator;
            }

            for (String packagePart : packageLiteral.split("[.]")) {
                basePath = basePath + packagePart + File.separator;
            }

            File folder = new File(basePath);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            final File file = new File(basePath + "OutlookServicesClient.java");

            try {

                if (!file.exists()) {
                    file.createNewFile();
                }

                final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                if (vf != null) {
                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        vf.setBinaryContent(code.getBytes());
                                        FileEditorManager.getInstance(project).openFile(vf, false);
                                    } catch (Throwable ex) {
                                        UIHelper.showException("Error trying to create Outlook Services client class", ex);
                                    }
                                }
                            });
                        }
                    }, ModalityState.defaultModalityState());
                }
            } catch (Throwable ex) {
                UIHelper.showException("Error trying to create Outlook Services client class", ex);
            }
        }
    }

    public void addFileServicesClass(String packageLiteral, String endpointUrl) {
        if (sourcePath != null) {
            InputStream is = ServiceCodeReferenceHelper.class.getResourceAsStream("/com/microsoftopentechnologies/intellij/templates/FileServicesClient.codetemplate");
            String template = getString(is);

            template = template.replace("$PACKAGE", packageLiteral);
            template = template.replace("$ENDPOINTURL", endpointUrl);

            final String code = template;

            String basePath = sourcePath.replace("file://", "");

            if (!basePath.endsWith(File.separator)) {
                basePath = basePath + File.separator;
            }

            for (String packagePart : packageLiteral.split("[.]")) {
                basePath = basePath + packagePart + File.separator;
            }

            File folder = new File(basePath);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            final File file = new File(basePath + "FileServicesClient.java");

            try {

                if (!file.exists()) {
                    file.createNewFile();
                }

                final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                if (vf != null) {
                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        vf.setBinaryContent(code.getBytes());
                                        FileEditorManager.getInstance(project).openFile(vf, false);
                                    } catch (Throwable ex) {
                                        UIHelper.showException("Error trying to create File Services client class", ex);
                                    }
                                }
                            });
                        }
                    }, ModalityState.defaultModalityState());
                }
            } catch (Throwable ex) {
                UIHelper.showException("Error trying to create File Services client class", ex);
            }
        }
    }

    public void addListServicesClass(String packageLiteral, String endpointUrl, String siteUrl) {
        if (sourcePath != null) {
            InputStream is = ServiceCodeReferenceHelper.class.getResourceAsStream("/com/microsoftopentechnologies/intellij/templates/ListServicesClient.codetemplate");
            String template = getString(is);

            template = template.replace("$PACKAGE", packageLiteral);
            template = template.replace("$ENDPOINTURL", endpointUrl);
            template = template.replace("$SITEURL", siteUrl);

            final String code = template;

            String basePath = sourcePath.replace("file://", "");

            if (!basePath.endsWith(File.separator)) {
                basePath = basePath + File.separator;
            }

            for (String packagePart : packageLiteral.split("[.]")) {
                basePath = basePath + packagePart + File.separator;
            }

            File folder = new File(basePath);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            final File file = new File(basePath + "ListServicesClient.java");

            try {

                if (!file.exists()) {
                    file.createNewFile();
                }

                final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                if (vf != null) {
                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        vf.setBinaryContent(code.getBytes());
                                        FileEditorManager.getInstance(project).openFile(vf, false);
                                    } catch (Throwable ex) {
                                        UIHelper.showException("Error trying to create List Services client class", ex);
                                    }
                                }
                            });
                        }
                    }, ModalityState.defaultModalityState());
                }
            } catch (Throwable ex) {
                UIHelper.showException("Error trying to create List Services client class", ex);
            }
        }
    }

    public void fillMobileServiceResource(String activityName, String appUrl, String appKey) throws IOException {
        VirtualFile vf = module.getModuleFile().getParent().findFileByRelativePath(STRINGS_XML);

        if (vf != null) {
            FileDocumentManager fdm = FileDocumentManager.getInstance();
            com.intellij.openapi.editor.Document document = fdm.getDocument(vf);

            if (document != null) {
                String content = document.getText();
                content = content.replace(">$APPURL_" + activityName + "<", ">" + appUrl + "<");
                content = content.replace(">$APPKEY_" + activityName + "<", ">" + appKey + "<");
                document.setText(content);
                fdm.saveDocument(document);
            }
        }
    }

    public void fillNotificationHubResource(String activityName, String senderId, String connStr, String hubName) {
        VirtualFile vf = module.getModuleFile().getParent().findFileByRelativePath(STRINGS_XML);

        if (vf != null) {
            FileDocumentManager fdm = FileDocumentManager.getInstance();
            com.intellij.openapi.editor.Document document = fdm.getDocument(vf);

            if (document != null) {
                String content = document.getText();
                content = content.replace(">$SENDERID_" + activityName + "<", ">" + senderId + "<");
                content = content.replace(">$CONNSTR_" + activityName + "<", ">" + connStr + "<");
                content = content.replace(">$HUBNAME_" + activityName + "<", ">" + hubName + "<");
                document.setText(content);
                fdm.saveDocument(document);
            }
        }
    }

    public static Boolean isAndroidGradleModule(VirtualFile virtualFileDir) throws IOException {
        VirtualFile buildGradleFile = null;
        for (VirtualFile file : virtualFileDir.getChildren()) {
            if (file.getName().contains("build.gradle"))
                buildGradleFile = file;
        }

        if (buildGradleFile == null)
            return false;

        return getString(buildGradleFile.getInputStream()).contains("apply plugin: 'com.android.application'");
    }

    private void addReferences(String zipPath, String libTemplate, String libName)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException {

        //Downloads libraries
        String path = System.getProperty("java.io.tmpdir");
        if (!path.endsWith(File.separator))
            path = path + File.separator;

        path = path + "TempAzure";
        File pathFile = new File(path);
        if (!pathFile.exists())
            pathFile.mkdirs();

        path = path + File.separator + "androidAzureSDK.zip";

        File zipFile = new File(path);

        if (!zipFile.exists())
            saveUrl(path, AZURESDK_URL);

        //Write all android modules to add new reference
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            File tempModuleFolder = new File(module.getModuleFilePath()).getParentFile();

            if (tempModuleFolder.exists()) {

                VirtualFile virtualFileDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempModuleFolder);

                if (isAndroidGradleModule(virtualFileDir)) {

                    copyJarFiles(virtualFileDir, zipFile, zipPath);

                    if (zipPath.equals(NOTIFICATIONHUBS_PATH))
                        copyJarFiles(virtualFileDir, zipFile, NOTIFICATIONHELPER_PATH);

                    //hardcoded path for gradle project
                    sourcePath = virtualFileDir.getUrl() + "/src/main/java";
                } else {

                    final VirtualFile moduleFile = module.getModuleFile();

                    if (moduleFile != null) {

                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(moduleFile.getInputStream());

                        final XPathFactory xPathfactory = XPathFactory.newInstance();

                        XPath isAndroidModuleXpath = xPathfactory.newXPath();
                        XPathExpression isAndroidModuleQuery = isAndroidModuleXpath.compile("boolean(//facet[@type='android'])");
                        Boolean isAndroidModule = (Boolean) isAndroidModuleQuery.evaluate(doc, XPathConstants.BOOLEAN);

                        if (isAndroidModule) {
                            //Unzips libraries and copies them to libs folder
                            copyJarFiles(project.getBaseDir(), zipFile, zipPath);
                            if (zipPath.equals(NOTIFICATIONHUBS_PATH))
                                copyJarFiles(project.getBaseDir(), zipFile, NOTIFICATIONHELPER_PATH);

                            //Add project level reference
                            VirtualFile ideaFolder = project.getProjectFile().getParent();
                            VirtualFile librariesFolder = null;
                            for (VirtualFile vf : ideaFolder.getChildren()) {
                                if (vf.getName().equals("libraries"))
                                    librariesFolder = vf;
                            }

                            if (librariesFolder == null)
                                librariesFolder = ideaFolder.createChildDirectory(project, "libraries");

                            final VirtualFile mobileServiceRefFile = librariesFolder.createChildData(project, libTemplate);

                            InputStream is = getTemplateResource(libTemplate);
                            final String template = getString(is);
                            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                mobileServiceRefFile.setBinaryContent(template.getBytes());
                                            } catch (Throwable ex) {
                                                UIHelper.showException("Error trying to configure Azure Mobile Services", ex);
                                            }
                                        }
                                    });
                                }
                            }, ModalityState.defaultModalityState());

                            //Sets the module main source path
                            XPath xpathSources = xPathfactory.newXPath();
                            XPathExpression sourcesQuery = xpathSources.compile("//sourceFolder");
                            NodeList sources = ((org.w3c.dom.NodeList) sourcesQuery.evaluate(doc, XPathConstants.NODESET));

                            for (int i = 0; i < sources.getLength() && sourcePath == null; i++) {
                                String url = sources.item(i).getAttributes().getNamedItem("url").getNodeValue();
                                if (url.contains("src")) {
                                    sourcePath = url.replace("file://$MODULE_DIR$", moduleFile.getParent().getUrl());
                                }
                            }

                            //Adds the libraries
                            XPath xpathComponent = xPathfactory.newXPath();
                            XPathExpression componentQuery = xpathComponent.compile("//component[@name='NewModuleRootManager']");
                            Node component = ((org.w3c.dom.NodeList) componentQuery.evaluate(doc, XPathConstants.NODESET)).item(0);

                            XPath existsLibEntryXPath = xPathfactory.newXPath();
                            XPathExpression existsLibEntryQuery = existsLibEntryXPath.compile("boolean(//orderEntry[@name='" + libName + "' and @type='library'])");
                            Boolean existsLibEntry = (Boolean) existsLibEntryQuery.evaluate(doc, XPathConstants.BOOLEAN);

                            if (!existsLibEntry) {
                                Element orderEntry = doc.createElement("orderEntry");
                                orderEntry.setAttribute("type", "library");
                                orderEntry.setAttribute("name", libName);
                                orderEntry.setAttribute("level", "project");

                                component.appendChild(orderEntry);

                                // Use a Transformer for output
                                TransformerFactory tFactory = TransformerFactory.newInstance();
                                Transformer transformer = tFactory.newTransformer();
                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                                DOMSource source = new DOMSource(doc);
                                StringWriter writer = new StringWriter();
                                StreamResult result = new StreamResult(writer);
                                transformer.transform(source, result);
                                final byte[] buff = writer.getBuffer().toString().getBytes();

                                if (moduleFile != null) {
                                    moduleFile.setWritable(true);
                                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                        @Override
                                        public void run() {
                                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        moduleFile.setBinaryContent(buff);
                                                    } catch (Throwable ex) {
                                                        UIHelper.showException("Error trying to configure Azure Mobile Services", ex);
                                                    }
                                                }
                                            });
                                        }
                                    }, ModalityState.defaultModalityState());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void copyJarFiles(VirtualFile baseDir, File zipFile, String zipPath) throws IOException {
        if (baseDir.isDirectory()) {
            final ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory() && zipEntry.getName().startsWith(zipPath) && zipEntry.getName().endsWith(".jar")) {
                    VirtualFile libsVf = null;
                    for (VirtualFile vf : baseDir.getChildren()) {
                        if (vf.getName().equals("libs")) {
                            libsVf = vf;
                            break;
                        }
                    }

                    if (libsVf == null) {
                        libsVf = baseDir.createChildDirectory(project, "libs");
                    }

                    final VirtualFile libs = libsVf;

                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        InputStream mobileserviceInputStream = zip.getInputStream(zipEntry);
                                        VirtualFile msVF = libs.createChildData(project, zipEntry.getName().split("/")[1]);
                                        msVF.setBinaryContent(getArray(mobileserviceInputStream));
                                    } catch (Throwable ex) {
                                        UIHelper.showException("Error trying to configure Azure Mobile Services", ex);
                                    }
                                }
                            });
                        }
                    }, ModalityState.defaultModalityState());
                }
            }
        }
    }

    private void updateSourceAndModule() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        //Write all android modules to add new reference
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            File tempModuleFolder = new File(module.getModuleFilePath()).getParentFile();

            if (tempModuleFolder.exists()) {
                VirtualFile virtualFileDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempModuleFolder);

                if (isAndroidGradleModule(virtualFileDir)) {
                    //hardcoded path for gradle project
                    sourcePath = virtualFileDir.getUrl() + "/src/main/java";
                } else {
                    final VirtualFile moduleFile = module.getModuleFile();

                    if (moduleFile != null) {
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(moduleFile.getInputStream());

                        final XPathFactory xPathfactory = XPathFactory.newInstance();

                        XPath isAndroidModuleXpath = xPathfactory.newXPath();
                        XPathExpression isAndroidModuleQuery = isAndroidModuleXpath.compile("boolean(//facet[@type='android'])");
                        Boolean isAndroidModule = (Boolean) isAndroidModuleQuery.evaluate(doc, XPathConstants.BOOLEAN);

                        if (isAndroidModule) {
                            //Add project level reference
                            VirtualFile ideaFolder = project.getProjectFile().getParent();

                            //Sets the module main source path
                            XPath xpathSources = xPathfactory.newXPath();
                            XPathExpression sourcesQuery = xpathSources.compile("//sourceFolder");
                            NodeList sources = ((org.w3c.dom.NodeList) sourcesQuery.evaluate(doc, XPathConstants.NODESET));

                            for (int i = 0; i < sources.getLength() && sourcePath == null; i++) {
                                String url = sources.item(i).getAttributes().getNamedItem("url").getNodeValue();
                                if (url.contains("src")) {
                                    sourcePath = url.replace("file://$MODULE_DIR$", moduleFile.getParent().getUrl());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Node addXMLElement(Node parent, String name, String attrName, String attrValue) throws XPathExpressionException {
        Document document = parent.getOwnerDocument();

        // there's no default implementation for NamespaceContext...seems kind of silly, no?
        NamespaceContext namespaceContext = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) throw new NullPointerException("Null prefix");
                else if ("android".equals(prefix))
                    return "http://schemas.android.com/apk/res/android";
                else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
                return XMLConstants.NULL_NS_URI;
            }

            // This method isn't necessary for XPath processing.
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            // This method isn't necessary for XPath processing either.
            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        };


        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath isAndroidModuleXpath = xPathfactory.newXPath();
        isAndroidModuleXpath.setNamespaceContext(namespaceContext);
        XPathExpression isAndroidModuleQuery = isAndroidModuleXpath.compile("boolean(//" + name + "[@" + attrName + "='" + attrValue + "'])");
        Boolean exists = (Boolean) isAndroidModuleQuery.evaluate(document, XPathConstants.BOOLEAN);

        if (exists)
            return null;

        Element element = document.createElement(name);
        element.setAttribute(attrName, attrValue);
        parent.appendChild(element);

        return element;
    }

    private Node addXMLElement(Node parent, String name, Map<String, String> attr) throws XPathExpressionException {
        Document document = parent.getOwnerDocument();
        Boolean exists = false;

        NamespaceContext namespaceContext = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) throw new NullPointerException("Null prefix");
                else if ("android".equals(prefix))
                    return "http://schemas.android.com/apk/res/android";
                else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
                return XMLConstants.NULL_NS_URI;
            }

            // This method isn't necessary for XPath processing.
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            // This method isn't necessary for XPath processing either.
            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        };


        Element element = document.createElement(name);

        for (String attrName : attr.keySet()) {
            String attrValue = attr.get(attrName);

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath isAndroidModuleXpath = xPathfactory.newXPath();
            isAndroidModuleXpath.setNamespaceContext(namespaceContext);
            XPathExpression isAndroidModuleQuery = isAndroidModuleXpath.compile("boolean(//" + name + "[@" + attrName + "='" + attrValue + "'])");
            exists = exists || (Boolean) isAndroidModuleQuery.evaluate(document, XPathConstants.BOOLEAN);

            element.setAttribute(attrName, attrValue);
        }

        if (exists)
            return null;

        parent.appendChild(element);
        return element;
    }

    private void saveUrl(String filename, String urlString)
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
            if (in != null)
                in.close();
            if (fout != null)
                fout.close();
        }
    }

    private byte[] getArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public static String getString(InputStream is) {
        //Using the trick described in this link to read whole streams in one operation:
        //http://stackoverflow.com/a/5445161
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @NotNull
    public static String getString(@NotNull InputStream is, @NotNull String charsetName) {
        //Using the trick described in this link to read whole streams in one operation:
        //http://stackoverflow.com/a/5445161
        Scanner s = new Scanner(is, charsetName).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}