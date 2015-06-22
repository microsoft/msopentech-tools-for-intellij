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
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.vm.Endpoint;
import com.microsoftopentechnologies.tooling.msservices.model.vm.VirtualMachine;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureNodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class VMNode extends AzureRefreshableNode {
    private static abstract class VMNodeActionPromptListener extends AzureNodeActionPromptListener {
        private VMNode vmNode;

        public VMNodeActionPromptListener(@NotNull VMNode vmNode,
                                          @NotNull String promptMessageFormat,
                                          @NotNull String progressMessage) {
            super(vmNode, String.format(promptMessageFormat, vmNode.virtualMachine.getName()), progressMessage);
            this.vmNode = vmNode;
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    public class DeleteVMAction extends VMNodeActionPromptListener {
        public DeleteVMAction() {
            super(VMNode.this,
                    "This operation will delete virtual machine %s. The associated disks will not be deleted " +
                            "from your storage account. Are you sure you want to continue?",
                    "Deleting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
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

    public class DownloadRDPAction extends AzureNodeActionListener {
        private JFileChooser saveFile;

        public DownloadRDPAction() {
            super(VMNode.this, "Downloading RDP File");
        }

        @NotNull
        @Override
        protected Callable<Boolean> beforeAsyncActionPerfomed() {
            return new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    saveFile = new JFileChooser();
                    saveFile.setDialogTitle("Save RDP file");

                    return (saveFile.showSaveDialog(null) == JFileChooser.APPROVE_OPTION);
                }
            };
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            try {
                File rdpFile = saveFile.getSelectedFile();

                if (rdpFile.exists() || rdpFile.createNewFile()) {
                    FileOutputStream fileOutputStream = new FileOutputStream(rdpFile);
                    fileOutputStream.write(AzureManagerImpl.getManager().downloadRDP(virtualMachine));
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("Error downloading RDP file:", ex);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    public class ShutdownVMAction extends VMNodeActionPromptListener {
        public ShutdownVMAction() {
            super(VMNode.this,
                    "This operation will result in losing the VIP that was assigned to this virtual machine. " +
                            "Are you sure that you want to shut down virtual machine %s?",
                    "Shutting down VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            AzureManagerImpl.getManager().shutdownVirtualMachine(virtualMachine, true);
        }
    }

    public class StartVMAction extends VMNodeActionPromptListener {
        public StartVMAction() {
            super(VMNode.this,
                    "Are you sure you want to start the virtual machine %s?",
                    "Starting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            AzureManagerImpl.getManager().startVirtualMachine(virtualMachine);
        }
    }

    public class RestartVMAction extends VMNodeActionPromptListener {
        public RestartVMAction() {
            super(VMNode.this,
                    "Are you sure you want to restart the virtual machine %s?",
                    "Restarting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
                throws AzureCmdException {
            AzureManagerImpl.getManager().restartVirtualMachine(virtualMachine);
        }
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

    public VMNode(Node parent, VirtualMachine virtualMachine)
            throws AzureCmdException {
        super(virtualMachine.getName(), virtualMachine.getName(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;

        loadActions();

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
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);

        if (eventState.isEventTriggered()) {
            return;
        }

        refreshItemsInternal();
    }

    private void refreshItemsInternal() {
        // update vm name and status icon
        setName(virtualMachine.getName());
        setIconPath(getVMIconPath());

        // load up the endpoint nodes
        removeAllChildNodes();

        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
            VMEndpointNode vmEndPoint = new VMEndpointNode(this, endpoint);
            addChildNode(vmEndPoint);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        Map<String, Class<? extends NodeActionListener>> actionMap =
                new HashMap<String, Class<? extends NodeActionListener>>();

        actionMap.put(ACTION_DELETE, DeleteVMAction.class);
        actionMap.put(ACTION_DOWNLOAD_RDP_FILE, DownloadRDPAction.class);
        actionMap.put(ACTION_SHUTDOWN, ShutdownVMAction.class);
        actionMap.put(ACTION_START, StartVMAction.class);
        actionMap.put(ACTION_RESTART, RestartVMAction.class);

        return ImmutableMap.copyOf(actionMap);
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
}