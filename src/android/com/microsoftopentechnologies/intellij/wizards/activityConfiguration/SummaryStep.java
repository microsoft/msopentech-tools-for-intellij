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
package com.microsoftopentechnologies.intellij.wizards.activityConfiguration;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.directoryservices.Application;
import com.microsoft.services.odata.ODataException;
import com.microsoft.services.odata.interfaces.ODataResponse;
import com.microsoftopentechnologies.intellij.helpers.ServiceCodeReferenceHelper;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.o365.Office365Manager;
import com.microsoftopentechnologies.intellij.helpers.o365.Office365RestAPIManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class SummaryStep extends WizardStep<AddServiceWizardModel> {
    //private static final String PACKAGE_NAME = "com.microsoftopentechnologies.intellij";
    //private static final String OUTLOOK_SERVICES_ENDPOINT_URL = "https://outlook.com/ews/odata/";
    //private static final String FILE_SERVICES_ENDPOINT_URL = "https://mytenant.sharepoint.com/_api/v1.0";
    //private static final String LIST_SERVICES_ENDPOINT_URL = "https://mytenant.sharepoint.com/_api/v1.0";
    //private static final String LIST_SERVICES_SITE_URL = "/";

    private final AddServiceWizardModel model;
    private JPanel rootPanel;
    private JEditorPane editorSummary;

    public SummaryStep(final String title, AddServiceWizardModel model) {
        super(title, null, null);
        this.model = model;
        this.editorSummary.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        UIHelper.showException("Unable to follow link.", ex);
                    }
                }
            }
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        rootPanel.revalidate();

        StringBuilder summary = new StringBuilder();
        summary.append("<html> <head> </head> <body style=\"font-family: sans serif;\"> <p style=\"margin-top: 0\"><b>Summary:</b></p> <ol> ");


        if (this.model.getService() != null || this.model.getHubName() != null) {
            if (this.model.getService() != null) {
                summary.append("<li>Added <a href=\"https://go.microsoft.com/fwLink/?LinkID=280126&clcid=0x409\">Azure Mobile Services</a> library to project <b>");
                summary.append(this.model.getProject().getName());
                summary.append("</b>.</li> ");
                summary.append("<li>Added helper class using Mobile Service <b>");
                summary.append(this.model.getService().getName());
                summary.append("</b>.</li> ");
            }

            if (this.model.getHubName() != null) {
                summary.append("<li>Added <a href=\"https://go.microsoft.com/fwLink/?LinkID=280126&clcid=0x409\">Notification Hub</a> library to project <b>");
                summary.append(this.model.getProject().getName());
                summary.append("</b>.</li> ");
                summary.append("<li>Added helper class using Notification Hub <b>");
                summary.append(this.model.getHubName());
                summary.append("</b>.</li> ");
            }

            summary.append("<li>Added an Azure Services Activity referencing the mentioned helper classes.</li> ");
        } else if (this.model.isOutlookServices() || this.model.isFileServices() || this.model.isListServices()) {
            if (this.model.isOutlookServices()) {
                summary.append("<li>Added a reference to the Outlook Services library in project <b>");
                summary.append(this.model.getProject().getName());
                summary.append("</b>.</li> ");
                summary.append("<li>Added helper class OutlookServicesClient.</li> ");
            }

            if (this.model.isFileServices()) {
                summary.append("<li>Added a reference to the File Services library in project <b>");
                summary.append(this.model.getProject().getName());
                summary.append("</b>.</li> ");
                summary.append("<li>Added helper class FileServicesClient.</li> ");
            }

            if (this.model.isListServices()) {
                summary.append("<li>Added a reference to the SharePoint Lists library in project <b>");
                summary.append(this.model.getProject().getName());
                summary.append("</b>.</li> ");
                summary.append("<li>Added helper class ListServicesClient.</li> ");
            }

            summary.append("<li>Added an Office 365 Activity referencing the mentioned helper classes.</li> ");
            summary.append("<li>You can follow the link to <a href=\"https://github.com/OfficeDev/Office-365-SDK-for-Android\">Office 365 SDK for Android</a> to learn more about the referenced libraries.</li> ");
        }

        summary.append("</ol> <p style=\"margin-top: 0\">After clicking Finish, it might take a few seconds to complete set up.</p> </body> </html>");

        editorSummary.setText(summary.toString());

        return rootPanel;
    }

    @Override
    public boolean onFinish() {
        final SummaryStep summaryStep = this;

        ProgressManager.getInstance().run(new Task.Backgroundable(this.model.getProject(), "Setting up project for Microsoft services...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setFraction(0.1d);
                    double totalSteps = model.getServiceTypes().size();
                    double steps = 0d;

                    if (summaryStep.model.getService() != null) {
                        progressIndicator.setText("Setting up Azure Mobile Services");
                        summaryStep.associateMobileService();
                        steps++;
                        progressIndicator.setFraction(steps / totalSteps);
                    }

                    if (summaryStep.model.getHubName() != null) {
                        progressIndicator.setText("Setting up Azure Notification Hubs");
                        summaryStep.associateNotificationHub();
                        steps++;
                        progressIndicator.setFraction(steps / totalSteps);
                    }

                    if (summaryStep.model.isOutlookServices() || summaryStep.model.isFileServices() || summaryStep.model.isListServices()) {
                        progressIndicator.setText("Setting up Office 365 Services");
                        summaryStep.associateOffice365();
                        steps++;
                        progressIndicator.setFraction(steps / totalSteps);
                    }

                    // just for Azure Services. Office 365 already triggered gradle sync - avoid conflict
                    if (summaryStep.model.getService() != null || summaryStep.model.getHubName() != null) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //scheduled a gradle sync project action from the android/gradle plugin
                                    final ProjectDataManager projectDataManager = ServiceManager.getService(ProjectDataManager.class);
                                    ExternalSystemUtil.refreshProject(myProject,
                                            GradleConstants.SYSTEM_ID,
                                            myProject.getBaseDir().getCanonicalPath() != null ? myProject.getBaseDir().getCanonicalPath() : myProject.getBaseDir().getPath(),
                                            new ExternalProjectRefreshCallback() {
                                                @Override
                                                public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
                                                    if (externalProject == null) {
                                                        return;
                                                    }

                                                    ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(myProject) {
                                                        @Override
                                                        public void execute() {
                                                            ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    projectDataManager.importData(externalProject.getKey(), Collections.singleton(externalProject), myProject, true);
                                                                }
                                                            });
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
                                                }
                                            },
                                            false,
                                            ProgressExecutionMode.IN_BACKGROUND_ASYNC);
                                } catch (Throwable ex) {
                                    UIHelper.showException("Error invoking Gradle build", ex);
                                }
                            }
                        }, ModalityState.NON_MODAL);
                    }
                } catch (Throwable ex) {
                    UIHelper.showException("Error setting up Microsoft services", ex);
                }
            }
        });

        return super.onFinish();
    }

    private void associateMobileService() {
        final Project project = this.model.getProject();
        final Module module = this.model.getModule();
        final String activityName = this.model.getActivityName();
        final String appUrl = this.model.getService().getAppUrl();
        final String appKey = this.model.getService().getAppKey();

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ServiceCodeReferenceHelper scrh = new ServiceCodeReferenceHelper(project, module);
                            scrh.fillMobileServiceResource(activityName, appUrl, appKey);
                        } catch (Throwable ex) {
                            UIHelper.showException("Error creating Service helper", ex);
                        }
                    }
                });
            }
        }, ModalityState.NON_MODAL);
    }

    private void associateNotificationHub() {
        final Project project = this.model.getProject();
        final Module module = this.model.getModule();
        final String activityName = this.model.getActivityName();
        final String senderId = this.model.getSenderId();
        final String connStr = this.model.getConnectionString();
        final String hubName = this.model.getHubName();

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ServiceCodeReferenceHelper scrh = new ServiceCodeReferenceHelper(project, module);
                            scrh.addNotificationHubsLibs();
                            scrh.fillNotificationHubResource(activityName, senderId, connStr, hubName);
                        } catch (Throwable ex) {
                            UIHelper.showException("Error:", ex);
                        }
                    }
                });
            }
        }, ModalityState.NON_MODAL);
    }

    private void associateOffice365() {
        //final Project project = this.model.getProject();
        //final Module module = this.model.getModule();

        try {
            // update graph api
            final Office365Manager manager = Office365RestAPIManager.getManager();
            ListenableFuture<Application> future = manager.setO365PermissionsForApp(model.getOfficeApp(), model.getOfficePermissions());
            future.get();

            /*
            ServiceCodeReferenceHelper serviceCodeReferenceHelper = new ServiceCodeReferenceHelper(project, module);

            if (model.isOutlookServices()) {
                serviceCodeReferenceHelper.addOutlookServicesLibs();
                serviceCodeReferenceHelper.addOutlookServicesClass(PACKAGE_NAME, OUTLOOK_SERVICES_ENDPOINT_URL);
            }

            if (model.isFileServices()) {
                serviceCodeReferenceHelper.addFileServicesLibs();
                serviceCodeReferenceHelper.addFileServicesClass(PACKAGE_NAME, FILE_SERVICES_ENDPOINT_URL);
            }

            if (model.isListServices()) {
                serviceCodeReferenceHelper.addListServicesLibs();
                serviceCodeReferenceHelper.addListServicesClass(PACKAGE_NAME, LIST_SERVICES_ENDPOINT_URL, LIST_SERVICES_SITE_URL);
            }
            */
        } catch (ExecutionException ex) {
            String message = "";

            if (ex.getCause() instanceof ODataException) {
                message = parseODataException((ODataException) ex.getCause());
            }

            if (!message.isEmpty()) {
                UIHelper.showException("An error occurred while trying to associate the Office 365 application - " + message,
                        ex.getCause(),
                        "Error associating Office 365 application",
                        false);
            } else {
                UIHelper.showException("An error occurred while trying to associate the Office 365 application",
                        ex.getCause(),
                        "Error associating Office 365 application");
            }
        } catch (Throwable ex) {
            UIHelper.showException("An error occurred while trying to associate the Office 365 application",
                    ex,
                    "Error associating Office 365 application");
        }
    }

    @NotNull
    private static String parseODataException(@NotNull ODataException oDataException) {
        String result = "";

        ODataResponse oDataResponse = oDataException.getODataResponse();

        if (oDataResponse != null) {
            if (oDataResponse.getPayload() != null) {
                try {
                    String json = new String(oDataResponse.getPayload(), "UTF-8");

                    JsonObject errorJson = new JsonParser().parse(json).getAsJsonObject();
                    result = errorJson.get("odata.error").getAsJsonObject()
                            .get("message").getAsJsonObject()
                            .get("value").getAsString();
                } catch (Throwable ignored) {
                }
            } else if (oDataResponse.getResponse() != null) {
                int status = oDataResponse.getResponse().getStatus();
                switch (status) {
                    case 403:
                        result = "Authorization error - Status 403";
                        break;
                    default:
                        break;
                }
            }
        }

        return result;
    }
}