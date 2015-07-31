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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.CustomAPINode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;

import javax.swing.*;
import java.io.File;

@Name("Update Custom API")
public class UpdateCustomAPIAction extends NodeActionListener {
    private CustomAPINode customAPINode;

    public UpdateCustomAPIAction(CustomAPINode customAPINode) {
        this.customAPINode = customAPINode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        try {
            // get the parent MobileServiceNode node
            MobileServiceNode mobileServiceNode = customAPINode.findParentByType(MobileServiceNode.class);
            MobileService mobileService = mobileServiceNode.getMobileService();
            saveCustomAPI((Project) customAPINode.getProject(), mobileService.getName(), mobileService.getSubcriptionId());
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException("Error uploading script:", e1);
        }
    }

    public void saveCustomAPI(Project project, final String serviceName, final String subscriptionId) throws AzureCmdException {
        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(customAPINode.getCustomAPI().getLocalFilePath(serviceName)));
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
                        AzureManagerImpl.getManager().uploadAPIScript(subscriptionId, serviceName, customAPINode.getCustomAPI().getName(),
                                customAPINode.getCustomAPI().getLocalFilePath(serviceName));
                    } catch (AzureCmdException e) {
                        DefaultLoader.getUIHelper().showException("Error uploading script", e);
                    }
                }
            });
        }
    }
}
