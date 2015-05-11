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

package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.CustomAPI;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Job;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Table;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobileServiceNode extends Node {
    private static final String ICON_PATH = "service.png";
    public static final String TABLES = "Tables";
    public static final String CUSTOM_APIS = "Custom APIs";
    public static final String SCHEDULED_JOBS = "Scheduled Jobs";

    protected MobileService mobileService;
    protected boolean childNodesLoaded = false;

    protected Node tablesNode;      // the parent node for all table nodes
    protected Node customAPIsNode;  // the parent node for all custom api nodes
    protected Node jobsNode;        // the parent node for all scheduled job nodes

    public MobileServiceNode(Node parent, MobileService mobileService) {
        super(mobileService.getName(), mobileService.getName(), parent, ICON_PATH, true, true);
        this.mobileService = mobileService;
        loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        if (AzureRestAPIHelper.existsMobileService(mobileService.getName())) {
            try {
                AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();
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
            DefaultLoader.getUIHelper().showError("The mobile service " + mobileService.getName() + " could not be reached. Please try again after some time.",
                    "Service Explorer");
        }
    }

    public void handleError(Exception e) {
        DefaultLoader.getUIHelper().showException(
                "An error occurred while initializing the mobile service: " +
                        mobileService.getName(), e);
    }

    public <E, N> Node loadServiceNode(
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
            super.initActions();
            return null;
        } else {// register the sole edit table action
            // todo
//            addAction("Show log", new ShowLogAction(this));
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

    public MobileService getMobileService() {
        return mobileService;
    }

    private boolean isNodeRuntime() {
        return MobileService.NODE_RUNTIME.equals(mobileService.getRuntime());
    }

    public static boolean isNodeRuntime(MobileService mobileService) {
        return MobileService.NODE_RUNTIME.equals(mobileService.getRuntime());
    }

    public Node getTablesNode() {
        return tablesNode;
    }

    public Node getCustomAPIsNode() {
        return customAPIsNode;
    }

    public Node getJobsNode() {
        return jobsNode;
    }
}
