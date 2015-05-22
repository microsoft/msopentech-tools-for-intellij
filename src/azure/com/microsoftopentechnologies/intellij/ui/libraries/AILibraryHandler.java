/**
 * Copyright 2015 Microsoft Open Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.ui.libraries;

import com.microsoftopentechnologies.intellij.AzurePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class AILibraryHandler {
    Document webXMLDoc = null;
    String webXMLPath = "";

    Document aiConfXMLDoc = null;
    String aiConfXMLPath = "";

    private static final String INSTRUMENTATION_KEY_NODE_NAME = "InstrumentationKey";
    private static final String DISABLE_TELEMETRY_NODE_NAME = "DisableTelemetry";

    public AILibraryHandler() {
    }

    public Document getWebXMLDoc() {
        return webXMLDoc;
    }

    public Document getAIConfXMLDoc() {
        return aiConfXMLDoc;
    }

    public AILibraryHandler(String webXmlPath, String aiConfXMLPath)
            throws Exception {
        try {
            // parse web.xml
            if (webXmlPath != null) {
                parseWebXmlPath(webXmlPath);
            }

            // parse ApplicationInsights.xml
            if (aiConfXMLPath != null) {
                parseAIConfXmlPath(aiConfXMLPath);
            }
        } catch (Exception e) {
            AzurePlugin.log(e.getMessage(), e);
            throw new Exception(message("aiParseErrMsg"));
        }
    }

    public void parseWebXmlPath(String webXmlPath) throws Exception {
        if (webXmlPath != null) {
            this.webXMLPath = webXmlPath;
            File xmlFile = new File(webXmlPath);
            if (xmlFile.exists()) {
                webXMLDoc = ParserXMLUtility.parseXMLFile(webXmlPath, message("aiParseErrMsg"));
            } else {
                throw new Exception(String.format("%s%s", webXmlPath, message("fileErrMsg")));
            }
        }
    }

    public boolean isAIWebFilterConfigured() throws Exception {
        if (webXMLDoc == null) {
            return false;
        }

        String exprFilter = message("aiExprConst");
        XPath xpath = XPathFactory.newInstance().newXPath();
        Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc, XPathConstants.NODE);
        if (eleFilter != null) {
            return true;
        }
        return false;
    }

    public void save() throws IOException, Exception {
        if (webXMLDoc != null) {
            ParserXMLUtility.saveXMLFile(webXMLPath, webXMLDoc);
        }

        if (aiConfXMLDoc != null) {
            ParserXMLUtility.saveXMLFile(aiConfXMLPath, aiConfXMLDoc);
        }
    }

    public void parseAIConfXmlPath(String aiConfXMLPath) throws Exception {
        if (aiConfXMLPath != null) {
            this.aiConfXMLPath = aiConfXMLPath;
            File xmlFile = new File(aiConfXMLPath);
            if (xmlFile.exists()) {
                aiConfXMLDoc = ParserXMLUtility.parseXMLFile(aiConfXMLPath, message("aiParseErrMsg"));
            } else {
                throw new Exception(String.format("%s%s", aiConfXMLPath, message("fileErrMsg")));
            }
        }
    }

    /**
     * This method adds filter mapping tags in web.xml.
     */
    private void setFilterMapping() {
        if (webXMLDoc == null) {
            return;
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFltMapping = (Element) xpath.evaluate(message("exprFltMapping"), webXMLDoc, XPathConstants.NODE);
            if (eleFltMapping == null) {
                Element filterMapping = webXMLDoc.createElement(message("filterMapTag"));
                Element filterName = webXMLDoc.createElement(message("filterEle"));
                filterName.setTextContent(message("aiWebfilter"));
                filterMapping.appendChild(filterName);

                Element urlPattern = webXMLDoc.createElement(message("urlPatternTag"));
                urlPattern.setTextContent("/*");
                filterMapping.appendChild(urlPattern);

                NodeList existingFilterMapNodeList = webXMLDoc.getElementsByTagName(message("filterMapTag"));
                Node existingFilterMapNode = existingFilterMapNodeList != null
                        & existingFilterMapNodeList.getLength() > 0 ? existingFilterMapNodeList
                        .item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(filterMapping, existingFilterMapNode);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }

    }

    private void setFilterDef() {
        if (webXMLDoc == null) {
            return;
        }
        try {
            String exprFilter = message("exprConst");
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc,
                    XPathConstants.NODE);
            if (eleFilter == null) {
                Element filter = webXMLDoc.createElement(message("filterTag"));
                Element filterName = webXMLDoc.createElement(message("filterEle"));
                filterName.setTextContent(message("aiWebfilter"));
                filter.appendChild(filterName);

                Element fClass = webXMLDoc.createElement("filter-class");
                fClass.setTextContent(message("aiWebFilterClassName"));
                filter.appendChild(fClass);

                NodeList existingFilterNodeList = webXMLDoc.getElementsByTagName(message("filterTag"));
                Node existingFilterNode = existingFilterNodeList != null
                        & existingFilterNodeList.getLength() > 0 ? existingFilterNodeList
                        .item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(filter, existingFilterNode);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    public void removeAIFilterDef() throws Exception {
        if (webXMLDoc == null) {
            return;
        }
        try {
            String exprFilter = message("exprConst");
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc, XPathConstants.NODE);

            if (eleFilter != null) {
                eleFilter.getParentNode().removeChild(eleFilter);
            }

            String exprFltMapping = message("exprFltMapping");
            Element eleFilMapping = (Element) xpath.evaluate(exprFltMapping, webXMLDoc, XPathConstants.NODE);
            if (eleFilMapping != null) {
                eleFilMapping.getParentNode().removeChild(eleFilMapping);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            AzurePlugin.log(ex.getMessage(), ex);
            throw new Exception(String.format("%s%s", message("aiRemoveErr"), ex.getMessage()));
        }
    }

    public void disableAIFilterConfiguration(boolean disable) {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(DISABLE_TELEMETRY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                nodeList.item(0).setTextContent(disable+"");
            } else {
                Element disableTelemetry = aiConfXMLDoc.createElement(DISABLE_TELEMETRY_NODE_NAME);
                disableTelemetry.setTextContent(disable+"");
                aiConfXMLDoc.appendChild(disableTelemetry);
            }
        }
    }

    public void setAIFilterConfig() throws Exception {
        if (isAIWebFilterConfigured()) {
            return;
        }
        setFilterDef();
        setFilterMapping();
    }

    public void setAIInstrumentationKey(String instrumentationKey) {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(INSTRUMENTATION_KEY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                nodeList.item(0).setTextContent(instrumentationKey);
            } else {
                Element instrumentationKeyElement = aiConfXMLDoc.createElement(INSTRUMENTATION_KEY_NODE_NAME);
                instrumentationKeyElement.setTextContent(instrumentationKey);
                aiConfXMLDoc.appendChild(instrumentationKeyElement);
            }
        }
    }

    public String getAIInstrumentationKey() {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(INSTRUMENTATION_KEY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        }
        return "";
    }
}
