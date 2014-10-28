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
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.AzureRestAPIHelper;
import com.microsoftopentechnologies.intellij.helpers.AzureTreeLoader;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.model.MobileServiceTreeItem;
import com.microsoftopentechnologies.intellij.model.Service;
import com.microsoftopentechnologies.intellij.model.Table;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.UUID;


public class RefreshAction extends AnAction {

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
                    if (selectedObject instanceof Table || selectedObject instanceof Service)
                        return (MobileServiceTreeItem) selectedObject;
                }
            }
        }

        return null;
    }

    public void actionPerformed(final AnActionEvent event) {



        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                Project project = event.getProject();
                refreshCurrentNode(project);
            }
        });
    }

    public static void refreshCurrentNode(final Project project) {
        final JTree tree = UIHelper.getProjectTree();

        TreePath tp = tree.getLeadSelectionPath();
        if (tp != null && tp.getLastPathComponent() instanceof DefaultMutableTreeNode) {

            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tp.getLastPathComponent();
            final MobileServiceTreeItem selectedObject = (MobileServiceTreeItem) selectedNode.getUserObject();

            if (!selectedObject.isLoading()) {
                PropertiesComponent pc = PropertiesComponent.getInstance(project);
                final String serviceName = pc.getValue("serviceName");
                final String subscriptionId = pc.getValue("subscriptionId");

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                selectedObject.setLoading(true);
                selectedNode.removeAllChildren();
                model.reload(selectedNode);

                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Mobile Services data...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {


                        if (AzureRestAPIHelper.existsMobileService(serviceName)) {

                            AzureTreeLoader atl = new AzureTreeLoader(UUID.fromString(subscriptionId), serviceName, project, tree, progressIndicator);

                            if (selectedObject instanceof Service) {
                                atl.serviceLoader(selectedNode);
                            } else if (selectedObject instanceof Table)
                                atl.tableLoader((Table) selectedObject, selectedNode);

                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {

                                    selectedObject.setLoading(false);
                                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                    model.reload(selectedNode);
                                }
                            });

                        } else {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    selectedObject.setLoading(false);
                                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                    model.reload(selectedNode);

                                    JOptionPane.showMessageDialog(tree,
                                            "The mobile service " +
                                              serviceName + " could not be reached. Please try again after some time.",
                                            "Microsoft Services Plugin",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
                    }
                });
            }
        }

    }

}
