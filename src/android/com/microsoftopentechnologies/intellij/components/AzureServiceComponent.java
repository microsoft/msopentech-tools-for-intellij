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
package com.microsoftopentechnologies.intellij.components;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.*;
import com.microsoftopentechnologies.intellij.forms.OpenSSLFinderForm;
import com.microsoftopentechnologies.intellij.helpers.AndroidStudioHelper;
import com.microsoftopentechnologies.intellij.helpers.OpenSSLHelper;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.wizards.activityConfiguration.AddServiceWizard;
import org.jetbrains.annotations.NotNull;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;

public class AzureServiceComponent implements ProjectComponent {
    private Project mProject;
    private static String MOBILE_SERVICE_CODE = "//010fa0c4-5af1-4f81-95c1-720d9fab8d96";
    private static String NOTIFICATION_HUBS_CODE = "//46cca6b7-ff7d-4e05-9ef2-d7eb4798222e";
    private static String NOTIFICATION_HUBS_MOBILE_SERVICE_CODE = "//657555dc-6167-466a-9536-071307770d46";
    private static String OUTLOOK_SERVICES_CODE = "//fa684d69-70b3-41ec-83ff-2f8fa77aeeba";
    private static String FILE_SERVICES_CODE = "//1073bed4-78c3-4b4a-8a4d-ad874a286d86";
    private static String LIST_SERVICES_CODE = "//6695fd94-10cc-4274-b5df-46a3bc63a33d";
    private static String OUTLOOK_FILE_SERVICES_CODE = "//c4c2fd13-4abf-4785-a410-1887c5a1f1fc";
    private static String OUTLOOK_LIST_SERVICES_CODE = "//322e22fa-c249-4805-b057-c7b282acb605";
    private static String FILE_LIST_SERVICES_CODE = "//7193e8e2-dcec-4eb9-a3d6-02d86f88eaed";
    private static String OUTLOOK_FILE_LIST_SERVICES_CODE = "//25fdea0c-8a15-457f-9b15-dacb4e7dc2b2";
    private static VirtualFileListener vfl = getVirtualFileListener();
    private static Integer vflCount = 0;

    public AzureServiceComponent(Project project) {
        mProject = project;
    }

