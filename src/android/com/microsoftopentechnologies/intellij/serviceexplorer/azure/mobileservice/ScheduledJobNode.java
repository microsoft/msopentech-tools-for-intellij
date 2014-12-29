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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.forms.JobForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.Job;
import com.microsoftopentechnologies.intellij.model.Service;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Map;

public class ScheduledJobNode extends ScriptNodeBase {
    public static final String ICON_PATH = "job.png";
    protected Job job;

    public ScheduledJobNode(Node parent, Job job) {
        super(job.getName(), job.getName(), parent, ICON_PATH, false);
        this.job = job;
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        // register actions
        return ImmutableMap.of(
                "Update job", UpdateJobAction.class,
                "Edit job", EditJobAction.class);
    }

    @Override
    protected void onNodeClick(NodeActionEvent event) {
        onNodeClickInternal(job);
    }

    @Override
    protected void downloadScript(Service mobileService, String scriptName, String localFilePath) throws AzureCmdException {
        AzureRestAPIManager.getManager().downloadJobScript(
                mobileService.getSubcriptionId(),
                mobileService.getName(),
                scriptName,
                localFilePath);
    }

    public class EditJobAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            // get the parent MobileServiceNode node
            MobileServiceNode mobileServiceNode = (MobileServiceNode)findParentByType(MobileServiceNode.class);
            final Service mobileService = mobileServiceNode.getMobileService();

            final JobForm form = new JobForm();
            form.setJob(job);
            form.setServiceName(mobileService.getName());
            form.setTitle("Edit job");
            form.setSubscriptionId(mobileService.getSubcriptionId());
            form.setAfterSave(new Runnable() {
                @Override
                public void run() {
                    job = form.getEditingJob();
                }
            });
            form.pack();
            form.setVisible(true);
        }
    }

    public class UpdateJobAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            // get the parent MobileServiceNode node
            MobileServiceNode mobileServiceNode = (MobileServiceNode)findParentByType(MobileServiceNode.class);
            final Service mobileService = mobileServiceNode.getMobileService();

            VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(job.getLocalFilePath(mobileService.getName())));
            if (editorFile != null) {
                FileEditor[] fe = FileEditorManager.getInstance(getProject()).getAllEditors(editorFile);

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

                ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Uploading job script", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            progressIndicator.setIndeterminate(true);
                            AzureRestAPIManager.getManager().uploadJobScript(
                                    mobileService.getSubcriptionId(), mobileService.getName(),
                                    job.getName(), job.getLocalFilePath(mobileService.getName()));
                        } catch (AzureCmdException e) {
                            UIHelper.showException("Error uploading script", e);
                        }
                    }
                });
            }
        }
    }
}
