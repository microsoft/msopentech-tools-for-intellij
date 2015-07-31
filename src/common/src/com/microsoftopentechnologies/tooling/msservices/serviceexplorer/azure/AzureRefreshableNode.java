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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventHandler;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.RefreshableNode;

public abstract class AzureRefreshableNode extends RefreshableNode {
    public AzureRefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public AzureRefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        EventHelper.runInterruptible(new EventHandler() {
            @Override
            public EventHelper.EventWaitHandle registerEvent()
                    throws AzureCmdException {
                return AzureManagerImpl.getManager().registerSubscriptionsChanged();
            }

            @Override
            public void unregisterEvent(@NotNull EventWaitHandle waitHandle)
                    throws AzureCmdException {
                AzureManagerImpl.getManager().unregisterSubscriptionsChanged(waitHandle);
            }

            @Override
            public void interruptibleAction(@NotNull EventStateHandle eventState)
                    throws AzureCmdException {
                refresh(eventState);
            }

            @Override
            public void eventTriggeredAction()
                    throws AzureCmdException {
            }
        });
    }

    protected abstract void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException;
}