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

package com.microsoftopentechnologies.intellij.actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.intellij.helpers.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.model.MobileServiceTreeItem;
import com.microsoftopentechnologies.intellij.model.Table;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.UUID;


public class EditTableAction extends AnAction {
    public void update(AnActionEvent event) {
        PropertiesComponent pc = PropertiesComponent.getInstance(event.getProject());
        boolean enabled = Boolean.parseBoolean(pc.getValue("pluginenabled"));
        event.getPresentation().setEnabled(enabled);

        if(enabled) {
            MobileServiceTreeItem item = getSelectedScript(event);
            event.getPresentation().setVisible(item != null);
        }

    }

    private MobileServiceTreeItem getSelectedScript(AnActionEvent event) {
        JTree tree = UIHelper.getProjectTree();

        if(tree != null) {

            TreePath tp = tree.getLeadSelectionPath();
            if (tp != null && tp.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tp.getLastPathComponent();
                if (selectedNode != null) {
                    Object selectedObject = selectedNode.getUserObject();
                    if (selectedObject instanceof Table)
                        return (MobileServiceTreeItem) selectedObject;
                }
            }
        }
        return null;
    }

    public void actionPerformed(final AnActionEvent e) {
        PropertiesComponent pc = PropertiesComponent.getInstance(e.getProject());
        final String serviceName = pc.getValue("serviceName");
        final String subscriptionId = pc.getValue("subscriptionId");

        JTree tree = UIHelper.getProjectTree();

        TreePath tp = tree.getLeadSelectionPath();

        if(tp != null && tp.getLastPathComponent() instanceof DefaultMutableTreeNode){

            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tp.getLastPathComponent();
            final Object selectedObject = selectedNode.getUserObject();
            if(selectedObject instanceof Table) {

                ProgressManager.getInstance().run(new Task.Backgroundable(e.getProject(), "Loading table info", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            progressIndicator.setIndeterminate(true);

                            final Table selectedTable = AzureRestAPIManager.getManager().showTableDetails(UUID.fromString(subscriptionId), serviceName, ((Table) selectedObject).getName());

                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    TableForm form = new TableForm();

                                    form.setServiceName(serviceName);
                                    form.setSubscriptionId(UUID.fromString(subscriptionId));
                                    form.setEditingTable(selectedTable);

                                    form.setProject(e.getProject());
                                    UIHelper.packAndCenterJDialog(form);
                                    form.setVisible(true);
                                }
                            });
                        } catch (Throwable ex) {
                            UIHelper.showException("Error creating table", ex);
                        }

                    }
                });




            }
        }
    }
}
