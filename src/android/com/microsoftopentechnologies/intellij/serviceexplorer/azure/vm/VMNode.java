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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeAction;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class VMNode extends Node {
    private static final String WAIT_ICON_PATH = "virtualmachinewait.png";
    private static final String STOP_ICON_PATH = "virtualmachinestop.png";
    private static final String RUN_ICON_PATH = "virtualmachinerun.png";
    private static final String VM_STATUS_RUNNING = "Running";
    private static final String VM_STATUS_SUSPENDED = "Suspended";

    protected VirtualMachine virtualMachine;

    public VMNode(Node parent, VirtualMachine virtualMachine) throws AzureCmdException {
        super(virtualMachine.getName(), virtualMachine.getName(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;

        // update vm icon based on vm status
        refreshItemsInternal();
    }

    private String getVMIconPath() {
        String status = virtualMachine.getStatus();
        if (status.equals(VM_STATUS_RUNNING))
            return RUN_ICON_PATH;
        if (status.equals(VM_STATUS_SUSPENDED))
            return STOP_ICON_PATH;
        return WAIT_ICON_PATH;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // update vm name and status icon
        virtualMachine = AzureSDKManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);

        refreshItemsInternal();
    }

    private void refreshItemsInternal() throws AzureCmdException {
        // update vm name and status icon
        setName(virtualMachine.getName());
        setIconPath(getVMIconPath());

        // load up the endpoint nodes
        removeAllChildNodes();

        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
            VMEndpointNode vmEndPoint = new VMEndpointNode(this, endpoint);
            addChildNode(vmEndPoint);
            vmEndPoint.refreshItems();
        }
    }

    @Override
    public List<NodeAction> getNodeActions() {
        // enable/disable menu items according to VM status
        getNodeActionByName("Shutdown").setEnabled(virtualMachine.getStatus().equals(VM_STATUS_RUNNING));
        getNodeActionByName("Start").setEnabled(!virtualMachine.getStatus().equals(VM_STATUS_RUNNING));
        getNodeActionByName("Restart").setEnabled(virtualMachine.getStatus().equals(VM_STATUS_RUNNING));
        getNodeActionByName("Download RDP file").setEnabled(virtualMachine.getStatus().equals(VM_STATUS_RUNNING));

        return super.getNodeActions();
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Delete", DeleteVMAction.class,
                "Download RDP file", DownloadRDPAction.class,
                "Shutdown", ShutdownVMAction.class,
                "Start", StartVMAction.class,
                "Restart", RestartVMAction.class);
    }

    public class DeleteVMAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            final int optionDialog = JOptionPane.showOptionDialog(null,
                    "This operation will delete virtual machine " + virtualMachine.getName() + ". The associated disks will not be deleted from your storage account. Are you sure you want to continue?",
                    "Service explorer",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Yes", "No"},
                    null);

            if (optionDialog == JOptionPane.YES_OPTION) {
                ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Deleting VM", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            AzureSDKManagerImpl.getManager().deleteVirtualMachine(virtualMachine, false);

                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    // instruct parent node to remove this node
                                    getParent().removeDirectChildNode(VMNode.this);
                                }
                            });
                        } catch (AzureCmdException ex) {
                            UIHelper.showException("Error deleting virtual machine", ex);
                        }
                    }
                });
            }
        }
    }

    public class DownloadRDPAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            try {
                JFileChooser saveFile = new JFileChooser();
                saveFile.setDialogTitle("Save RDP file");
                if (saveFile.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File rdpFile = saveFile.getSelectedFile();

                    if (!rdpFile.exists()) {
                        rdpFile.createNewFile();
                    }

                    FileOutputStream fileOutputStream = new FileOutputStream(rdpFile);
                    fileOutputStream.write(AzureSDKManagerImpl.getManager().downloadRDP(virtualMachine));
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (Exception ex) {
                UIHelper.showException("Error downloading RDP file:", ex);
            }
        }
    }

    private interface VMActionRunnable {
        abstract void run() throws AzureCmdException;
    }

    private void runVMAction(
            String promptMessage,
            String progressMessage,
            final VMActionRunnable action) {
        final int optionDialog = JOptionPane.showOptionDialog(null,
                promptMessage,
                "Service explorer",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Yes", "No"},
                null);

        if (optionDialog == JOptionPane.YES_OPTION) {
            ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), progressMessage, false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        action.run();

                        // reload vm details
                        refreshItems();
                    } catch (AzureCmdException ex) {
                        UIHelper.showException("Error shutting down virtual machine", ex);
                    }

                }
            });
        }
    }

    public class ShutdownVMAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            runVMAction(
                    "This operation will result in losing the VIP that was assigned to this virtual machine. Are you sure that you want to shut down virtual machine " + virtualMachine.getName() + "?",
                    "Shutting down VM",
                    new VMActionRunnable() {
                        @Override
                        public void run() throws AzureCmdException {
                            AzureSDKManagerImpl.getManager().shutdownVirtualMachine(virtualMachine, true);
                        }
                    }
            );
        }
    }

    public class StartVMAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            runVMAction(
                    "Are you sure you want to start the virtual machine " + virtualMachine.getName() + "?",
                    "Starting VM",
                    new VMActionRunnable() {
                        @Override
                        public void run() throws AzureCmdException {
                            AzureSDKManagerImpl.getManager().startVirtualMachine(virtualMachine);
                        }
                    }
            );
        }
    }

    public class RestartVMAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            runVMAction(
                    "Are you sure you want to restart the virtual machine " + virtualMachine.getName() + "?",
                    "Restarting VM",
                    new VMActionRunnable() {
                        @Override
                        public void run() throws AzureCmdException {
                            AzureSDKManagerImpl.getManager().restartVirtualMachine(virtualMachine);
                        }
                    }
            );
        }
    }
}