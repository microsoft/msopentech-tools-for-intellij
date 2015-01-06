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
package com.microsoftopentechnologies.intellij.helpers.azure.sdk;

import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.DeploymentOperations;
import com.microsoft.windowsazure.management.compute.HostedServiceOperations;
import com.microsoft.windowsazure.management.compute.VirtualMachineOperations;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AzureSDKManagerImpl implements AzureSDKManager {
    private static final String PERSISTENT_VM_ROLE = "PersistentVMRole";
    private static final String NETWORK_CONFIGURATION = "NetworkConfiguration";

    private static AzureSDKManager apiManager;
    private static AzureSDKManager apiManagerADAuth;

    private AzureSDKManagerImpl() {
    }

    @NotNull
    public static AzureSDKManager getManager() {
        if (apiManager == null) {
            apiManager = new AzureSDKManagerImpl();
            apiManagerADAuth = new AzureSDKManagerADAuthDecorator(apiManager);
        }

        AzureAuthenticationMode authenticationMode = AzureRestAPIManager.getManager().getAuthenticationMode();
        if(authenticationMode == AzureAuthenticationMode.ActiveDirectory) {
            return apiManagerADAuth;
        }

        return apiManager;
    }

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId) throws AzureCmdException {
        List<VirtualMachine> vmList = new ArrayList<VirtualMachine>();
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(subscriptionId);

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            HostedServiceOperations hso = client.getHostedServicesOperations();

            if (hso == null) {
                throw new Exception("Unable to retrieve Hosted Services information");
            }

            HostedServiceListResponse hslr = hso.list();

            if (hslr == null) {
                return vmList;
            }

            ArrayList<HostedService> hostedServices = hslr.getHostedServices();

            if (hostedServices == null) {
                return vmList;
            }

            for (HostedService hostedService : hostedServices) {
                String serviceName = hostedService.getServiceName() != null ? hostedService.getServiceName() : "";
                vmList = processDeploymentSlot(client, subscriptionId, serviceName, DeploymentSlot.Production, vmList);
                vmList = processDeploymentSlot(client, subscriptionId, serviceName, DeploymentSlot.Staging, vmList);
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the VM list", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }

        return vmList;
    }

    @Override
    @NotNull
    public VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(vm.getSubscriptionId());

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            DeploymentGetResponse deployment = getDeployment(client, vm.getServiceName(), vm.getDeploymentName());

            if (deployment == null) {
                throw new Exception("Invalid Virtual Machine information. No Deployment matches the VM data.");
            }

            ArrayList<Role> roles = deployment.getRoles();

            if (roles == null) {
                throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
            }

            Role vmRole = null;

            for (Role role : roles) {
                if (PERSISTENT_VM_ROLE.equals(role.getRoleType()) && vm.getName().equals(role.getRoleName())) {
                    vmRole = role;
                    break;
                }
            }

            if (vmRole == null) {
                throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
            }

            vm.setDns(deployment.getUri() != null ? deployment.getUri().toString() : "");
            vm.setEnvironment(deployment.getDeploymentSlot() != null ? deployment.getDeploymentSlot().toString() : "");
            vm.setSize(vmRole.getRoleSize() != null ? vmRole.getRoleSize() : "");
            vm.setStatus(deployment.getStatus() != null ? deployment.getStatus().toString() : "");

            vm.getEndpoints().clear();
            vm = loadEndpoints(vmRole, vm);
        } catch (Throwable t) {
            throw new AzureCmdException("Error starting the VM", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }

        return vm;
    }

    @Override
    public void startVirtualMachine(@NotNull VirtualMachine vm) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(vm.getSubscriptionId());

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

            if (vmo == null) {
                return;
            }

            OperationStatusResponse osr = vmo.start(vm.getServiceName(), vm.getDeploymentName(), vm.getName());

            if (osr == null) {
                return;
            }

            if (osr.getError() != null) {
                throw new Exception(osr.getError().getMessage());
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error starting the VM", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void shutdownVirtualMachine(@NotNull VirtualMachine vm, boolean deallocate) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(vm.getSubscriptionId());

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

            if (vmo == null) {
                return;
            }

            VirtualMachineShutdownParameters parameters = new VirtualMachineShutdownParameters();
            parameters.setPostShutdownAction(deallocate ? PostShutdownAction.StoppedDeallocated : PostShutdownAction.Stopped);

            OperationStatusResponse osr = vmo.shutdown(vm.getServiceName(), vm.getDeploymentName(), vm.getName(), parameters);

            if (osr == null) {
                return;
            }

            if (osr.getError() != null) {
                throw new Exception(osr.getError().getMessage());
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error shutting down the VM", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void restartVirtualMachine(@NotNull VirtualMachine vm) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(vm.getSubscriptionId());

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

            if (vmo == null) {
                throw new Exception("Unable to retrieve Virtual Machine information");
            }

            OperationStatusResponse osr = vmo.restart(vm.getServiceName(), vm.getDeploymentName(), vm.getName());

            if (osr == null) {
                return;
            }

            if (osr.getError() != null) {
                throw new Exception(osr.getError().getMessage());
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error restarting the VM", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void deleteVirtualMachine(@NotNull VirtualMachine vm, boolean deleteFromStorage) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(vm.getSubscriptionId());

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            DeploymentGetResponse deployment = getDeployment(client, vm.getServiceName(), vm.getDeploymentName());

            if (deployment == null) {
                throw new Exception("Invalid Virtual Machine information. No Deployment matches the VM data.");
            }

            ArrayList<Role> roles = deployment.getRoles();

            if (roles == null) {
                throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
            }

            if (roles.size() == 1) {
                Role role = roles.get(0);

                if (PERSISTENT_VM_ROLE.equals(role.getRoleType()) && vm.getName().equals(role.getRoleName())) {
                    deleteDeployment(client, vm.getServiceName(), vm.getDeploymentName(), deleteFromStorage);
                } else {
                    throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
                }
            } else if (roles.size() > 1) {
                deleteVMRole(client, vm.getServiceName(), vm.getDeploymentName(), vm.getName(), deleteFromStorage);
            } else {
                throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the VM", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @NotNull
    @Override
    public byte[] downloadRDP(@NotNull VirtualMachine vm) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = AzureSDKHelper.getComputeManagementClient(vm.getSubscriptionId());

            if (client == null) {
                throw new Exception("Unable to instantiate Compute Management client");
            }

            VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

            if (vmo == null) {
                throw new Exception("Unable to retrieve Virtual Machine information");
            }

            VirtualMachineGetRemoteDesktopFileResponse vmgrdfr = vmo.getRemoteDesktopFile(
                    vm.getServiceName(),
                    vm.getDeploymentName(),
                    vm.getName());

            if (vmgrdfr == null) {
                throw new Exception("Unable to retrieve RDP information");
            }

            byte[] remoteDesktopFile = vmgrdfr.getRemoteDesktopFile();

            if (remoteDesktopFile == null) {
                throw new Exception("Unable to retrieve RDP information");
            }

            return (new String(remoteDesktopFile, "UTF-8")).getBytes();
        } catch (Throwable t) {
            throw new AzureCmdException("Error downloading the RDP file", t);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @NotNull
    private static List<VirtualMachine> processDeploymentSlot(@NotNull ComputeManagementClient client, @NotNull String subscriptionId, @NotNull String serviceName,
                                                              @NotNull DeploymentSlot slot, @NotNull List<VirtualMachine> vmList)
            throws Exception {
        DeploymentGetResponse deployment = getDeployment(client, serviceName, slot);

        if (deployment == null) {
            return vmList;
        }

        for (Role role : deployment.getRoles()) {
            if (role.getRoleType() != null
                    && role.getRoleType().equals(PERSISTENT_VM_ROLE)) {
                VirtualMachine vm = new VirtualMachine(
                        role.getRoleName() != null ? role.getRoleName() : "",
                        serviceName,
                        deployment.getUri() != null ? deployment.getUri().toString() : "",
                        slot.toString(),
                        deployment.getName() != null ? deployment.getName() : "",
                        role.getRoleSize() != null ? role.getRoleSize() : "",
                        deployment.getStatus() != null ? deployment.getStatus().toString() : "",
                        subscriptionId);

                vm = loadEndpoints(role, vm);

                vmList.add(vm);
            }
        }

        return vmList;
    }

    @Nullable
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                                       @NotNull DeploymentSlot slot)
            throws Exception {
        try {
            DeploymentOperations dop = client.getDeploymentsOperations();

            if (dop == null) {
                throw new Exception("Unable to retrieve Deployment information");
            }

            return dop.getBySlot(serviceName, slot);
        } catch (ServiceException se) {
            if (se.getHttpStatusCode() == 404) {
                return null;
            } else {
                throw se;
            }
        }
    }

    @Nullable
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                                       @NotNull String deploymentName)
            throws Exception {
        try {
            DeploymentOperations dop = client.getDeploymentsOperations();

            if (dop == null) {
                throw new Exception("Unable to retrieve Deployment information");
            }

            return dop.getByName(serviceName, deploymentName);
        } catch (ServiceException se) {
            if (se.getHttpStatusCode() == 404) {
                return null;
            } else {
                throw se;
            }
        }
    }

    @NotNull
    private static VirtualMachine loadEndpoints(@NotNull Role role, @NotNull VirtualMachine vm) {
        if (role.getConfigurationSets() == null) {
            return vm;
        }

        List<Endpoint> endpoints = vm.getEndpoints();

        for (ConfigurationSet configurationSet : role.getConfigurationSets()) {
            if (configurationSet.getConfigurationSetType() != null
                    && configurationSet.getConfigurationSetType().equals(NETWORK_CONFIGURATION)
                    && configurationSet.getInputEndpoints() != null) {
                for (InputEndpoint inputEndpoint : configurationSet.getInputEndpoints()) {
                    endpoints.add(new Endpoint(
                            inputEndpoint.getName() != null ? inputEndpoint.getName() : "",
                            inputEndpoint.getProtocol() != null ? inputEndpoint.getProtocol() : "",
                            inputEndpoint.getLocalPort(),
                            inputEndpoint.getPort()));
                }
            }
        }

        return vm;
    }

    private static void deleteVMRole(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                     @NotNull String deploymentName, @NotNull String virtualMachineName,
                                     boolean deleteFromStorage)
            throws Exception {
        VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

        if (vmo == null) {
            throw new Exception("Unable to retrieve Virtual Machine information");
        }

        OperationStatusResponse osr = vmo.delete(serviceName, deploymentName, virtualMachineName, deleteFromStorage);

        if (osr == null) {
            return;
        }

        if (osr.getError() != null) {
            throw new Exception(osr.getError().getMessage());
        }
    }

    private static void deleteDeployment(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                         @NotNull String deploymentName, boolean deleteFromStorage)
            throws Exception {
        DeploymentOperations dop = client.getDeploymentsOperations();

        if (dop == null) {
            throw new Exception("Unable to retrieve Deployment information");
        }

        OperationStatusResponse osr = dop.deleteByName(serviceName, deploymentName, deleteFromStorage);

        if (osr == null) {
            return;
        }

        if (osr.getError() != null) {
            throw new Exception(osr.getError().getMessage());
        }
    }
}