    public void initComponent() {
        try {
            ApplicationInfo.getInstance();

            final PropertiesComponent pc = PropertiesComponent.getInstance(mProject);

            if (OpenSSLHelper.existsOpenSSL()) {
                pc.setValue("pluginenabled", String.valueOf(true));
            } else {
                OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm();
                openSSLFinderForm.setModal(true);
                UIHelper.packAndCenterJDialog(openSSLFinderForm);
                openSSLFinderForm.setVisible(true);
                openSSLFinderForm.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent windowEvent) {
                        try {
                            pc.setValue("pluginenabled", String.valueOf(OpenSSLHelper.existsOpenSSL()));
                        } catch (AzureCmdException e) {
                            UIHelper.showException("Error initializing Microsoft Services plugin", e);
                        }
                    }
                });
            }


            synchronized (vflCount) {
                if (vflCount.equals(0)) {
                    VirtualFileManager.getInstance().addVirtualFileListener(vfl);
                }

                vflCount++;
            }
        } catch (AzureCmdException e) {
            UIHelper.showException("Error initializing Microsoft Services plugin", e);
        }
    }

    public void disposeComponent() {
        synchronized (vflCount) {
            vflCount--;

            if (vflCount.equals(0)) {
                VirtualFileManager.getInstance().removeVirtualFileListener(vfl);
            }
        }
    }

    @NotNull
    public String getComponentName() {
        return "AzureServiceComponent";
    }

    public void projectOpened() {
    }

    public void projectClosed() {
        UIHelper.setProjectTree(null);
    }

    private static VirtualFileListener getVirtualFileListener() {
        return new VirtualFileListener() {
            @Override
            public void propertyChanged(@NotNull VirtualFilePropertyEvent virtualFilePropertyEvent) {
            }

            @Override
            public void contentsChanged(@NotNull VirtualFileEvent virtualFileEvent) {
            }

            @Override
            public void fileCreated(@NotNull final VirtualFileEvent virtualFileEvent) {
            }

            @Override
            public void fileDeleted(@NotNull VirtualFileEvent virtualFileEvent) {
            }

            @Override
            public void fileMoved(@NotNull VirtualFileMoveEvent virtualFileMoveEvent) {
            }

            @Override
            public void fileCopied(@NotNull VirtualFileCopyEvent virtualFileCopyEvent) {
            }

            @Override
            public void beforePropertyChange(@NotNull VirtualFilePropertyEvent virtualFilePropertyEvent) {
            }

            @Override
            public void beforeContentsChange(@NotNull VirtualFileEvent virtualFileEvent) {
                if (virtualFileEvent.isFromSave()) {
                    final VirtualFile vf = virtualFileEvent.getFile();
                    Object requestor = virtualFileEvent.getRequestor();

                    if ("java".equals(vf.getExtension()) && (requestor instanceof FileDocumentManagerImpl)) {
                        FileDocumentManagerImpl fdm = (FileDocumentManagerImpl) requestor;
                        final Document document = fdm.getDocument(vf);

                        if (document != null) {
                            final int codeLineStart = document.getLineStartOffset(0);
                            int codeLineEnd = document.getLineEndOffset(0);
                            TextRange codeLineRange = new TextRange(codeLineStart, codeLineEnd);
                            String codeLine = document.getText(codeLineRange);
                            final boolean isMobileService = codeLine.equals(MOBILE_SERVICE_CODE) || codeLine.equals(NOTIFICATION_HUBS_MOBILE_SERVICE_CODE);
                            final boolean isNotificationHub = codeLine.equals(NOTIFICATION_HUBS_CODE) || codeLine.equals(NOTIFICATION_HUBS_MOBILE_SERVICE_CODE);
                            final boolean isOutlookServices = codeLine.equals(OUTLOOK_SERVICES_CODE) || codeLine.equals(OUTLOOK_FILE_SERVICES_CODE) || codeLine.equals(OUTLOOK_LIST_SERVICES_CODE) || codeLine.equals(OUTLOOK_FILE_LIST_SERVICES_CODE);
                            final boolean isFileServices = codeLine.equals(FILE_SERVICES_CODE) || codeLine.equals(OUTLOOK_FILE_SERVICES_CODE) || codeLine.equals(FILE_LIST_SERVICES_CODE) || codeLine.equals(OUTLOOK_FILE_LIST_SERVICES_CODE);
                            final boolean isListServices = codeLine.equals(LIST_SERVICES_CODE) || codeLine.equals(OUTLOOK_LIST_SERVICES_CODE) || codeLine.equals(FILE_LIST_SERVICES_CODE) || codeLine.equals(OUTLOOK_FILE_LIST_SERVICES_CODE);

                            if (isMobileService || isNotificationHub || isOutlookServices || isFileServices || isListServices) {
                                final int packageLineStart = document.getLineStartOffset(1);

                                CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        document.deleteString(codeLineStart, packageLineStart);
                                    }
                                });

                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        Collection<Project> projects = ProjectLocator.getInstance().getProjectsForFile(vf);
                                        if (projects.size() == 1) {
                                            AddServiceWizard.run((Project) projects.toArray()[0], ModuleUtil.findModuleForFile(vf, (Project) projects.toArray()[0]), isMobileService, isNotificationHub, isOutlookServices, isFileServices, isListServices);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void beforeFileDeletion(@NotNull VirtualFileEvent virtualFileEvent) {
            }

            @Override
            public void beforeFileMovement(@NotNull VirtualFileMoveEvent virtualFileMoveEvent) {
            }
        };
    }
}