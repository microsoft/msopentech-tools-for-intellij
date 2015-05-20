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

package com.microsoftopentechnologies.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureServiceModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.ManageSubscriptionsAction;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice.*;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice.CreateTableAction;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.*;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.*;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.CreateVMAction;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.TableNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.*;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;

import java.util.HashMap;
import java.util.Map;

public class NodeActionsMap {
    public static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>>();
    static {
        node2Actions.put(AzureServiceModule.class, new ImmutableList.Builder().add(ManageSubscriptionsAction.class).build());
        node2Actions.put(VMServiceModule.class, new ImmutableList.Builder().add(CreateVMAction.class).build());
        node2Actions.put(MobileServiceModule.class, new ImmutableList.Builder().add(CreateServiceAction.class).build());
        node2Actions.put(QueueModule.class, new ImmutableList.Builder().add(CreateQueueAction.class).build());
        node2Actions.put(TableModule.class, new ImmutableList.Builder().add(com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.CreateTableAction.class).build());
        node2Actions.put(MobileServiceNode.class, new ImmutableList.Builder().add(CreateTableAction.class, CreateAPIAction.class, CreateNewJobAction.class).build());
        node2Actions.put(TableNode.class, new ImmutableList.Builder().add(EditTableAction.class).build());
        node2Actions.put(TableScriptNode.class, new ImmutableList.Builder().add(UpdateScriptAction.class).build());
        node2Actions.put(CustomAPINode.class, new ImmutableList.Builder().add(UpdateCustomAPIAction.class, EditCustomAPIAction.class).build());
        node2Actions.put(ScheduledJobNode.class, new ImmutableList.Builder().add(UpdateJobAction.class, EditJobAction.class).build());
        node2Actions.put(BlobModule.class, new ImmutableList.Builder().add(CreateBlobContainer.class).build());
        node2Actions.put(StorageModule.class, new ImmutableList.Builder().add(CreateStorageAccountAction.class, AttachExternalStorageAccountAction.class).build());
        node2Actions.put(ExternalStorageNode.class, new ImmutableList.Builder().add(ConfirmDialogAction.class, ModifyExternalStorageAccountAction.class).build());

    }
}
