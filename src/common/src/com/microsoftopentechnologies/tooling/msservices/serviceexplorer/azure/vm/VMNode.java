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

import com.google.common.collect.ImmutableMap;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManager.EventWaitHandle;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.vm.Endpoint;
import com.microsoftopentechnologies.tooling.msservices.model.vm.VirtualMachine;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.*;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class VMNode extends Node {
    private static abstract class VMNodeActionListener extends NodeActionListenerAsync {
        private static class EventSyncInfo {
            private final Object eventSync = new Object();
            Semaphore semaphore = new Semaphore(0);

            EventWaitHandle eventWaitHandle;
            boolean registeredEvent = false;
            boolean eventTriggered = false;
            AzureCmdException exception;
        }

        protected VMNode vmNode;
        private String promptMessageFormat;
        private int optionDialog;

        public VMNodeActionListener(String promptMessageFormat,
                                    String progressMessage) {
            super(progressMessage);
            this.promptMessageFormat = promptMessageFormat;
        }

        @NotNull
        @Override
        protected Callable<Boolean> beforeAsyncActionPerfomed() {
            return new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            optionDialog = JOptionPane.showOptionDialog(null,
                                    String.format(promptMessageFormat, vmNode.virtualMachine.getName()),
                                    "Service explorer",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    new String[]{"Yes", "No"},
                                    null);
                        }
                    });

                    return (optionDialog == JOptionPane.YES_OPTION);
                }
            };
        }

        @Override
        protected void runInBackground(NodeActionEvent e) throws AzureCmdException {
            final EventSyncInfo subsChanged = new EventSyncInfo();

            subsChanged.eventWaitHandle = AzureManagerImpl.getManager().registerSubscriptionsChanged();
            subsChanged.registeredEvent = true;

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        subsChanged.eventWaitHandle.waitEvent(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (subsChanged.eventSync) {
                                    if (subsChanged.registeredEvent) {
                                        subsChanged.registeredEvent = false;
                                        subsChanged.eventTriggered = true;
                                        subsChanged.semaphore.release();
                                    }
                                }
                            }
                        });
                    } catch (AzureCmdException ignored) {
                    }
                }
            });

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runVMAction();

                        synchronized (subsChanged.eventSync) {
                            if (subsChanged.registeredEvent) {
                                subsChanged.registeredEvent = false;
                                subsChanged.semaphore.release();
                            }
                        }
                    } catch (AzureCmdException ex) {
                        synchronized (subsChanged.eventSync) {
                            if (subsChanged.registeredEvent) {
                                subsChanged.registeredEvent = false;
                                subsChanged.exception = ex;
                                subsChanged.semaphore.release();
                            }
                        }
                    }
                }
            });

            try {
                subsChanged.semaphore.acquire();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } finally {
                AzureManagerImpl.getManager().unregisterSubscriptionsChanged(subsChanged.eventWaitHandle);
            }

            synchronized (subsChanged.eventSync) {
                if (!subsChanged.eventTriggered) {
                    if (subsChanged.exception != null) {
                        throw subsChanged.exception;
                    }

                    vmNode.refreshItems();
                }
            }
        }

        protected abstract void runVMAction() throws AzureCmdException;
    }

    private static final String WAIT_ICON_PATH = "virtualmachinewait.png";
    private static final String STOP_ICON_PATH = "virtualmachinestop.png";
    private static final String RUN_ICON_PATH = "virtualmachinerun.png";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_DOWNLOAD_RDP_FILE = "Connect Remote Desktop";
    public static final String ACTION_SHUTDOWN = "Shutdown";
    public static final String ACTION_START = "Start";
    public static final String ACTION_RESTART = "Restart";
    public static final int REMOTE_DESKTOP_PORT = 3389;

    protected VirtualMachine virtualMachine;

    public VMNode(Node parent, VirtualMachine virtualMachine) throws AzureCmdException {
        super(virtualMachine.getName(), virtualMachine.getName(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;

        // update vm icon based on vm status
        refreshItemsInternal();
    }

    private String getVMIconPath() {
        switch (virtualMachine.getStatus()) {
            case Ready:
                return RUN_ICON_PATH;
            case Stopped:
            case StoppedDeallocated:
                return STOP_ICON_PATH;
            default:
                return WAIT_ICON_PATH;
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // update vm name and status icon
        virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);

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
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                ACTION_DELETE, DeleteVMAction.class,
                ACTION_DOWNLOAD_RDP_FILE, DownloadRDPAction.class,
                ACTION_SHUTDOWN, ShutdownVMAction.class,
                ACTION_START, StartVMAction.class,
                ACTION_RESTART, RestartVMAction.class);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        // enable/disable menu items according to VM status
        boolean started = virtualMachine.getStatus().equals(VirtualMachine.Status.Ready);
        boolean stopped = virtualMachine.getStatus().equals(VirtualMachine.Status.Stopped) ||
                virtualMachine.getStatus().equals(VirtualMachine.Status.StoppedDeallocated);

        getNodeActionByName(ACTION_DOWNLOAD_RDP_FILE).setEnabled(!stopped && hasRDPPort(virtualMachine));
        getNodeActionByName(ACTION_SHUTDOWN).setEnabled(started);
        getNodeActionByName(ACTION_START).setEnabled(stopped);
        getNodeActionByName(ACTION_RESTART).setEnabled(started);

        return super.getNodeActions();
    }

    private boolean hasRDPPort(VirtualMachine virtualMachine) {
        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
            if (endpoint.getPrivatePort() == REMOTE_DESKTOP_PORT) {
                return true;
            }
        }

        return false;
    }

    public class DeleteVMAction extends NodeActionListenerAsync {
        int optionDialog;

        public DeleteVMAction() {
            super("Deleting VM");
        }

        @NotNull
        @Override
        protected Callable<Boolean> beforeAsyncActionPerfomed() {

            return new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    optionDialog = JOptionPane.showOptionDialog(null,
                            "This operation will delete virtual machine " + virtualMachine.getName() +
                                    ". The associated disks will not be deleted from your storage account. " +
                                    "Are you sure you want to continue?",
                            "Service explorer",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new String[]{"Yes", "No"},
                            null);

                    return (optionDialog == JOptionPane.YES_OPTION);
                }
            };
        }

        @Override
        protected void runInBackground(NodeActionEvent e) throws AzureCmdException {

            AzureManagerImpl.getManager().deleteVirtualMachine(virtualMachine, false);
            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                    // instruct parent node to remove this node
                    getParent().removeDirectChildNode(VMNode.this);
                }
            });

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

                    if (rdpFile.exists() || rdpFile.createNewFile()) {
                        FileOutputStream fileOutputStream = new FileOutputStream(rdpFile);
                        fileOutputStream.write(AzureManagerImpl.getManager().downloadRDP(virtualMachine));
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                }
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("Error downloading RDP file:", ex);
            }
        }
    }

    public class ShutdownVMAction extends VMNodeActionListener {
        public ShutdownVMAction() {
            super(
                    "This operation will result in losing the VIP that was assigned to this virtual machine. Are you " +
                            "sure that you want to shut down virtual machine %s?",
                    "Shutting down VM"
            );

            vmNode = VMNode.this;
        }

        @Override
        protected void runVMAction() throws AzureCmdException {
            AzureManagerImpl.getManager().shutdownVirtualMachine(vmNode.virtualMachine, true);
        }
    }

    public class StartVMAction extends VMNodeActionListener {
        public StartVMAction() {
            super(
                    "Are you sure you want to start the virtual machine %s?",
                    "Starting VM"
            );

            vmNode = VMNode.this;
        }

        @Override
        protected void runVMAction() throws AzureCmdException {
            AzureManagerImpl.getManager().startVirtualMachine(vmNode.virtualMachine);
        }
    }

    public class RestartVMAction extends VMNodeActionListener {
        public RestartVMAction() {
            super(
                    "Are you sure you want to restart the virtual machine %s?",
                    "Restarting VM"
            );

            vmNode = VMNode.this;
        }

        @Override
        protected void runVMAction() throws AzureCmdException {
            AzureManagerImpl.getManager().restartVirtualMachine(vmNode.virtualMachine);
        }
    }
}