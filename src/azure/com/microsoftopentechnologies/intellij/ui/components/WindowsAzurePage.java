/**
 * Copyright 2014 Microsoft Open Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.ui.components;

import javax.swing.event.EventListenerList;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.ConfigurationEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.ConfigurationEventListener;

public abstract class WindowsAzurePage extends DialogWrapper {

    private static final EventListenerList LISTENERS = new EventListenerList();

    //	protected WindowsAzurePage(String pageName) {
//		super(pageName);
//	}
    protected WindowsAzurePage(Project project) {
        super(project);
    }

    public static void addConfigurationEventListener(ConfigurationEventListener listener) {
        LISTENERS.add(ConfigurationEventListener.class, listener);
    }

    public void removeConfigurationEventListener(ConfigurationEventListener listener) {
        LISTENERS.remove(ConfigurationEventListener.class, listener);
    }

    public void fireConfigurationEvent(ConfigurationEventArgs config) {
        Object[] list = LISTENERS.getListenerList();

        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == ConfigurationEventListener.class) {
                ((ConfigurationEventListener) list[i + 1]).onConfigurationChanged(config);
            }
        }
    }
}
