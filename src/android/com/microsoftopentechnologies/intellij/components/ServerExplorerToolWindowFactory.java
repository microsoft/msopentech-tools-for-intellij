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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoftopentechnologies.intellij.forms.*;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureTreeLoader;
import com.microsoftopentechnologies.intellij.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ServerExplorerToolWindowFactory implements ToolWindowFactory {
    private boolean isRefreshEnabled;
    private JPanel treePanel;
    private JLabel loading;

    @Override
    public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
        isRefreshEnabled = true;

        treePanel = new JPanel(new GridBagLayout());
        loading = new JLabel("Loading services...");

        final JComponent toolWindowComponent = toolWindow.getComponent();

        final Tree tree = new Tree();

        tree.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                int selRow = tree.getRowForLocation(mouseEvent.getX(), mouseEvent.getY());
                TreePath selPath = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());

                if (selPath != null && selRow != -1) {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    if (selectedNode != null) {

                        AzureTreeLoader azureTreeLoader = new AzureTreeLoader(project, tree);

                        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                            azureTreeLoader.treeClick(selectedNode);
                        }


                        if (SwingUtilities.isRightMouseButton(mouseEvent) || mouseEvent.isPopupTrigger()) {
                            if (selectedNode.getUserObject() instanceof Subscription) {

                                JBPopupMenu menu = new JBPopupMenu();
                                JMenuItem mi = new JMenuItem("Create Service");
                                mi.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent actionEvent) {
                                        CreateNewServiceForm form = new CreateNewServiceForm();
                                        form.setServiceCreated(new Runnable() {
                                            @Override
                                            public void run() {
                                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        loadTree(project, tree);
                                                    }
                                                });
                                            }
                                        });

                                        form.setModal(true);
                                        UIHelper.packAndCenterJDialog(form);
                                        form.setVisible(true);
                                    }
                                });
                                mi.setIconTextGap(16);
                                menu.add(mi);

                                menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());

                            } else if (selectedNode.getUserObject() instanceof ServiceTreeItem) {
                                ServiceTreeItem selectedObject = (ServiceTreeItem) selectedNode.getUserObject();

                                JBPopupMenu menu = new JBPopupMenu();
                                JBMenuItem[] menuItems = azureTreeLoader.getMenuItems(project, selectedObject, selectedNode, tree);
                                if(menuItems != null) {
                                    for (JBMenuItem mi :menuItems) {
                                        mi.setIconTextGap(16);
                                        menu.add(mi);
                                    }

                                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                                }
                            }
                        }


                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        });

        tree.setCellRenderer(AzureTreeLoader.getTreeNodeRenderer());
        tree.setRootVisible(false);

        if (toolWindow instanceof ToolWindowEx) {
            ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;

            toolWindowEx.setTitleActions(
                    new AnAction("Refresh", "Refresh Service List", UIHelper.loadIcon("refresh.png")) {
                        @Override
                        public void actionPerformed(AnActionEvent event) {
                            loadTree(project, tree);
                        }
                    },
                    new AnAction("Manage Subscriptions", "Manage Subscriptions", AllIcons.Ide.Link) {
                        @Override
                        public void actionPerformed(AnActionEvent anActionEvent) {
                            ManageSubscriptionForm form = new ManageSubscriptionForm(anActionEvent.getProject());
                            UIHelper.packAndCenterJDialog(form);
                            form.setVisible(true);
                            loadTree(project, tree);
                        }
                    });

        }

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;

        treePanel.add(tree, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        treePanel.add(loading, c);

        tree.setVisible(false);
        loading.setVisible(false);

        toolWindowComponent.add(new JBScrollPane(treePanel));

        loadTree(project, tree);
    }

    private void loadTree(Project project, final JTree tree) {
        final ServerExplorerToolWindowFactory toolWindowFactory = this;

        if (isRefreshEnabled) {
            toolWindowFactory.isRefreshEnabled = false;

            final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            root.removeAllChildren();
            model.reload(root);

            tree.setVisible(false);
            loading.setVisible(true);

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Subscriptions...", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);

                        final ArrayList<Subscription> subscriptionList = AzureRestAPIManager.getManager().getSubscriptionList();

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (Subscription subscription : subscriptionList) {

                                    DefaultMutableTreeNode subscriptionNode = new DefaultMutableTreeNode(subscription.getName());
                                    subscriptionNode.setUserObject(subscription);
                                    root.add(subscriptionNode);
                                    model.reload(root);

                                    for (AzureType type : AzureType.values()) {
                                        DefaultMutableTreeNode serviceTree = new DefaultMutableTreeNode(type.toString());
                                        serviceTree.setUserObject(type);
                                        subscriptionNode.add(serviceTree);
                                        model.reload(subscriptionNode);
                                    }

                                }
                                toolWindowFactory.isRefreshEnabled = true;
                                loading.setVisible(false);
                                tree.setVisible(true);
                            }
                        });

                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error querying mobile services data", e);
                    }
                }
            });

        }
    }


}
