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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.AzureAbstractPanel;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsPanel implements AzureAbstractPanel {
    private JPanel rootPanel;
    private JCheckBox aiCheck;
    private JTextField txtInstrumentationKey;
    private JXHyperlink lnkInstrumentationKey;
    private JXHyperlink lnkAIPrivacy;
    private JLabel lblInstrumentationKey;

    private AILibraryHandler handler;
    private Module module;

    private String webxmlPath = message("xmlPath");

    public ApplicationInsightsPanel(Module module) {
        this.module = module;
        handler = new AILibraryHandler();
        init();
    }

    private void init() {
        initLink(lnkInstrumentationKey, message("lnkInstrumentationKey"), message("instrumentationKey"));
        initLink(lnkAIPrivacy, message("lnkAIPrivacy"), message("AIPrivacy"));
        try {
            String webXmlFilePath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, webxmlPath);
            if (new File(webXmlFilePath).exists()) {
                handler.parseWebXmlPath(webXmlFilePath);
            }
            String aiXMLFilePath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
            if (new File(aiXMLFilePath).exists()) {
                handler.parseAIConfXmlPath(aiXMLFilePath);
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("aiParseError"));
        }
        if (isEdit()) {
            populateData();
        } else {
            txtInstrumentationKey.setEnabled(false);
            lblInstrumentationKey.setEnabled(false);
        }
        aiCheck.addActionListener(createAiCheckListener());
    }

    private ActionListener createAiCheckListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aiCheck.isSelected()) {
                    txtInstrumentationKey.setEnabled(true);
                    lblInstrumentationKey.setEnabled(true);
                    txtInstrumentationKey.setText(handler.getAIInstrumentationKey() != null ? handler.getAIInstrumentationKey() : "");
                } else {
                    txtInstrumentationKey.setText("");
                    txtInstrumentationKey.setEnabled(false);
                    lblInstrumentationKey.setEnabled(false);
                }
            }
        };
    }

    private void initLink(JXHyperlink link, String linkText, String linkName) {
        link.setURI(URI.create(linkText));
        link.setText(linkName);
    }

    @Override
    public JComponent getPanel() {
        return rootPanel;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public boolean doOKAction() {
        // validate
        if (aiCheck.isSelected() && (txtInstrumentationKey.getText() == null || txtInstrumentationKey.getText().trim().length() == 0)) {
            PluginUtil.displayErrorDialog(message("aiErrTitle"), message("aiInstrumentationKeyNull"));
            return false;
        } else if (!aiCheck.isSelected()) {
            // disable if exists
            try {
                handler.disableAIFilterConfiguration(true);
                handler.removeAIFilterDef();
                handler.save();
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("aiErrTitle"), message("aiConfigRemoveError") + e.getLocalizedMessage());
                return false;
            }
        } else {
            try {
                createAIConfiguration();
                configureAzureSDK();
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("aiErrTitle"), message("aiConfigError") + e.getLocalizedMessage());
                return false;
            }
        }
        LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        return true;
    }

    private void populateData() {
        aiCheck.setSelected(true);
        txtInstrumentationKey.setText(handler.getAIInstrumentationKey());
        txtInstrumentationKey.setEnabled(true);
    }

    private boolean isEdit() {
        try {
            return handler.isAIWebFilterConfigured();
        } catch (Exception e) {
            // just return false if there is any exception
            return false;
        }
    }

    private void createAIConfiguration() throws Exception {
        handleWebXML();
        handleAppInsightsXML();
        handler.save();
    }

    private void handleWebXML() throws Exception {
        String xmlPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, webxmlPath);
        if (new File(xmlPath).exists()) {
            handler.parseWebXmlPath(xmlPath);
            handler.setAIFilterConfig();
        } else { // create web.xml
            int choice = Messages.showYesNoDialog(message("depDescMsg"), message("depDescTtl"), Messages.getQuestionIcon());
            if (choice == Messages.YES) {
                String path = createFileIfNotExists(message("depFileName"), message("depDirLoc"), message("aiWebXmlResFileLoc"));
                handler.parseWebXmlPath(path);
            } else {
                throw new Exception(": Application Insights cannot be configured without creating web.xml ");
            }
        }
    }

    private void handleAppInsightsXML() throws Exception {
        String aiXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
        if (new File(aiXMLPath).exists()) {
            handler.parseAIConfXmlPath(aiXMLPath);
            handler.disableAIFilterConfiguration(false);
        } else { // create ApplicationInsights.xml
            String path = createFileIfNotExists(message("aiConfFileName"), message("aiConfRelDirLoc"), message("aiConfResFileLoc"));
            handler.parseAIConfXmlPath(path);
        }

        if (txtInstrumentationKey.getText() != null && txtInstrumentationKey.getText().length() > 0) {
            handler.setAIInstrumentationKey(txtInstrumentationKey.getText().trim());
        }
    }

    private void configureAzureSDK() {
        final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
            if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                    && AzureLibrary.AZURE_LIBRARIES.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                return;
            }
        }

        final LibrariesContainer.LibraryLevel level = LibrariesContainer.LibraryLevel.MODULE;
        AccessToken token = WriteAction.start();
        try {
            Library newLibrary = LibrariesContainerFactory.createContainer(modifiableModel).createLibrary(AzureLibrary.AZURE_LIBRARIES.getName(), level, new ArrayList<OrderRoot>());
            for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
                if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                        && AzureLibrary.AZURE_LIBRARIES.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                    ((ModuleLibraryOrderEntryImpl) orderEntry).setExported(true);
                    break;
                }
            }
            Library.ModifiableModel newLibraryModel = newLibrary.getModifiableModel();
            File file = new File(String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, AzureLibrary.AZURE_LIBRARIES.getLocation()));
            AddLibraryUtility.addLibraryRoot(file, newLibraryModel);
            AddLibraryUtility.addLibraryFiles(new File(String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, "lib")), newLibraryModel,
                    AzureLibrary.AZURE_LIBRARIES.getFiles());
            newLibraryModel.commit();
            modifiableModel.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            token.finish();
        }
    }

    public String createFileIfNotExists(String fileName, String relDirLocation, String resFileLoc)  {
        String path = null;
        try {
            File cmpntFileLoc = new File(String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, relDirLocation));
            String cmpntFile = String.format("%s%s%s", cmpntFileLoc, File.separator, fileName);
            if (!cmpntFileLoc.exists()) {
                cmpntFileLoc.mkdirs();
            }
            AzurePlugin.copyResourceFile(resFileLoc, cmpntFile);
            path = cmpntFile;
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("acsErrTtl"), message("fileCrtErrMsg"), e);
        }
        return new File(path).getPath();
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }
}
