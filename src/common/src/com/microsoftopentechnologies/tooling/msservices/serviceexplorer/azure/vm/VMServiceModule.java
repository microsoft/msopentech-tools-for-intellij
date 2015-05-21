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

package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm;

import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.vm.VirtualMachine;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;

import java.util.ArrayList;
import java.util.List;

public class VMServiceModule extends Node {
    private static final String VM_SERVICE_MODULE_ID = VMServiceModule.class.getName();
    private static final String ICON_PATH = "virtualmachines.png";
    private static final String BASE_MODULE_NAME = "Virtual Machines";

    public VMServiceModule(Node parent) {
        super(VM_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH, true);
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // remove all child nodes
        removeAllChildNodes();

        // load all VMs
        ArrayList<Subscription> subscriptionList = AzureRestAPIManagerImpl.getManager().getSubscriptionList();

        if (subscriptionList != null) {
            for (Subscription subscription : subscriptionList) {
                List<VirtualMachine> virtualMachines = AzureSDKManagerImpl.getManager().getVirtualMachines(subscription.getId().toString());
                for (VirtualMachine vm : virtualMachines) {
                    addChildNode(new VMNode(this, vm));
                }
            }
        }
    }

//    @Override
//    protected Map<String, Class<? extends NodeActionListener>> initActions() {
//
//        addAction("Create VM", new CreateVMAction());
//        return null;
//    }

}
