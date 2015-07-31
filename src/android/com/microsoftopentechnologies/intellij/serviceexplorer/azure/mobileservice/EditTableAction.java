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
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Table;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureNodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.TableNode;

@Name("Edit table")
public class EditTableAction extends AzureNodeActionListener {
    private TableNode tableNode;

    public EditTableAction(TableNode tableNode) {
        super(tableNode, "Retrieving Table Information");
        this.tableNode = tableNode;
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
            throws AzureCmdException {
        try {
            // get the parent MobileServiceNode node
            final MobileService mobileService = tableNode.findParentByType(MobileServiceNode.class).getMobileService();

            final Table selectedTable = AzureManagerImpl.getManager().showTableDetails(
                    mobileService.getSubcriptionId(),
                    mobileService.getName(),
                    tableNode.getTable().getName());

            if (stateHandle.isEventTriggered()) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    TableForm form = new TableForm();
                    form.setServiceName(mobileService.getName());
                    form.setSubscriptionId(mobileService.getSubcriptionId());
                    form.setEditingTable(selectedTable);
                    form.setProject((Project) tableNode.getProject());
                    UIHelperImpl.packAndCenterJDialog(form);
                    form.setVisible(true);
                }
            }, ModalityState.any());
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException("Error editing table", e1);
        }
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e)
            throws AzureCmdException {
    }
}