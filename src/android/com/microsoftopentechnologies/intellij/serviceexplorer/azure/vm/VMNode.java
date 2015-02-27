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
import com.intellij.openapi.application.ModalityState;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine.Status;
import com.microsoftopentechnologies.intellij.serviceexplorer.*;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class VMNode extends Node {
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
        getNodeActionByName(ACTION_SHUTDOWN).setEnabled(virtualMachine.getStatus().equals(Status.Ready));
        getNodeActionByName(ACTION_START).setEnabled(!virtualMachine.getStatus().equals(Status.Ready));
        getNodeActionByName(ACTION_RESTART).setEnabled(virtualMachine.getStatus().equals(Status.Ready));
        getNodeActionByName(ACTION_DOWNLOAD_RDP_FILE).setEnabled(
                virtualMachine.getStatus().equals(Status.Ready)
                && hasRDPPort(virtualMachine)
        );

        return super.getNodeActions();
    }

    private boolean hasRDPPort(VirtualMachine virtualMachine) {
        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
            if(endpoint.getPrivatePort() == REMOTE_DESKTOP_PORT) {
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

        @Override
        protected void runInBackground(NodeActionEvent e) throws AzureCmdException {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
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
                }
            }, ModalityState.any());

            if (optionDialog == JOptionPane.YES_OPTION) {
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
                    throw ex;
                }
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

                    if (rdpFile.exists() || rdpFile.createNewFile()) {
                        FileOutputStream fileOutputStream = new FileOutputStream(rdpFile);
                        fileOutputStream.write(AzureSDKManagerImpl.getManager().downloadRDP(virtualMachine));
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                }
            } catch (Exception ex) {
                UIHelper.showException("Error downloading RDP file:", ex);
            }
        }
    }

    public abstract class VMNodeActionListener extends NodeActionListenerAsync {
        private String promptMessageFormat;
        private String progressMessage;
        private int optionDialog;

        public VMNodeActionListener(String promptMessageFormat,
                                    String progressMessage) {
            super(progressMessage);
            this.promptMessageFormat = promptMessageFormat;
            this.progressMessage = progressMessage;
        }

        @Override
        protected void runInBackground(NodeActionEvent e) throws AzureCmdException {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    optionDialog = JOptionPane.showOptionDialog(null,
                            String.format(promptMessageFormat, virtualMachine.getName()),
                            "Service explorer",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new String[]{"Yes", "No"},
                            null);
                }
            }, ModalityState.any());

            if (optionDialog == JOptionPane.YES_OPTION) {
                try {
                    runVMAction();

                    // reload vm details
                    refreshItems();
                } catch (AzureCmdException ex) {
                    UIHelper.showException("Error " + progressMessage + " " + virtualMachine.getName(), ex);
                    throw ex;
                }
            }
        }

        protected void runVMAction() throws AzureCmdException {
        }
    }

    public class ShutdownVMAction extends VMNodeActionListener {
        public ShutdownVMAction() {
            super(
                    "This operation will result in losing the VIP that was assigned to this virtual machine. Are you " +
                            "sure that you want to shut down virtual machine %s?",
                    "Shutting down VM"
            );
        }

        @Override
        protected void runVMAction() throws AzureCmdException {
            AzureSDKManagerImpl.getManager().shutdownVirtualMachine(virtualMachine, true);
        }
    }

    public class StartVMAction extends VMNodeActionListener {
        public StartVMAction() {
            super(
                    "Are you sure you want to start the virtual machine %s?",
                    "Starting VM"
            );
        }

        @Override
        protected void runVMAction() throws AzureCmdException {
            AzureSDKManagerImpl.getManager().startVirtualMachine(virtualMachine);
        }
    }

    public class RestartVMAction extends VMNodeActionListener {
        public RestartVMAction() {
            super(
                    "Are you sure you want to restart the virtual machine %s?",
                    "Restarting VM"
            );
        }

        @Override
        protected void runVMAction() throws AzureCmdException {
            AzureSDKManagerImpl.getManager().restartVirtualMachine(virtualMachine);
        }
    }
}