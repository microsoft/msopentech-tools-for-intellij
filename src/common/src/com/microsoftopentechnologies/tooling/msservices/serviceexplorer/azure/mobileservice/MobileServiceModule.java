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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public class MobileServiceModule extends AzureRefreshableNode {
    private static final String MOBILE_SERVICE_MODULE_ID = MobileServiceModule.class.getName();
    private static final String ICON_PATH = "mobileservices.png";
    private static final String BASE_MODULE_NAME = "Mobile Services";

    public MobileServiceModule(Node parent) {
        super(MOBILE_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        // remove all child mobile service nodes
        removeAllChildNodes();

        // load all mobile services
        List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();

        for (Subscription subscription : subscriptionList) {
            List<MobileService> mobileServices = AzureManagerImpl.getManager().getMobileServiceList(subscription.getId());

            if (eventState.isEventTriggered()) {
                return;
            }

            for (MobileService mobileService : mobileServices) {
                addChildNode(new MobileServiceNode(this, mobileService));
            }
        }
    }
}