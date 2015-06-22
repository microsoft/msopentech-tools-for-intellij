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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.collections.ObservableList;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Node {
    private static final String CLICK_ACTION = "click";
    protected String id;
    protected String name;
    protected Node parent;
    protected ObservableList<Node> childNodes = new ObservableList<Node>();
    protected String iconPath;
    protected Object viewData;
    protected NodeAction clickAction = new NodeAction(this, CLICK_ACTION);
    protected List<NodeAction> nodeActions = new ArrayList<NodeAction>();

    // marks this node as being in a "loading" state; when this field is true
    // the following consequences apply:
    //  [1] all actions associated with this node get disabled
    //  [2] click action gets disabled automatically
    protected boolean loading = false;

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Node(String id, String name) {
        this(id, name, null, null, false);
    }

    public Node(String id, String name, Node parent, String iconPath) {
        this(id, name, parent, iconPath, false);
    }

    public Node(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        this.iconPath = iconPath;

        if (!delayActionLoading) {
            loadActions();
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        propertyChangeSupport.firePropertyChange("name", oldValue, name);
    }

    public Node getParent() {
        return parent;
    }

    public ObservableList<Node> getChildNodes() {
        return childNodes;
    }

    public boolean isDirectChild(Node node) {
        return childNodes.contains(node);
    }

    public boolean isDescendant(Node node) {
        if (isDirectChild(node))
            return true;
        for (Node child : childNodes) {
            if (child.isDescendant(node))
                return true;
        }

        return false;
    }

    // Walk up the tree till we find a parent node who's type
    // is equal to "clazz".
    public <T extends Node> T findParentByType(Class<T> clazz) {
        if (parent == null)
            return null;
        if (parent.getClass().equals(clazz))
            return (T) parent;
        return parent.findParentByType(clazz);
    }

    public boolean hasChildNodes() {
        return !childNodes.isEmpty();
    }

    public void removeDirectChildNode(Node childNode) {
        if (isDirectChild(childNode)) {
            // remove this node's child nodes (so they get an
            // opportunity to clean up after them)
            childNode.removeAllChildNodes();

            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire
            childNodes.remove(childNode);
        }
    }

    public void removeAllChildNodes() {
        while (!childNodes.isEmpty()) {
            Node node = childNodes.get(0);

            // remove this node's child nodes (so they get an
            // opportunity to clean up after them)
            node.removeAllChildNodes();

            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire
            childNodes.remove(0);
        }
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        String oldValue = this.iconPath;
        this.iconPath = iconPath;
        propertyChangeSupport.firePropertyChange("iconPath", oldValue, iconPath);
    }

    public void addChildNode(Node child) {
        childNodes.add(child);
    }

    public void addAction(NodeAction action) {
        nodeActions.add(action);
    }

    // Convenience method to add a new action with a pre-configured listener. If
    // an action with the same name already exists then the listener is added
    // to that action.
    public NodeAction addAction(String name, NodeActionListener actionListener) {
        NodeAction nodeAction = getNodeActionByName(name);
        if (nodeAction == null) {
            addAction(nodeAction = new NodeAction(this, name));
        }
        nodeAction.addListener(actionListener);
        return nodeAction;
    }

    protected void loadActions() {
        // add the click action handler
        addClickActionListener(new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                onNodeClick(e);
            }
        });

        // add the other actions
        Map<String, Class<? extends NodeActionListener>> actions = initActions();

        if (actions != null) {
            for (Map.Entry<String, Class<? extends NodeActionListener>> entry : actions.entrySet()) {
                try {
                    // get default constructor
                    Class<? extends NodeActionListener> listenerClass = entry.getValue();
                    NodeActionListener actionListener = createNodeActionListener(listenerClass);
                    addAction(entry.getKey(), actionListener);
                } catch (InstantiationException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e);
                } catch (NoSuchMethodException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e);
                }
            }
        }
    }

    protected NodeActionListener createNodeActionListener(Class<? extends NodeActionListener> listenerClass) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor constructor = listenerClass.getDeclaredConstructor(getClass());

        // create an instance passing this object as a constructor argument
        // since we assume that this is an inner class
        return (NodeActionListener) constructor.newInstance(this);
    }

    // sub-classes are expected to override this method and
    // add code for initializing node-specific actions; this
    // method is called when the node is being constructed and
    // is guaranteed to be called only once per node
    // NOTE: The Class<?> objects returned by this method MUST be
    // public inner classes of the sub-class. We assume that they are.
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        List<Class<? extends NodeActionListener>> actions = DefaultLoader.getActions(this.getClass());
        if (actions != null) {
            try {
                for (Class<? extends NodeActionListener> actionListener : actions) {
                    Name name = actionListener.getAnnotation(Name.class);
                    if (name != null) {
                        addAction(name.value(), createNodeActionListener(actionListener));
                    }
                }
            } catch (InstantiationException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e);
            }
        }
        return null;
    }

    // sub-classes are expected to override this method and
    // add a handler for the case when something needs to be
    // done when the user left-clicks this node in the tree view
    protected void onNodeClick(NodeActionEvent e) {
    }

    public List<NodeAction> getNodeActions() {
        return nodeActions;
    }

    public NodeAction getNodeActionByName(final String name) {
        return Iterators.tryFind(nodeActions.iterator(), new Predicate<NodeAction>() {
            @Override
            public boolean apply(NodeAction nodeAction) {
                return name.compareTo(nodeAction.getName()) == 0;
            }
        }).orNull();
    }

    public boolean hasNodeActions() {
        return !nodeActions.isEmpty();
    }

    public void addActions(Iterable<NodeAction> actions) {
        for (NodeAction action : actions) {
            addAction(action);
        }
    }

    public NodeAction getClickAction() {
        return clickAction;
    }

    public void addClickActionListener(NodeActionListener actionListener) {
        clickAction.addListener(actionListener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public Object getViewData() {
        return viewData;
    }

    public void setViewData(Object viewData) {
        this.viewData = viewData;
    }

    public Object getProject() {
        // delegate to parent node if there's one else return null
        if (parent != null) {
            return parent.getProject();
        }

        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}