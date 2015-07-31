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
package com.microsoftopentechnologies.intellij.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.collections.ListChangeListener;
import com.microsoftopentechnologies.tooling.msservices.helpers.collections.ListChangedEvent;
import com.microsoftopentechnologies.tooling.msservices.helpers.collections.ObservableList;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureServiceModule;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Collection;

public class ServerExplorerToolWindowFactory implements ToolWindowFactory, PropertyChangeListener {
    private JTree tree;
    private AzureServiceModule azureServiceModule;
    private DefaultTreeModel treeModel;

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        // initialize azure service module
        azureServiceModule = new AzureServiceModule(project);

        // initialize with all the service modules
        treeModel = new DefaultTreeModel(initRoot());

        // initialize tree
        tree = new Tree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // add a click handler for the tree
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                treeMousePressed(e);
            }
        });

        // add the tree to the window
        toolWindow.getComponent().add(new JBScrollPane(tree));

        // setup toolbar icons
        addToolbarItems(toolWindow);

        try {
            azureServiceModule.registerSubscriptionsChanged();
        } catch (AzureCmdException ignored) {
        }
    }

    private DefaultMutableTreeNode initRoot() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        // add the azure service root service module
        root.add(createTreeNode(azureServiceModule));

        // kick-off asynchronous load of child nodes on all the modules
        azureServiceModule.load();

        return root;
    }

    private void treeMousePressed(MouseEvent e) {
        // get the tree node associated with this mouse click
        TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
        if (treePath == null)
            return;

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        Node node = (Node) treeNode.getUserObject();

        // delegate click to the node's click action if this is a left button click
        if (SwingUtilities.isLeftMouseButton(e)) {
            // if the node in question is in a "loading" state then we
            // do not propagate the click event to it
            if (!node.isLoading()) {
                node.getClickAction().fireNodeActionEvent();
            }
        }
        // for right click show the context menu populated with all the
        // actions from the node
        else if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
            if (node.hasNodeActions()) {
                // select the node which was right-clicked
                tree.getSelectionModel().setSelectionPath(treePath);

                JPopupMenu menu = createPopupMenuForNode(node);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private JPopupMenu createPopupMenuForNode(Node node) {
        JPopupMenu menu = new JPopupMenu();

        for (final NodeAction nodeAction : node.getNodeActions()) {
            JMenuItem menuItem = new JMenuItem(nodeAction.getName());
            menuItem.setIconTextGap(16);
            menuItem.setEnabled(nodeAction.isEnabled());

            // delegate the menu item click to the node action's listeners
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    nodeAction.fireNodeActionEvent();
                }
            });

            menu.add(menuItem);
        }

        return menu;
    }

    private DefaultMutableTreeNode createTreeNode(Node node) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node, true);

        // associate the DefaultMutableTreeNode with the Node via it's "viewData"
        // property; this allows us to quickly retrieve the DefaultMutableTreeNode
        // object associated with a Node
        node.setViewData(treeNode);

        // listen for property change events on the node
        node.addPropertyChangeListener(this);

        // listen for structure changes on the node, i.e. when child nodes are
        // added or removed
        node.getChildNodes().addChangeListener(new NodeListChangeListener(treeNode));

        // create child tree nodes for each child node
        if (node.hasChildNodes()) {
            for (Node childNode : node.getChildNodes()) {
                treeNode.add(createTreeNode(childNode));
            }
        }

        return treeNode;
    }

    private void removeEventHandlers(Node node) {
        node.removePropertyChangeListener(this);

        ObservableList<Node> childNodes = node.getChildNodes();
        childNodes.removeAllChangeListeners();

        if (node.hasChildNodes()) {
            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire which should recursively
            // clean up event handlers on it's children
            node.removeAllChildNodes();
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        // if we are not running on the dispatch thread then switch
        // to dispatch thread
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    propertyChange(evt);
                }
            }, ModalityState.any());

            return;
        }

        // this event is fired whenever a property on a node in the
        // model changes; we respond by triggering a node change
        // event in the tree's model
        Node node = (Node) evt.getSource();

        // the treeModel object can be null before it is initialized
        // from createToolWindowContent; we ignore property change
        // notifications till we have a valid model object
        if (treeModel != null) {
            treeModel.nodeChanged((TreeNode) node.getViewData());
        }
    }

    private class NodeListChangeListener implements ListChangeListener {
        private DefaultMutableTreeNode treeNode;

        public NodeListChangeListener(DefaultMutableTreeNode treeNode) {
            this.treeNode = treeNode;
        }

        @Override
        public void listChanged(final ListChangedEvent e) {
            // if we are not running on the dispatch thread then switch
            // to dispatch thread
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        listChanged(e);
                    }
                }, ModalityState.any());

                return;
            }

            switch (e.getAction()) {
                case add:
                    // create child tree nodes for the new nodes
                    for (Node childNode : (Collection<Node>) e.getNewItems()) {
                        treeNode.add(createTreeNode(childNode));
                    }
                    break;
                case remove:
                    // unregister all event handlers recursively and remove
                    // child nodes from the tree
                    for (Node childNode : (Collection<Node>) e.getOldItems()) {
                        removeEventHandlers(childNode);

                        // remove this node from the tree
                        treeNode.remove((MutableTreeNode) childNode.getViewData());
                    }
                    break;
            }

            treeModel.reload(treeNode);
        }
    }

    private class NodeTreeCellRenderer extends NodeRenderer {
        @Override
        protected void doPaint(Graphics2D g) {
            super.doPaint(g);
            setOpaque(false);
        }

        @Override
        public void customizeCellRenderer(JTree jTree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean isLeaf,
                                          int row,
                                          boolean focused) {
            super.customizeCellRenderer(tree, value, selected, expanded, isLeaf, row, focused);

            // if the node has an icon set then we use that
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            Node node = (Node) treeNode.getUserObject();

            // "node" can be null if it's the root node which we keep hidden to simulate
            // a multi-root tree control
            if (node == null) {
                return;
            }

            String iconPath = node.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                setIcon(loadIcon(iconPath));
            }

            // setup a tooltip
            setToolTipText(node.getName());
        }

        private ImageIcon loadIcon(String iconPath) {
            URL url = NodeTreeCellRenderer.class.getResource("/com/microsoftopentechnologies/intellij/icons/" + iconPath);
            return new ImageIcon(url);
        }
    }

    private void addToolbarItems(ToolWindow toolWindow) {
        if (toolWindow instanceof ToolWindowEx) {
            ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;

            toolWindowEx.setTitleActions(
                    new AnAction("Refresh", "Refresh Service List", UIHelperImpl.loadIcon("refresh.png")) {
                        @Override
                        public void actionPerformed(AnActionEvent event) {
                            azureServiceModule.load();
                        }
                    },
                    new AnAction("Manage Subscriptions", "Manage Subscriptions", AllIcons.Ide.Link) {
                        @Override
                        public void actionPerformed(AnActionEvent anActionEvent) {
                            ManageSubscriptionForm form = new ManageSubscriptionForm(anActionEvent.getProject());
                            UIHelperImpl.packAndCenterJDialog(form);
                            form.setVisible(true);
                            //azureServiceModule.load();
                        }
                    });
        }
    }
}