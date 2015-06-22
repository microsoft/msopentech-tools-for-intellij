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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Column;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Script;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Table;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public class TableNode extends AzureRefreshableNode {
    public static final String ICON_PATH = "table.png";
    public static final String SCRIPTS = "Scripts";
    public static final String COLUMNS = "Columns";
    protected Table table;
    protected boolean childNodesLoaded = false;

    protected Node scriptsNode; // parent node for all script nodes
    protected Node columnsNode; // parent node for all column nodes

    public TableNode(Node parent, Table table) {
        super(table.getName(), table.getName(), parent, ICON_PATH);
        this.table = table;
    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = findParentByType(MobileServiceNode.class);
        MobileService mobileService = mobileServiceNode.getMobileService();

        // fetch table details
        Table tableInfo = AzureManagerImpl.getManager().showTableDetails(
                mobileService.getSubcriptionId(),
                mobileService.getName(),
                table.getName());

        if (eventState.isEventTriggered()) {
            return;
        }

        // load scripts and columns nodes
        scriptsNode = loadScriptNode(tableInfo);
        columnsNode = loadColumnNode(tableInfo);
    }

    protected Node loadScriptNode(Table tableInfo) {
        // create and add a new parent node for this item; we add the "node"
        // variable as a child *before* adding the element nodes so that the
        // service explorer tool window is automatically notified when they are
        // added; if we called "addChildNode" after the children of "node"
        // have been added then the service explorer tool window will not be
        // notified of those new nodes
        if (scriptsNode == null) {
            scriptsNode = new Node(table.getName() + "_script", SCRIPTS, this, null);
            addChildNode(scriptsNode);
        } else {
            scriptsNode.removeAllChildNodes();
        }

        for (String operation : Script.getOperationList()) {
            Script s = new Script();
            s.setOperation(operation);
            s.setBytes(0);
            s.setName(String.format("%s.%s", tableInfo.getName(), operation));

            for (Script script : tableInfo.getScripts()) {
                if (script.getOperation().equals(operation)) {
                    s = script;
                }
            }

            scriptsNode.addChildNode(new TableScriptNode(scriptsNode, s));
        }

        return scriptsNode;
    }

    protected Node loadColumnNode(Table tableInfo) {
        // create and add a new parent node for this item; we add the "node"
        // variable as a child *before* adding the element nodes so that the
        // service explorer tool window is automatically notified when they are
        // added; if we called "addChildNode" after the children of "node"
        // have been added then the service explorer tool window will not be
        // notified of those new nodes
        if (columnsNode == null) {
            columnsNode = new Node(table.getName() + "_column", COLUMNS, this, null);
            addChildNode(columnsNode);
        } else {
            columnsNode.removeAllChildNodes();
        }

        for (Column col : tableInfo.getColumns()) {
            if (!col.getName().startsWith("__")) {
                columnsNode.addChildNode(new TableColumnNode(columnsNode, col));
            }
        }

        return columnsNode;
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

    public Table getTable() {
        return table;
    }
}