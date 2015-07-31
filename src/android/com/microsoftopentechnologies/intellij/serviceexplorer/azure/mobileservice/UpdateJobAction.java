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
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.ScheduledJobNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

@Name("Update job")
public class UpdateJobAction extends NodeActionListener {
    private ScheduledJobNode scheduledJobNode;

    public UpdateJobAction(ScheduledJobNode scheduledJobNode) {
        this.scheduledJobNode = scheduledJobNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = scheduledJobNode.findParentByType(MobileServiceNode.class);
        final MobileService mobileService = mobileServiceNode.getMobileService();

        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(scheduledJobNode.getJob().getLocalFilePath(mobileService.getName())));

        if (editorFile != null) {
            FileEditor[] fe = FileEditorManager.getInstance((Project) scheduledJobNode.getProject()).getAllEditors(editorFile);

            if (fe.length > 0 && fe[0].isModified()) {
                int i = JOptionPane.showConfirmDialog(null, "The file is modified. Do you want to save pending changes?", "Service Explorer", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

                switch (i) {
                    case JOptionPane.YES_OPTION:
                        ApplicationManager.getApplication().saveAll();
                        break;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;
                }
            }

            ProgressManager.getInstance().run(new Task.Backgroundable((Project) scheduledJobNode.getProject(), "Uploading job script", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        AzureManagerImpl.getManager().uploadJobScript(
                                mobileService.getSubcriptionId(), mobileService.getName(),
                                scheduledJobNode.getJob().getName(), scheduledJobNode.getJob().getLocalFilePath(mobileService.getName()));
                    } catch (AzureCmdException e) {
                        DefaultLoader.getUIHelper().showException("Error uploading script", e);
                    }
                }
            });
        }
    }
}