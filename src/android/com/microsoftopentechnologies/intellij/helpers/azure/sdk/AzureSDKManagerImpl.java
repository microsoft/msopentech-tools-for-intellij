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
import com.microsoft.windowsazure.management.compute.*;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineOSImageListResponse.VirtualMachineOSImage;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineVMImageListResponse.VirtualMachineVMImage;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
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

        if (AzureRestAPIManager.getManager().getAuthenticationMode() == AzureAuthenticationMode.ActiveDirectory) {
            return apiManagerADAuth;
        } else {
            return apiManager;
        }
    }

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId) throws AzureCmdException {
        List<VirtualMachine> vmList = new ArrayList<VirtualMachine>();
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(subscriptionId);

            HostedServiceOperations hso = getHostedServiceOperations(client);

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
            client = getComputeManagementClient(vm.getSubscriptionId());

            DeploymentGetResponse deployment = getDeployment(client, vm);

            List<Role> roles = getVMDeploymentRoles(deployment);

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
            client = getComputeManagementClient(vm.getSubscriptionId());

            VirtualMachineOperations vmo = getVirtualMachineOperations(client);

            OperationStatusResponse osr = vmo.start(vm.getServiceName(), vm.getDeploymentName(), vm.getName());

            validateOperationStatus(osr);
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
            client = getComputeManagementClient(vm.getSubscriptionId());

            VirtualMachineOperations vmo = getVirtualMachineOperations(client);

            VirtualMachineShutdownParameters parameters = new VirtualMachineShutdownParameters();
            parameters.setPostShutdownAction(deallocate ? PostShutdownAction.StoppedDeallocated : PostShutdownAction.Stopped);

            OperationStatusResponse osr = vmo.shutdown(vm.getServiceName(), vm.getDeploymentName(), vm.getName(), parameters);

            validateOperationStatus(osr);
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
            client = getComputeManagementClient(vm.getSubscriptionId());

            VirtualMachineOperations vmo = getVirtualMachineOperations(client);

            OperationStatusResponse osr = vmo.restart(vm.getServiceName(), vm.getDeploymentName(), vm.getName());

            validateOperationStatus(osr);
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
            client = getComputeManagementClient(vm.getSubscriptionId());

            DeploymentGetResponse deployment = getDeployment(client, vm);

            List<Role> roles = getVMDeploymentRoles(deployment);

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
            client = getComputeManagementClient(vm.getSubscriptionId());

            VirtualMachineOperations vmo = getVirtualMachineOperations(client);

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
    @Override
    public List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId) throws AzureCmdException {
        List<VirtualMachineImage> vmImageList = new ArrayList<VirtualMachineImage>();
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(subscriptionId);
            vmImageList = loadOSImages(client, vmImageList);
            vmImageList = loadVMImages(client, vmImageList);

            return vmImageList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the VM Image list", t);
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
    private static ComputeManagementClient getComputeManagementClient(@NotNull String subscriptionId) throws Exception {
        ComputeManagementClient client = AzureSDKHelper.getComputeManagementClient(subscriptionId);

        if (client == null) {
            throw new Exception("Unable to instantiate Compute Management client");
        }

        return client;
    }

    @NotNull
    private static HostedServiceOperations getHostedServiceOperations(@NotNull ComputeManagementClient client) throws Exception {
        HostedServiceOperations hso = client.getHostedServicesOperations();

        if (hso == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hso;
    }

    @NotNull
    private static DeploymentOperations getDeploymentOperations(@NotNull ComputeManagementClient client) throws Exception {
        DeploymentOperations dop = client.getDeploymentsOperations();

        if (dop == null) {
            throw new Exception("Unable to retrieve Deployment information");
        }

        return dop;
    }

    @NotNull
    private static VirtualMachineOperations getVirtualMachineOperations(@NotNull ComputeManagementClient client) throws Exception {
        VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

        if (vmo == null) {
            throw new Exception("Unable to retrieve Virtual Machines Information");
        }

        return vmo;
    }

    @NotNull
    private static VirtualMachineOSImageOperations getVirtualMachineOSImageOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        VirtualMachineOSImageOperations vmosio = client.getVirtualMachineOSImagesOperations();

        if (vmosio == null) {
            throw new Exception("Unable to retrieve OS Images information");
        }

        return vmosio;
    }

    @NotNull
    private static VirtualMachineVMImageOperations getVirtualMachineVMImageOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        VirtualMachineVMImageOperations vmvmio = client.getVirtualMachineVMImagesOperations();

        if (vmvmio == null) {
            throw new Exception("Unable to retrieve VM Images information");
        }

        return vmvmio;
    }

    @Nullable
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                                       @NotNull DeploymentSlot slot)
            throws Exception {
        try {
            DeploymentOperations dop = getDeploymentOperations(client);

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
            DeploymentOperations dop = getDeploymentOperations(client);

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
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client, @NotNull VirtualMachine vm)
            throws Exception {
        DeploymentGetResponse deployment = getDeployment(client, vm.getServiceName(), vm.getDeploymentName());

        if (deployment == null) {
            throw new Exception("Invalid Virtual Machine information. No Deployment matches the VM data.");
        }

        return deployment;
    }

    @NotNull
    private static List<Role> getVMDeploymentRoles(@NotNull DeploymentGetResponse deployment) throws Exception {
        ArrayList<Role> roles = deployment.getRoles();

        if (roles == null) {
            throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
        }

        return roles;
    }

    private static void validateOperationStatus(@Nullable OperationStatusResponse osr) throws Exception {
        if (osr == null) {
            throw new Exception("Unable to retrieve Operation Status");
        }

        if (osr.getError() != null) {
            throw new Exception(osr.getError().getMessage());
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
        VirtualMachineOperations vmo = getVirtualMachineOperations(client);

        OperationStatusResponse osr = vmo.delete(serviceName, deploymentName, virtualMachineName, deleteFromStorage);

        validateOperationStatus(osr);
    }

    private static void deleteDeployment(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                         @NotNull String deploymentName, boolean deleteFromStorage)
            throws Exception {
        DeploymentOperations dop = getDeploymentOperations(client);

        OperationStatusResponse osr = dop.deleteByName(serviceName, deploymentName, deleteFromStorage);

        validateOperationStatus(osr);
    }

    @NotNull
    private static List<VirtualMachineImage> loadOSImages(@NotNull ComputeManagementClient client,
                                                          @NotNull List<VirtualMachineImage> vmImageList)
            throws Exception {
        VirtualMachineOSImageListResponse osImages = getVirtualMachineOSImageOperations(client).list();

        if (osImages != null) {
            for (VirtualMachineOSImage osImage : osImages) {
                vmImageList.add(
                        new VirtualMachineImage(
                                osImage.getName() != null ? osImage.getName() : "",
                                osImage.getCategory() != null ? osImage.getCategory() : "",
                                osImage.getPublisherName() != null ? osImage.getPublisherName() : "",
                                osImage.getPublishedDate() != null ? osImage.getPublishedDate() : GregorianCalendar.getInstance(),
                                osImage.getLabel() != null ? osImage.getLabel() : "",
                                osImage.getDescription() != null ? osImage.getDescription() : "",
                                osImage.getOperatingSystemType() != null ? osImage.getOperatingSystemType() : "",
                                osImage.getLocation() != null ? osImage.getLocation() : "",
                                osImage.getEula() != null ? osImage.getEula() : "",
                                osImage.getPrivacyUri() != null ? osImage.getPrivacyUri().toString() : "",
                                osImage.getPricingDetailUri() != null ? osImage.getPricingDetailUri().toString() : "",
                                osImage.isShowInGui() != null ? osImage.isShowInGui() : false));
            }
        }

        return vmImageList;
    }

    @NotNull
    private static List<VirtualMachineImage> loadVMImages(@NotNull ComputeManagementClient client,
                                                          @NotNull List<VirtualMachineImage> vmImageList)
            throws Exception {
        VirtualMachineVMImageListResponse vmImages = getVirtualMachineVMImageOperations(client).list();

        if (vmImages != null) {
            for (VirtualMachineVMImage vmImage : vmImages) {
                vmImageList.add(
                        new VirtualMachineImage(
                                vmImage.getName() != null ? vmImage.getName() : "",
                                vmImage.getCategory() != null ? vmImage.getCategory() : "",
                                vmImage.getPublisherName() != null ? vmImage.getPublisherName() : "",
                                vmImage.getPublishedDate() != null ? vmImage.getPublishedDate() : GregorianCalendar.getInstance(),
                                vmImage.getLabel() != null ? vmImage.getLabel() : "",
                                vmImage.getDescription() != null ? vmImage.getDescription() : "",
                                vmImage.getOSDiskConfiguration() != null
                                        && vmImage.getOSDiskConfiguration().getOperatingSystem() != null ?
                                        vmImage.getOSDiskConfiguration().getOperatingSystem() :
                                        "",
                                vmImage.getLocation() != null ? vmImage.getLocation() : "",
                                vmImage.getEula() != null ? vmImage.getEula() : "",
                                vmImage.getPrivacyUri() != null ? vmImage.getPrivacyUri().toString() : "",
                                vmImage.getPricingDetailLink() != null ? vmImage.getPricingDetailLink().toString() : "",
                                vmImage.isShowInGui() != null ? vmImage.isShowInGui() : false));
            }
        }

        return vmImageList;
    }
}