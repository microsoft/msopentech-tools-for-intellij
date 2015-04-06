package com.microsoftopentechnologies.intellij.components;

import com.google.common.collect.ImmutableList;
import com.microsoftopentechnologies.intellij.helpers.IDEHelper;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.List;
import java.util.Map;

public class DefaultLoader {
    public static final String PLUGIN_ID = "com.microsoftopentechnologies.intellij";
    private static UIHelper uiHelper;
    private static IDEHelper ideHelper;
    private static PluginComponent pluginComponent;
    private static Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions;

    public static void setUiHelper(UIHelper uiHelper) {
        DefaultLoader.uiHelper = uiHelper;
    }

    public static void setPluginComponent(PluginComponent pluginComponent) {
        DefaultLoader.pluginComponent = pluginComponent;
    }

    public static void setNode2Actions(Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions) {
        DefaultLoader.node2Actions = node2Actions;
    }

    public static void setIdeHelper(IDEHelper ideHelper) {
        DefaultLoader.ideHelper = ideHelper;
    }

    public static UIHelper getUIHelper() {
        return uiHelper;
    }

    public static PluginComponent getPluginComponent() {
        return pluginComponent;
    }

    public static List<Class<? extends NodeActionListener>> getActions(Class<? extends Node> nodeClass) {
        return node2Actions.get(nodeClass);
    }

    public static IDEHelper getIdeHelper() {
        return ideHelper;
    }
}
