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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.model.MobileServiceScriptTreeItem;
import com.microsoftopentechnologies.intellij.model.Script;
import com.microsoftopentechnologies.intellij.model.Service;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class ScriptNodeBase extends Node {
    public ScriptNodeBase(String id, String name, Node parent, String iconPath, boolean hasRefreshAction) {
        super(id, name, parent, iconPath, hasRefreshAction);
    }

    protected void downloadScript(Service mobileService, String scriptName, String localFilePath) throws AzureCmdException {}

    protected void onNodeClickInternal(final MobileServiceScriptTreeItem script) {
        // TODO: This function is far too long and confusing. Refactor this to smaller well-defined sub-routines.

        // find the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = (MobileServiceNode)findParentByType(MobileServiceNode.class);
        final Service mobileService = mobileServiceNode.getMobileService();

        VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByIoFile(new File(script.getLocalFilePath(mobileService.getName())));
        boolean fileIsEditing = false;

        if (scriptFile != null)
            fileIsEditing = FileEditorManager.getInstance(getProject()).getEditors(scriptFile).length != 0;

        if (!fileIsEditing) {
            try {
                File temppath = new File(script.getLocalDirPath(mobileService.getName()));
                temppath.mkdirs();

                if (script instanceof Script && ((Script) script).getSelfLink() == null) {
                    InputStream is = this.getClass().getResourceAsStream(
                            String.format("/com/microsoftopentechnologies/intellij/templates/%s.js",
                                    ((Script) script).getOperation()));
                    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

                    int b;
                    while ((b = is.read()) != -1)
                        buff.write(b);

                    final File tempf = new File(temppath, ((Script) script).getOperation() + ".js");
                    tempf.createNewFile();

                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                final VirtualFile editfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempf);
                                if (editfile != null) {
                                    editfile.setWritable(true);

                                    editfile.setBinaryContent(buff.toByteArray());

                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            FileEditorManager.getInstance(getProject()).openFile(editfile, true);
                                        }
                                    });
                                }
                            } catch (Throwable e) {
                                UIHelper.showException("Error writing temporal editable file:", e);
                            }
                        }
                    });
                } else {
                    boolean download = false;
                    final File file = new File(script.getLocalFilePath(mobileService.getName()));
                    if (file.exists()) {
                        String[] options = new String[]{"Use remote", "Use local"};
                        int optionDialog = JOptionPane.showOptionDialog(null,
                                "There is a local copy of the script. Do you want you replace it with the remote version?",
                                "Edit script",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[1]);

                        if (optionDialog == JOptionPane.YES_OPTION) {
                            download = true;
                        }
                    } else {
                        download = true;
                    }

                    if (download) {
                        ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Loading Mobile Services data...", false) {
                            @Override
                            public void run(@NotNull ProgressIndicator progressIndicator) {
                                progressIndicator.setIndeterminate(true);
                                progressIndicator.setText("Downloading script");
                                try {
                                    downloadScript(mobileService, script.getName(), script.getLocalFilePath(mobileService.getName()));
                                    final VirtualFile finalEditfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            openFile(finalEditfile, script);
                                        }
                                    });
                                } catch (Throwable e) {
                                    UIHelper.showException("Error writing temporal editable file:", e);
                                }
                            }
                        });
                    } else {
                        final VirtualFile finalEditfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                        ApplicationManager.getApplication().runReadAction(new Runnable() {
                            @Override
                            public void run() {
                                openFile(finalEditfile, script);
                            }
                        });
                    }
                }
            }
            catch (Throwable e) {
                UIHelper.showException("Error writing temporal editable file:", e);
            }
        }
    }

    private void openFile(final VirtualFile finalEditfile, MobileServiceScriptTreeItem script) {
        try {
            if (finalEditfile != null) {
                finalEditfile.setWritable(true);

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        FileEditorManager.getInstance(getProject()).openFile(finalEditfile, true);
                    }
                });
            }
        } catch (Throwable e) {
            UIHelper.showException("Error writing temporal editable file:", e);
        }
    }
}
