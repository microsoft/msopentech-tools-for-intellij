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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.windowsazure.core.OperationResponse;
import com.microsoft.windowsazure.core.OperationStatus;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.AffinityGroupOperations;
import com.microsoft.windowsazure.management.LocationOperations;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.RoleSizeOperations;
import com.microsoft.windowsazure.management.compute.*;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineOSImageListResponse.VirtualMachineOSImage;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineVMImageListResponse.VirtualMachineVMImage;
import com.microsoft.windowsazure.management.models.AffinityGroupListResponse;
import com.microsoft.windowsazure.management.models.LocationsListResponse;
import com.microsoft.windowsazure.management.models.RoleSizeListResponse;
import com.microsoft.windowsazure.management.models.RoleSizeListResponse.RoleSize;
import com.microsoft.windowsazure.management.storage.StorageAccountOperations;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoft.windowsazure.management.storage.models.StorageAccountCreateParameters;
import com.microsoft.windowsazure.management.storage.models.StorageAccountGetKeysResponse;
import com.microsoft.windowsazure.management.storage.models.StorageAccountListResponse;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.model.vm.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class AzureSDKManagerImpl implements AzureSDKManager {
    private static final String PERSISTENT_VM_ROLE = "PersistentVMRole";
    private static final String NETWORK_CONFIGURATION = "NetworkConfiguration";
    public static final String PLATFORM_IMAGE = "Platform";
    public static final String USER_IMAGE = "User";
    private static final String WINDOWS_OS_TYPE = "Windows";
    private static final String LINUX_OS_TYPE = "Linux";
    private static final String WINDOWS_PROVISIONING_CONFIGURATION = "WindowsProvisioningConfiguration";
    private static final String LINUX_PROVISIONING_CONFIGURATION = "LinuxProvisioningConfiguration";

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
    public List<CloudService> getCloudServices(@NotNull String subscriptionId) throws AzureCmdException {
        List<CloudService> csList = new ArrayList<CloudService>();
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(subscriptionId);

            ArrayList<HostedService> hostedServices = getHostedServices(client).getHostedServices();

            if (hostedServices == null) {
                return csList;
            }

            for (HostedService hostedService : hostedServices) {
                ListenableFuture<DeploymentGetResponse> productionFuture = getDeploymentAsync(
                        client,
                        hostedService.getServiceName(),
                        DeploymentSlot.Production);
                ListenableFuture<DeploymentGetResponse> stagingFuture = getDeploymentAsync(
                        client,
                        hostedService.getServiceName(),
                        DeploymentSlot.Staging);

                DeploymentGetResponse prodDGR = productionFuture.get();
                String productionDeployment = prodDGR.getName();
                String stagingDeployment = stagingFuture.get().getName();

                CloudService cloudService = new CloudService(
                        hostedService.getServiceName() != null ? hostedService.getServiceName() : "",
                        hostedService.getProperties() != null && hostedService.getProperties().getLocation() != null ?
                                hostedService.getProperties().getLocation() :
                                "",
                        hostedService.getProperties() != null && hostedService.getProperties().getAffinityGroup() != null ?
                                hostedService.getProperties().getAffinityGroup() :
                                "",
                        productionDeployment != null ? productionDeployment : "",
                        stagingDeployment != null ? stagingDeployment : "",
                        subscriptionId);

                cloudService = loadAvailabilitySets(prodDGR, cloudService);

                csList.add(cloudService);
            }

            return csList;
        } catch (ExecutionException e) {
            throw new AzureCmdException("Error retrieving the Cloud Service list", e.getCause());
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Cloud Service list", t);
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
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId) throws AzureCmdException {
        List<VirtualMachine> vmList = new ArrayList<VirtualMachine>();
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(subscriptionId);

            ArrayList<HostedService> hostedServices = getHostedServices(client).getHostedServices();

            if (hostedServices == null) {
                return vmList;
            }

            for (HostedService hostedService : hostedServices) {
                String serviceName = hostedService.getServiceName() != null ? hostedService.getServiceName() : "";
                vmList = loadVirtualMachines(client, subscriptionId, serviceName, vmList);
            }

            return vmList;
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

            vm.setAvailabilitySet(vmRole.getAvailabilitySetName() != null ? vmRole.getAvailabilitySetName() : "");
            vm.setSize(vmRole.getRoleSize() != null ? vmRole.getRoleSize() : "");
            vm.setStatus(deployment.getStatus() != null ? deployment.getStatus().toString() : "");

            vm.getEndpoints().clear();
            vm = loadEndpoints(vmRole, vm);

            return vm;
        } catch (Throwable t) {
            throw new AzureCmdException("Error refreshing the VM information", t);
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
    public List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId) throws AzureCmdException {
        List<StorageAccount> saList = new ArrayList<StorageAccount>();
        StorageManagementClient client = null;

        try {
            client = getStorageManagementClient(subscriptionId);

            ArrayList<com.microsoft.windowsazure.management.storage.models.StorageAccount> storageAccounts =
                    getStorageAccounts(client).getStorageAccounts();

            if (storageAccounts == null) {
                return saList;
            }

            for (com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount : storageAccounts) {
                String key = "";

                if (storageAccount.getName() != null) {
                    String primaryKey = getStorageAccountKeys(client, storageAccount.getName()).getPrimaryKey();

                    if (primaryKey != null) {
                        key = primaryKey;
                    }
                }

                StorageAccount sa = new StorageAccount(
                        storageAccount.getName() != null ? storageAccount.getName() : "",
                        storageAccount.getProperties() != null && storageAccount.getProperties().getAccountType() != null ?
                                storageAccount.getProperties().getAccountType() :
                                "",
                        storageAccount.getProperties() != null && storageAccount.getProperties().getLocation() != null ?
                                storageAccount.getProperties().getLocation() :
                                "",
                        storageAccount.getProperties() != null && storageAccount.getProperties().getAffinityGroup() != null ?
                                storageAccount.getProperties().getAffinityGroup() :
                                "",
                        key,
                        subscriptionId);

                saList.add(sa);
            }

            return saList;
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
    @Override
    public List<VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId) throws AzureCmdException {
        List<VirtualMachineSize> vmSizeList = new ArrayList<VirtualMachineSize>();
        ManagementClient client = null;

        try {
            client = getManagementClient(subscriptionId);
            vmSizeList = loadVMSizes(client, vmSizeList);

            return vmSizeList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the VM Size list", t);
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
    public List<Location> getLocations(@NotNull String subscriptionId) throws AzureCmdException {
        List<Location> locationList = new ArrayList<Location>();
        ManagementClient client = null;

        try {
            client = getManagementClient(subscriptionId);
            locationList = loadLocations(client, locationList);

            return locationList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Location list", t);
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
    public List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId) throws AzureCmdException {
        List<AffinityGroup> affinityGroupList = new ArrayList<AffinityGroup>();
        ManagementClient client = null;

        try {
            client = getManagementClient(subscriptionId);
            affinityGroupList = loadAffinityGroups(client, affinityGroupList);

            return affinityGroupList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Affinity Group list", t);
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
    public void createStorageAccount(@NotNull StorageAccount storageAccount) throws AzureCmdException {
        StorageManagementClient client = null;

        try {
            client = getStorageManagementClient(storageAccount.getSubscriptionId());
            StorageAccountOperations sao = getStorageAccountOperations(client);
            StorageAccountCreateParameters sacp = new StorageAccountCreateParameters(storageAccount.getName(),
                    storageAccount.getName());
            sacp.setAccountType(storageAccount.getType());
            if (!storageAccount.getAffinityGroup().isEmpty()) {
                sacp.setAffinityGroup(storageAccount.getAffinityGroup());
            } else if (!storageAccount.getLocation().isEmpty()) {
                sacp.setLocation(storageAccount.getLocation());
            }

            OperationStatusResponse osr = sao.create(sacp);
            validateOperationStatus(osr);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Storage Account", t);
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
    public void createCloudService(@NotNull CloudService cloudService) throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(cloudService.getSubscriptionId());
            HostedServiceOperations hso = getHostedServiceOperations(client);
            HostedServiceCreateParameters hscp = new HostedServiceCreateParameters(cloudService.getName(),
                    cloudService.getName());

            if (!cloudService.getAffinityGroup().isEmpty()) {
                hscp.setAffinityGroup(cloudService.getAffinityGroup());
            } else if (!cloudService.getLocation().isEmpty()) {
                hscp.setLocation(cloudService.getLocation());
            }

            OperationResponse or = hso.create(hscp);

            if (or == null) {
                throw new Exception("Unable to retrieve Operation");
            }

            OperationStatusResponse osr = getOperationStatusResponse(client, or);
            validateOperationStatus(osr);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Cloud Service", t);
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
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull StorageAccount storageAccount, @NotNull String username,
                                     @NotNull String password)
            throws AzureCmdException {
        try {
            String mediaLocation = getMediaLocation(virtualMachine, storageAccount);

            createVirtualMachine(virtualMachine, vmImage, mediaLocation, username, password);
        } catch (AzureCmdException e) {
            throw e;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the VM", t);
        }
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull String mediaLocation, @NotNull String username, @NotNull String password)
            throws AzureCmdException {

        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(virtualMachine.getSubscriptionId());
            VirtualMachineOperations vmo = getVirtualMachineOperations(client);

            if (!virtualMachine.getDeploymentName().isEmpty()) {
                createVM(vmo, virtualMachine, vmImage, mediaLocation, username, password);
            } else {
                createVMDeployment(vmo, virtualMachine, vmImage, mediaLocation, username, password);
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the VM", t);
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
    private static StorageManagementClient getStorageManagementClient(@NotNull String subscriptionId) throws Exception {
        StorageManagementClient client = AzureSDKHelper.getStorageManagementClient(subscriptionId);

        if (client == null) {
            throw new Exception("Unable to instantiate Storage Management client");
        }

        return client;
    }

    @NotNull
    private static ManagementClient getManagementClient(@NotNull String subscriptionId) throws Exception {
        ManagementClient client = AzureSDKHelper.getManagementClient(subscriptionId);

        if (client == null) {
            throw new Exception("Unable to instantiate Management client");
        }

        return client;
    }

    @NotNull
    private static HostedServiceOperations getHostedServiceOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        HostedServiceOperations hso = client.getHostedServicesOperations();

        if (hso == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hso;
    }

    @NotNull
    private static DeploymentOperations getDeploymentOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        DeploymentOperations dop = client.getDeploymentsOperations();

        if (dop == null) {
            throw new Exception("Unable to retrieve Deployment information");
        }

        return dop;
    }

    @NotNull
    private static VirtualMachineOperations getVirtualMachineOperations(@NotNull ComputeManagementClient client)
            throws Exception {
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

    @NotNull
    private static RoleSizeOperations getRoleSizeOperations(@NotNull ManagementClient client)
            throws Exception {
        RoleSizeOperations rso = client.getRoleSizesOperations();

        if (rso == null) {
            throw new Exception("Unable to retrieve Role Sizes information");
        }

        return rso;
    }

    @NotNull
    private static LocationOperations getLocationsOperations(@NotNull ManagementClient client)
            throws Exception {
        LocationOperations lo = client.getLocationsOperations();

        if (lo == null) {
            throw new Exception("Unable to retrieve Locations information");
        }

        return lo;
    }

    @NotNull
    private static AffinityGroupOperations getAffinityGroupOperations(@NotNull ManagementClient client)
            throws Exception {
        AffinityGroupOperations ago = client.getAffinityGroupsOperations();

        if (ago == null) {
            throw new Exception("Unable to retrieve Affinity Groups information");
        }

        return ago;
    }

    @NotNull
    private static StorageAccountOperations getStorageAccountOperations(@NotNull StorageManagementClient client)
            throws Exception {
        StorageAccountOperations sao = client.getStorageAccountsOperations();

        if (sao == null) {
            throw new Exception("Unable to retrieve Storage Accounts information");
        }

        return sao;
    }

    @NotNull
    private HostedServiceListResponse getHostedServices(@NotNull ComputeManagementClient client) throws Exception {
        HostedServiceOperations hso = getHostedServiceOperations(client);

        HostedServiceListResponse hslr = hso.list();

        if (hslr == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hslr;
    }

    @NotNull
    private static ListenableFuture<DeploymentGetResponse> getDeploymentAsync(@NotNull final ComputeManagementClient client,
                                                                              @NotNull final String serviceName,
                                                                              @NotNull final DeploymentSlot slot) {
        final SettableFuture<DeploymentGetResponse> future = SettableFuture.create();

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(getDeployment(client, serviceName, slot));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });

        return future;
    }

    @NotNull
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client,
                                                       @NotNull String serviceName,
                                                       @NotNull DeploymentSlot slot)
            throws Exception {
        try {
            DeploymentGetResponse dgr = getDeploymentOperations(client).getBySlot(serviceName, slot);

            if (dgr == null) {
                throw new Exception("Unable to retrieve Deployment information");
            }

            return dgr;
        } catch (ServiceException se) {
            if (se.getHttpStatusCode() == 404) {
                return new DeploymentGetResponse();
            } else {
                throw se;
            }
        }
    }

    @NotNull
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client,
                                                       @NotNull String serviceName,
                                                       @NotNull String deploymentName)
            throws Exception {
        try {
            DeploymentGetResponse dgr = getDeploymentOperations(client).getByName(serviceName, deploymentName);

            if (dgr == null) {
                throw new Exception("Unable to retrieve Deployment information");
            }

            return dgr;
        } catch (ServiceException se) {
            if (se.getHttpStatusCode() == 404) {
                return new DeploymentGetResponse();
            } else {
                throw se;
            }
        }
    }

    @NotNull
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client,
                                                       @NotNull VirtualMachine vm)
            throws Exception {
        return getDeployment(client, vm.getServiceName(), vm.getDeploymentName());
    }

    @NotNull
    private StorageAccountListResponse getStorageAccounts(@NotNull StorageManagementClient client) throws Exception {
        StorageAccountListResponse salr = getStorageAccountOperations(client).list();

        if (salr == null) {
            throw new Exception("Unable to retrieve Storage Accounts information");
        }

        return salr;
    }

    @NotNull
    private StorageAccountGetKeysResponse getStorageAccountKeys(@NotNull StorageManagementClient client,
                                                                @NotNull String storageName)
            throws Exception {
        StorageAccountGetKeysResponse sagkr = getStorageAccountOperations(client).getKeys(storageName);

        if (sagkr == null) {
            throw new Exception("Unable to retrieve Storage Account Keys information");
        }

        return sagkr;
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

    @Nullable
    private static OperationStatusResponse getOperationStatusResponse(@NotNull ComputeManagementClient client,
                                                                      @NotNull OperationResponse or)
            throws InterruptedException, java.util.concurrent.ExecutionException, ServiceException {
        OperationStatusResponse osr = client.getOperationStatusAsync(or.getRequestId()).get();
        int delayInSeconds = 30;

        if (client.getLongRunningOperationInitialTimeout() >= 0) {
            delayInSeconds = client.getLongRunningOperationInitialTimeout();
        }

        while (osr.getStatus() == OperationStatus.InProgress) {
            Thread.sleep(delayInSeconds * 1000);
            osr = client.getOperationStatusAsync(or.getRequestId()).get();
            delayInSeconds = 30;

            if (client.getLongRunningOperationRetryTimeout() >= 0) {
                delayInSeconds = client.getLongRunningOperationRetryTimeout();
            }
        }

        if (osr.getStatus() != OperationStatus.Succeeded) {
            if (osr.getError() != null) {
                ServiceException ex = new ServiceException(osr.getError().getCode() + " : " + osr.getError().getMessage());
                ex.setErrorCode(osr.getError().getCode());
                ex.setErrorMessage(osr.getError().getMessage());
                throw ex;
            } else {
                throw new ServiceException("");
            }
        }

        return osr;
    }

    @NotNull
    private static CloudService loadAvailabilitySets(@NotNull DeploymentGetResponse deployment,
                                                     @NotNull CloudService cloudService)
            throws Exception {
        if (deployment.getRoles() != null) {
            Set<String> availabilitySets = cloudService.getAvailabilitySets();

            for (Role role : deployment.getRoles()) {
                if (role.getRoleType() != null &&
                        role.getRoleType().equals(PERSISTENT_VM_ROLE) &&
                        role.getAvailabilitySetName() != null &&
                        !role.getAvailabilitySetName().isEmpty()) {
                    availabilitySets.add(role.getAvailabilitySetName());
                }
            }
        }

        return cloudService;
    }

    @NotNull
    private static List<VirtualMachine> loadVirtualMachines(@NotNull ComputeManagementClient client,
                                                            @NotNull String subscriptionId,
                                                            @NotNull String serviceName,
                                                            @NotNull List<VirtualMachine> vmList)
            throws Exception {
        DeploymentGetResponse deployment = getDeployment(client, serviceName, DeploymentSlot.Production);

        if (deployment.getRoles() == null) {
            return vmList;
        }

        for (Role role : deployment.getRoles()) {
            if (role.getRoleType() != null
                    && role.getRoleType().equals(PERSISTENT_VM_ROLE)) {
                VirtualMachine vm = new VirtualMachine(
                        role.getRoleName() != null ? role.getRoleName() : "",
                        serviceName,
                        deployment.getName() != null ? deployment.getName() : "",
                        role.getAvailabilitySetName() != null ? role.getAvailabilitySetName() : "",
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
    private static String getMediaLocation(@NotNull VirtualMachine virtualMachine,
                                           @NotNull StorageAccount storageAccount)
            throws URISyntaxException, InvalidKeyException, StorageException {
        Calendar calendar = GregorianCalendar.getInstance();
        String blobName = String.format("%s-%s-%04d-%02d-%02d.vhd",
                virtualMachine.getServiceName(),
                virtualMachine.getName(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE));

        CloudStorageAccount csa = CloudStorageAccount.parse("DefaultEndpointsProtocol=http;" +
                "AccountName=" + storageAccount.getName() + ";" +
                "AccountKey=" + storageAccount.getKey());

        CloudBlobContainer container = csa.createCloudBlobClient().getContainerReference("vhds");
        container.createIfNotExists();

        return container.getUri().toString() + "/" + blobName;
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
                                PLATFORM_IMAGE,
                                osImage.getCategory() != null ? osImage.getCategory() : "",
                                osImage.getPublisherName() != null ? osImage.getPublisherName() : "",
                                osImage.getPublishedDate() != null ?
                                        osImage.getPublishedDate() :
                                        GregorianCalendar.getInstance(),
                                osImage.getLabel() != null ? osImage.getLabel() : "",
                                osImage.getDescription() != null ? osImage.getDescription() : "",
                                osImage.getOperatingSystemType() != null ? osImage.getOperatingSystemType() : "",
                                osImage.getLocation() != null ? osImage.getLocation() : "",
                                osImage.getEula() != null ? osImage.getEula() : "",
                                osImage.getPrivacyUri() != null ? osImage.getPrivacyUri().toString() : "",
                                osImage.getPricingDetailUri() != null ? osImage.getPricingDetailUri().toString() : "",
                                osImage.getRecommendedVMSize() != null ? osImage.getRecommendedVMSize() : "",
                                osImage.isShowInGui() != null ? osImage.isShowInGui() : true));
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
                                USER_IMAGE,
                                vmImage.getCategory() != null ? vmImage.getCategory() : "",
                                vmImage.getPublisherName() != null ? vmImage.getPublisherName() : "",
                                vmImage.getPublishedDate() != null ?
                                        vmImage.getPublishedDate() :
                                        GregorianCalendar.getInstance(),
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
                                vmImage.getRecommendedVMSize() != null ? vmImage.getRecommendedVMSize() : "",
                                vmImage.isShowInGui() != null ? vmImage.isShowInGui() : true));
            }
        }

        return vmImageList;
    }

    @NotNull
    private static List<VirtualMachineSize> loadVMSizes(@NotNull ManagementClient client,
                                                        @NotNull List<VirtualMachineSize> vmSizeList)
            throws Exception {
        RoleSizeListResponse rslr = getRoleSizeOperations(client).list();

        if (rslr == null) {
            throw new Exception("Unable to retrieve Role Sizes information");
        }

        if (rslr.getRoleSizes() != null) {
            for (RoleSize rs : rslr.getRoleSizes()) {
                if (rs.isSupportedByVirtualMachines()) {
                    vmSizeList.add(
                            new VirtualMachineSize(
                                    rs.getName() != null ? rs.getName() : "",
                                    rs.getLabel() != null ? rs.getLabel() : "",
                                    rs.getCores(),
                                    rs.getMemoryInMb()
                            ));
                }
            }
        }

        return vmSizeList;
    }

    @NotNull
    private static List<Location> loadLocations(@NotNull ManagementClient client,
                                                @NotNull List<Location> locationList)
            throws Exception {
        LocationsListResponse llr = getLocationsOperations(client).list();

        if (llr == null) {
            throw new Exception("Unable to retrieve Locations information");
        }

        if (llr.getLocations() != null) {
            for (LocationsListResponse.Location location : llr.getLocations()) {
                locationList.add(
                        new Location(
                                location.getName() != null ? location.getName() : "",
                                location.getDisplayName() != null ? location.getDisplayName() : ""
                        ));
            }
        }

        return locationList;
    }

    @NotNull
    private static List<AffinityGroup> loadAffinityGroups(@NotNull ManagementClient client,
                                                          @NotNull List<AffinityGroup> affinityGroupList)
            throws Exception {
        AffinityGroupListResponse aglr = getAffinityGroupOperations(client).list();

        if (aglr == null) {
            throw new Exception("Unable to retrieve Affinity Groups information");
        }

        if (aglr.getAffinityGroups() != null) {
            for (AffinityGroupListResponse.AffinityGroup ag : aglr.getAffinityGroups()) {
                affinityGroupList.add(
                        new AffinityGroup(
                                ag.getName() != null ? ag.getName() : "",
                                ag.getLabel() != null ? ag.getLabel() : "",
                                ag.getLocation() != null ? ag.getLocation() : ""
                        ));
            }
        }

        return affinityGroupList;
    }

    private static void createVM(@NotNull VirtualMachineOperations vmo,
                                 @NotNull VirtualMachine virtualMachine,
                                 @NotNull VirtualMachineImage vmImage,
                                 @NotNull String mediaLocation,
                                 @NotNull String username,
                                 @NotNull String password)
            throws Exception {
        VirtualMachineCreateParameters vmcp = new VirtualMachineCreateParameters(virtualMachine.getName());

        if (!virtualMachine.getAvailabilitySet().isEmpty()) {
            vmcp.setAvailabilitySetName(virtualMachine.getAvailabilitySet());
        }

        if (vmImage.getType().equals(USER_IMAGE)) {
            vmcp.setVMImageName(vmImage.getName());
            vmcp.setMediaLocation(new URI(mediaLocation));
        } else if (vmImage.getType().equals(PLATFORM_IMAGE)) {
            OSVirtualHardDisk osVHD = new OSVirtualHardDisk();
            osVHD.setSourceImageName(vmImage.getName());
            osVHD.setMediaLink(new URI(mediaLocation));
            vmcp.setOSVirtualHardDisk(osVHD);
        }

        vmcp.setRoleSize(virtualMachine.getSize());

        vmcp.getConfigurationSets().add(getProvisioningConfigurationSet(virtualMachine, vmImage, username, password));

        if (virtualMachine.getEndpoints().size() > 0) {
            vmcp.getConfigurationSets().add(getNetworkConfigurationSet(virtualMachine));
        }

        OperationStatusResponse osr = vmo.create(virtualMachine.getServiceName(), virtualMachine.getDeploymentName(), vmcp);

        validateOperationStatus(osr);
    }

    private static void createVMDeployment(@NotNull VirtualMachineOperations vmo,
                                           @NotNull VirtualMachine virtualMachine,
                                           @NotNull VirtualMachineImage vmImage,
                                           @NotNull String mediaLocation,
                                           @NotNull String username,
                                           @NotNull String password) throws Exception {
        VirtualMachineCreateDeploymentParameters vmcdp = new VirtualMachineCreateDeploymentParameters();
        vmcdp.setName(virtualMachine.getName());
        vmcdp.setLabel(virtualMachine.getName());
        vmcdp.setDeploymentSlot(DeploymentSlot.Production);

        Role role = new Role();
        role.setRoleName(virtualMachine.getName());

        if (!virtualMachine.getAvailabilitySet().isEmpty()) {
            role.setAvailabilitySetName(virtualMachine.getAvailabilitySet());
        }

        if (vmImage.getType().equals("User")) {
            role.setVMImageName(vmImage.getName());
            role.setMediaLocation(new URI(mediaLocation));
        } else if (vmImage.getType().equals("Platform")) {
            OSVirtualHardDisk osVHD = new OSVirtualHardDisk();
            osVHD.setSourceImageName(vmImage.getName());
            osVHD.setMediaLink(new URI(mediaLocation));
            role.setOSVirtualHardDisk(osVHD);
        }

        role.setRoleSize(virtualMachine.getSize());
        role.setRoleType(PERSISTENT_VM_ROLE);

        role.getConfigurationSets().add(getProvisioningConfigurationSet(virtualMachine, vmImage, username, password));

        if (virtualMachine.getEndpoints().size() > 0) {
            role.getConfigurationSets().add(getNetworkConfigurationSet(virtualMachine));
        }

        vmcdp.getRoles().add(role);

        OperationStatusResponse osr = vmo.createDeployment(virtualMachine.getServiceName(), vmcdp);

        validateOperationStatus(osr);
    }

    @NotNull
    private static ConfigurationSet getProvisioningConfigurationSet(@NotNull VirtualMachine virtualMachine,
                                                                    @NotNull VirtualMachineImage vmImage,
                                                                    @NotNull String username,
                                                                    @NotNull String password) {
        ConfigurationSet provConfSet = new ConfigurationSet();

        if (vmImage.getOperatingSystemType().equals(WINDOWS_OS_TYPE)) {
            provConfSet.setConfigurationSetType(WINDOWS_PROVISIONING_CONFIGURATION);
            provConfSet.setAdminUserName(username);
            provConfSet.setAdminPassword(password);
            provConfSet.setComputerName(String.format("%s-%s-%02d",
                    virtualMachine.getServiceName().substring(0, 5),
                    virtualMachine.getName().substring(0, 5),
                    1));
        } else if (vmImage.getOperatingSystemType().equals(LINUX_OS_TYPE)) {
            provConfSet.setConfigurationSetType(LINUX_PROVISIONING_CONFIGURATION);
            provConfSet.setUserName(username);
            provConfSet.setUserPassword(password);
            provConfSet.setDisableSshPasswordAuthentication(false);
            provConfSet.setHostName(String.format("%s-%s-%02d",
                    virtualMachine.getServiceName().substring(0, 5),
                    virtualMachine.getName().substring(0, 5),
                    1));
        }

        return provConfSet;
    }

    @NotNull
    private static ConfigurationSet getNetworkConfigurationSet(@NotNull VirtualMachine virtualMachine) {
        ConfigurationSet netConfSet = new ConfigurationSet();
        netConfSet.setConfigurationSetType(NETWORK_CONFIGURATION);
        ArrayList<InputEndpoint> inputEndpoints = netConfSet.getInputEndpoints();

        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
            InputEndpoint inputEndpoint = new InputEndpoint();
            inputEndpoint.setName(endpoint.getName());
            inputEndpoint.setProtocol(endpoint.getProtocol());
            inputEndpoint.setLocalPort(endpoint.getPrivatePort());
            inputEndpoint.setPort(endpoint.getPublicPort());

            inputEndpoints.add(inputEndpoint);
        }

        return netConfSet;
    }
}