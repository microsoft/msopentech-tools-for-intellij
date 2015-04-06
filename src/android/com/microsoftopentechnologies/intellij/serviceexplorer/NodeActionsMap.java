package com.microsoftopentechnologies.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.AzureServiceModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.ManageSubscriptionsAction;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice.*;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.BlobModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.CreateBlobContainer;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.StorageModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.CreateVMAction;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.VMServiceModule;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage.CreateStorageAccountAction;

import java.util.HashMap;
import java.util.Map;

public class NodeActionsMap {
    public static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>>();
    static {
        node2Actions.put(AzureServiceModule.class, new ImmutableList.Builder().add(ManageSubscriptionsAction.class).build());
        node2Actions.put(VMServiceModule.class, new ImmutableList.Builder().add(CreateVMAction.class).build());
        node2Actions.put(MobileServiceModule.class, new ImmutableList.Builder().add(CreateServiceAction.class).build());
        node2Actions.put(MobileServiceNode.class, new ImmutableList.Builder().add(CreateTableAction.class, CreateAPIAction.class, CreateNewJobAction.class).build());
        node2Actions.put(TableNode.class, new ImmutableList.Builder().add(EditTableAction.class).build());
        node2Actions.put(TableScriptNode.class, new ImmutableList.Builder().add(UpdateScriptAction.class).build());
        node2Actions.put(CustomAPINode.class, new ImmutableList.Builder().add(UpdateCustomAPIAction.class, EditCustomAPIAction.class).build());
        node2Actions.put(ScheduledJobNode.class, new ImmutableList.Builder().add(UpdateJobAction.class, EditJobAction.class).build());
        node2Actions.put(BlobModule.class, new ImmutableList.Builder().add(CreateBlobContainer.class).build());
        node2Actions.put(StorageModule.class, new ImmutableList.Builder().add(CreateStorageAccountAction.class).build());
    }
}
