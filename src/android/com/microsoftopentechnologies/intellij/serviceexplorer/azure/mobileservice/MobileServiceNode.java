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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
import com.microsoftopentechnologies.intellij.forms.JobForm;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.intellij.forms.ViewLogForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureManager;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.CustomAPI;
import com.microsoftopentechnologies.intellij.model.Job;
import com.microsoftopentechnologies.intellij.model.Service;
import com.microsoftopentechnologies.intellij.model.Table;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobileServiceNode extends Node {
    private static final String ICON_PATH = "service.png";
    public static final String TABLES = "Tables";
    public static final String CUSTOM_APIS = "Custom APIs";
    public static final String SCHEDULED_JOBS = "Scheduled Jobs";

    protected Service mobileService;
    protected boolean childNodesLoaded = false;

    protected Node tablesNode;      // the parent node for all table nodes
    protected Node customAPIsNode;  // the parent node for all custom api nodes
    protected Node jobsNode;        // the parent node for all scheduled job nodes

    public MobileServiceNode(Node parent, Service service) {
        super(service.getName(), service.getName(), parent, ICON_PATH, true, true);
        mobileService = service;
        loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        if (AzureRestAPIHelper.existsMobileService(mobileService.getName())) {
            try {
                AzureManager apiManager = AzureRestAPIManager.getManager();
                UUID subscriptionId = mobileService.getSubcriptionId();
                String serviceName = mobileService.getName();

                if (isNodeRuntime()) {
                    // load tables
                    tablesNode = loadServiceNode(
                            apiManager.getTableList(subscriptionId, serviceName),
                            "_tables",
                            TABLES,
                            tablesNode,
                            TableNode.class,
                            Table.class);

                    // load custom APIs
                    customAPIsNode = loadServiceNode(
                            apiManager.getAPIList(subscriptionId, serviceName),
                            "_apis",
                            CUSTOM_APIS,
                            customAPIsNode,
                            CustomAPINode.class,
                            CustomAPI.class);

                    // load scheduled jobs
                    jobsNode = loadServiceNode(
                            apiManager.listJobs(subscriptionId, serviceName),
                            "_jobs",
                            SCHEDULED_JOBS,
                            jobsNode,
                            ScheduledJobNode.class,
                            Job.class);
                }
            } catch (NoSuchMethodException e) {
                handleError(e);
            } catch (IllegalAccessException e) {
                handleError(e);
            } catch (InvocationTargetException e) {
                handleError(e);
            } catch (InstantiationException e) {
                handleError(e);
            }
        } else {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null,
                            "The mobile service " + mobileService.getName() +
                                    " could not be reached. Please try again after some time.",
                            "Microsoft Services Plugin",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void handleError(Exception e) {
        UIHelper.showException(
                "An error occurred while initializing the mobile service: " +
                        mobileService.getName(), e);
    }

    private <E, N> Node loadServiceNode(
            List<E> nodesList,
            String idSuffix,
            String displayName,
            Node parentNode,
            Class<N> nodeClass,
            Class<E> modelClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        // create and add a new parent node for this item; we add the "node"
        // variable as a child *before* adding the element nodes so that the
        // service explorer tool window is automatically notified when they are
        // added; if we called "addChildNode" after the children of "node"
        // have been added then the service explorer tool window will not be
        // notified of those new nodes
        if (parentNode == null) {
            parentNode = new Node(mobileService.getName() + idSuffix, displayName, this, null, false);
            addChildNode(parentNode);
        } else {
            // clear the parent node since we are re-initializing it
            parentNode.removeAllChildNodes();
        }

        // create child table nodes for this node
        Constructor<N> constructor = nodeClass.getConstructor(Node.class, modelClass);
        for (E nodeElement : nodesList) {
            parentNode.addChildNode((Node) constructor.newInstance(parentNode, nodeElement));
        }

        return parentNode;
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        if (isNodeRuntime()) {
            return ImmutableMap.of(
                    "Create table", CreateTableAction.class,
                    "Create API", CreateAPIAction.class,
                    "Create new job", CreateNewJobAction.class,
                    "Show log", ShowLogAction.class);
        } else {// register the sole edit table action
            addAction("Show log", new ShowLogAction());
            return null;
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent event) {
        // we attempt loading the services only if we haven't already
        // loaded them
        if (!childNodesLoaded) {
            Futures.addCallback(load(), new FutureCallback<List<Node>>() {
                @Override
                public void onSuccess(List<Node> nodes) {
                    childNodesLoaded = true;
                }

                @Override
                public void onFailure(Throwable throwable) {
                }
            });
        }
    }

    public Service getMobileService() {
        return mobileService;
    }

    public class CreateTableAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            TableForm form = new TableForm();
            form.setServiceName(mobileService.getName());
            form.setSubscriptionId(mobileService.getSubcriptionId());
            form.setProject(getProject());

            ArrayList<String> existingTables = new ArrayList<String>();
            for (Table table : mobileService.getTables())
                existingTables.add(table.getName());

            form.setExistingTableNames(existingTables);

            form.setAfterSave(new Runnable() {
                @Override
                public void run() {
                    // refresh the tables node
                    tablesNode.removeAllChildNodes();
                    try {
                        loadServiceNode(
                                AzureRestAPIManager.getManager().getTableList(
                                        mobileService.getSubcriptionId(),
                                        mobileService.getName()),
                                "_tables",
                                TABLES,
                                tablesNode,
                                TableNode.class,
                                Table.class);
                    } catch (NoSuchMethodException e1) {
                        handleError(e1);
                    } catch (IllegalAccessException e1) {
                        handleError(e1);
                    } catch (InvocationTargetException e1) {
                        handleError(e1);
                    } catch (InstantiationException e1) {
                        handleError(e1);
                    } catch (AzureCmdException e1) {
                        handleError(e1);
                    }
                }
            });

            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);
        }
    }

    public class CreateAPIAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            CustomAPIForm form = new CustomAPIForm();
            form.setServiceName(mobileService.getName());
            form.setSubscriptionId(mobileService.getSubcriptionId());
            form.setProject(getProject());

            form.setAfterSave(new Runnable() {
                @Override
                public void run() {
                    // refresh the apis node
                    customAPIsNode.removeAllChildNodes();
                    try {
                        loadServiceNode(
                                AzureRestAPIManager.getManager().getAPIList(
                                        mobileService.getSubcriptionId(),
                                        mobileService.getName()),
                                "_apis",
                                CUSTOM_APIS,
                                customAPIsNode,
                                CustomAPINode.class,
                                CustomAPI.class);
                    } catch (NoSuchMethodException e1) {
                        handleError(e1);
                    } catch (IllegalAccessException e1) {
                        handleError(e1);
                    } catch (InvocationTargetException e1) {
                        handleError(e1);
                    } catch (InstantiationException e1) {
                        handleError(e1);
                    } catch (AzureCmdException e1) {
                        handleError(e1);
                    }
                }
            });

            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);
        }
    }

    public class CreateNewJobAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            JobForm form = new JobForm();
            form.setServiceName(mobileService.getName());
            form.setSubscriptionId(mobileService.getSubcriptionId());
            form.setTitle("Create new Job");

            form.setAfterSave(new Runnable() {
                @Override
                public void run() {
                    // refresh the jobs node
                    jobsNode.removeAllChildNodes();
                    try {
                        loadServiceNode(
                                AzureRestAPIManager.getManager().listJobs(
                                        mobileService.getSubcriptionId(),
                                        mobileService.getName()),
                                "_jobs",
                                SCHEDULED_JOBS,
                                jobsNode,
                                ScheduledJobNode.class,
                                Job.class);
                    } catch (NoSuchMethodException e1) {
                        handleError(e1);
                    } catch (IllegalAccessException e1) {
                        handleError(e1);
                    } catch (InvocationTargetException e1) {
                        handleError(e1);
                    } catch (InstantiationException e1) {
                        handleError(e1);
                    } catch (AzureCmdException e1) {
                        handleError(e1);
                    }
                }
            });

            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);
        }
    }

    public class ShowLogAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            final ViewLogForm form = new ViewLogForm();

            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    form.queryLog(mobileService.getSubcriptionId(), mobileService.getName(), mobileService.getRuntime());
                }
            });

            UIHelper.packAndCenterJDialog(form);
            form.setVisible(true);
        }
    }

    private boolean isNodeRuntime() {
        return Service.NODE_RUNTIME.equals(mobileService.getRuntime());
    }
}
