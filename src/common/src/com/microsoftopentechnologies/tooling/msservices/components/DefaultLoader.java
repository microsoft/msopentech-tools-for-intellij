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
package com.microsoftopentechnologies.tooling.msservices.components;

import com.google.common.collect.ImmutableList;
import com.microsoftopentechnologies.tooling.msservices.helpers.IDEHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.UIHelper;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.List;
import java.util.Map;

public class DefaultLoader {
    public static final String PLUGIN_ID = "com.microsoftopentechnologies.intellij";
    private static UIHelper uiHelper;
    private static IDEHelper ideHelper;
    private static PluginComponent pluginComponent;
    private static Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions;

    private DefaultLoader() {
    }

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
