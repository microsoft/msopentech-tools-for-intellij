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

package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.CustomAPI;
import com.microsoftopentechnologies.intellij.model.Service;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Map;
import java.util.UUID;

public class CustomAPINode extends ScriptNodeBase {
    public static final String ICON_PATH = "api.png";
    protected CustomAPI customAPI;

    public CustomAPINode(Node parent, CustomAPI customAPI) {
        super(customAPI.getName(), customAPI.getName(), parent, ICON_PATH, false);
        this.customAPI = customAPI;
    }

    @Override
    protected void refreshItems() {
        // TODO:
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        // register actions
        return ImmutableMap.of(
                "Update Custom API", UpdateCustomAPIAction.class,
                "Edit Custom API", EditCustomAPIAction.class);
    }

    @Override
    protected void onNodeClick(NodeActionEvent event) {
        onNodeClickInternal(customAPI);
    }

    @Override
    protected void downloadScript(Service mobileService, String scriptName, String localFilePath) throws AzureCmdException {
        AzureRestAPIManager.getManager().downloadAPIScript(
                mobileService.getSubcriptionId(),
                mobileService.getName(),
                scriptName,
                localFilePath);
    }

    public class UpdateCustomAPIAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            try {
                // get the parent MobileServiceNode node
                MobileServiceNode mobileServiceNode = (MobileServiceNode)findParentByType(MobileServiceNode.class);
                Service mobileService = mobileServiceNode.getMobileService();
                saveCustomAPI(getProject(), mobileService.getName(), mobileService.getSubcriptionId());
            } catch (AzureCmdException e1) {
                UIHelper.showException("Error uploading script:", e1);
            }
        }

        public void saveCustomAPI(Project project, final String serviceName, final UUID subscriptionId) throws AzureCmdException {
            VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(customAPI.getLocalFilePath(serviceName)));
            if (editorFile != null) {
                FileEditor[] fe = FileEditorManager.getInstance(project).getAllEditors(editorFile);

                if (fe.length > 0 && fe[0].isModified()) {
                    int i = JOptionPane.showConfirmDialog(null, "The file is modified. Do you want to save pending changes?", "Upload Script", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    switch (i) {
                        case JOptionPane.YES_OPTION:
                            ApplicationManager.getApplication().saveAll();
                            break;
                        case JOptionPane.CANCEL_OPTION:
                        case JOptionPane.CLOSED_OPTION:
                            return;
                    }
                }

                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading custom api script", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            progressIndicator.setIndeterminate(true);
                            AzureRestAPIManager.getManager().uploadAPIScript(subscriptionId, serviceName, customAPI.getName(), customAPI.getLocalFilePath(serviceName));
                        } catch (AzureCmdException e) {
                            UIHelper.showException("Error uploading script", e);
                        }
                    }
                });
            }
        }
    }

    public class EditCustomAPIAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            // get the parent MobileServiceNode node
            MobileServiceNode mobileServiceNode = (MobileServiceNode)findParentByType(MobileServiceNode.class);
            Service mobileService = mobileServiceNode.getMobileService();

            final CustomAPIForm form = new CustomAPIForm();
            form.setEditingCustomAPI(customAPI);
            form.setServiceName(mobileService.getName());

            form.setSubscriptionId(mobileService.getSubcriptionId());
            form.setProject(getProject());
            form.setAfterSave(new Runnable() {
                @Override
                public void run() {
                    customAPI = form.getEditingCustomAPI();
                }
            });
            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);
        }
    }
}
