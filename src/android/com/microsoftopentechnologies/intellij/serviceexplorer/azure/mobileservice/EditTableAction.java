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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Table;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.TableNode;

@Name("Edit table")
public class EditTableAction extends NodeActionListener {
    private TableNode tableNode;

    public EditTableAction(TableNode tableNode) {
        this.tableNode = tableNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = (MobileServiceNode) tableNode.findParentByType(MobileServiceNode.class);
        final MobileService mobileService = mobileServiceNode.getMobileService();

        runAsBackground("Editing table " + tableNode.getName() + "...", new Runnable() {
            @Override
            public void run() {
                Table selectedTable = null;
                try {
                    selectedTable = AzureManagerImpl.getManager().showTableDetails(
                            mobileService.getSubcriptionId(),
                            mobileService.getName(),
                            tableNode.getTable().getName());

                    TableForm form = new TableForm();
                    form.setServiceName(mobileService.getName());
                    form.setSubscriptionId(mobileService.getSubcriptionId());
                    form.setEditingTable(selectedTable);
                    form.setProject((Project) tableNode.getProject());
                    UIHelperImpl.packAndCenterJDialog(form);
                    form.setVisible(true);
                } catch (AzureCmdException e1) {
                    DefaultLoader.getUIHelper().showException("Error editing table", e1);
                }
            }
        });
    }

    public void runAsBackground(final String status, final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(
                        new Task.Backgroundable((Project) tableNode.getProject(), status, false) {
                            @Override
                            public void run(ProgressIndicator progressIndicator) {
                                runnable.run();
                            }
                        });
            }
        });
    }
}
