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

import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceModule;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;

import java.util.List;

public class AzureServiceModule extends RefreshableNode {
    private static final String AZURE_SERVICE_MODULE_ID = AzureServiceModule.class.getName();
    private static final String ICON_PATH = "azure.png";
    private static final String BASE_MODULE_NAME = "Azure";

    private Object project;
    private MobileServiceModule mobileServiceModule = new MobileServiceModule(this);
    private VMServiceModule vmServiceModule = new VMServiceModule(this);
    private StorageModule storageServiceModule = new StorageModule(this);
    private EventWaitHandle subscriptionsChanged;
    private boolean registeredSubscriptionsChanged;
    private final Object subscriptionsChangedSync = new Object();

    public AzureServiceModule(Object project) {
        this(null, ICON_PATH, null);
        this.project = project;
    }

    public AzureServiceModule(Node parent, String iconPath, Object data) {
        super(AZURE_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, iconPath);
    }

    @Override
    public String getName() {
        try {
            List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
            if (subscriptionList.size() > 0) {
                return String.format("%s (%s)", BASE_MODULE_NAME, subscriptionList.size() > 1
                        ? String.format("%s subscriptions", subscriptionList.size())
                        : subscriptionList.get(0).getName());
            }
        } catch (AzureCmdException e) {
            DefaultLoader.getUIHelper().showException("Error getting subscription list", e, "Service explorer", false, true);
        }
        return BASE_MODULE_NAME;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // add the mobile service module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item

        if (!mobileServiceModule.isLoading()) {
            if (!isDirectChild(mobileServiceModule)) {
                addChildNode(mobileServiceModule);
            }

            mobileServiceModule.load();
        }

        if (!vmServiceModule.isLoading()) {
            if (!isDirectChild(vmServiceModule)) {
                addChildNode(vmServiceModule);
            }

            vmServiceModule.load();
        }


        if (!storageServiceModule.isLoading()) {
            if (!isDirectChild(storageServiceModule)) {
                addChildNode(storageServiceModule);
            }

            storageServiceModule.load();
        }
    }

    @Override
    public Object getProject() {
        return project;
    }

    public void registerSubscriptionsChanged()
            throws AzureCmdException {
        synchronized (subscriptionsChangedSync) {
            if (subscriptionsChanged == null) {
                subscriptionsChanged = AzureManagerImpl.getManager().registerSubscriptionsChanged();
            }

            registeredSubscriptionsChanged = true;

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    while (registeredSubscriptionsChanged) {
                        try {
                            subscriptionsChanged.waitEvent(new Runnable() {
                                @Override
                                public void run() {
                                    if (registeredSubscriptionsChanged) {
                                        removeAllChildNodes();

                                        mobileServiceModule = new MobileServiceModule(AzureServiceModule.this);
                                        vmServiceModule = new VMServiceModule(AzureServiceModule.this);
                                        storageServiceModule = new StorageModule(AzureServiceModule.this);

                                        load();
                                    }
                                }
                            });
                        } catch (AzureCmdException ignored) {
                            break;
                        }
                    }
                }
            });
        }
    }

    public void unregisterSubscriptionsChanged()
            throws AzureCmdException {
        synchronized (subscriptionsChangedSync) {
            registeredSubscriptionsChanged = false;

            if (subscriptionsChanged != null) {
                AzureManagerImpl.getManager().unregisterSubscriptionsChanged(subscriptionsChanged);
                subscriptionsChanged = null;
            }
        }
    }
}
