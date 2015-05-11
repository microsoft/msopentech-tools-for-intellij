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
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.BlobModule;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.QueueModule;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.TableModule;
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
        node2Actions.put(StorageModule.class, new ImmutableList.Builder().add(CreateStorageAccountAction.class).build());
    }
}
