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
package com.microsoftopentechnologies.intellij.helpers.azure.sdk;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.azure.storage.queue.QueueListingDetails;
import com.microsoft.azure.storage.table.*;
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
import com.microsoft.windowsazure.management.network.NetworkManagementClient;
import com.microsoft.windowsazure.management.network.NetworkOperations;
import com.microsoft.windowsazure.management.network.models.NetworkListResponse;
import com.microsoft.windowsazure.management.network.models.NetworkListResponse.Subnet;
import com.microsoft.windowsazure.management.network.models.NetworkListResponse.VirtualNetworkSite;
import com.microsoft.windowsazure.management.storage.StorageAccountOperations;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoft.windowsazure.management.storage.models.*;
import com.microsoft.azure.storage.core.Base64;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.CallableSingleArg;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.*;
import com.microsoftopentechnologies.intellij.model.storage.Queue;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.TableEntity;
import com.microsoftopentechnologies.intellij.model.storage.TableEntity.Property;
import com.microsoftopentechnologies.intellij.model.vm.*;
import com.microsoftopentechnologies.intellij.model.vm.CloudService.Deployment;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine.Status;

import javax.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

public class AzureSDKManagerImpl implements AzureSDKManager {

    private static class StatusLiterals {
        private static final String UNKNOWN = "Unknown";
        private static final String READY_ROLE = "ReadyRole";
        private static final String STOPPED_VM = "StoppedVM";
        private static final String STOPPED_DEALLOCATED = "StoppedDeallocated";
        private static final String BUSY_ROLE = "BusyRole";
        private static final String CREATING_VM = "CreatingVM";
        private static final String CREATING_ROLE = "CreatingRole";
        private static final String STARTING_VM = "StartingVM";
        private static final String STARTING_ROLE = "StartingRole";
        private static final String STOPPING_VM = "StoppingVM";
        private static final String STOPPING_ROLE = "StoppingRole";
        private static final String DELETING_VM = "DeletingVM";
        private static final String RESTARTING_ROLE = "RestartingRole";
        private static final String CYCLING_ROLE = "CyclingRole";
        private static final String FAILED_STARTING_VM = "FailedStartingVM";
        private static final String FAILED_STARTING_ROLE = "FailedStartingRole";
        private static final String UNRESPONSIVE_ROLE = "UnresponsiveRole";
        private static final String PREPARING = "Preparing";
    }

    private static final String PERSISTENT_VM_ROLE = "PersistentVMRole";
    private static final String NETWORK_CONFIGURATION = "NetworkConfiguration";
    private static final String PLATFORM_IMAGE = "Platform";
    private static final String USER_IMAGE = "User";
    private static final String WINDOWS_OS_TYPE = "Windows";
    private static final String LINUX_OS_TYPE = "Linux";
    private static final String WINDOWS_PROVISIONING_CONFIGURATION = "WindowsProvisioningConfiguration";
    private static final String LINUX_PROVISIONING_CONFIGURATION = "LinuxProvisioningConfiguration";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static AzureSDKManager apiManager;
    private static AzureSDKManager apiManagerADAuth;

    private AzureSDKManagerImpl() {
    }


    public static AzureSDKManager getManager() {
        if (apiManager == null) {
            apiManagerADAuth = new AzureSDKManagerADAuthDecorator(
                    apiManager = new AzureSDKManagerImpl());
        }

        if (AzureRestAPIManagerImpl.getManager().getAuthenticationMode() == AzureAuthenticationMode.ActiveDirectory) {
            return apiManagerADAuth;
        } else {
            return apiManager;
        }
    }


    @Override
    public List<CloudService> getCloudServices(String subscriptionId) throws AzureCmdException {
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

                DeploymentGetResponse stagingDGR = stagingFuture.get();

                CloudService cloudService = new CloudService(
                        hostedService.getServiceName() != null ? hostedService.getServiceName() : "",
                        hostedService.getProperties() != null && hostedService.getProperties().getLocation() != null ?
                                hostedService.getProperties().getLocation() :
                                "",
                        hostedService.getProperties() != null && hostedService.getProperties().getAffinityGroup() != null ?
                                hostedService.getProperties().getAffinityGroup() :
                                "",
                        subscriptionId);

                loadDeployment(prodDGR, cloudService);

                cloudService = loadDeployment(prodDGR, cloudService);
                cloudService = loadDeployment(stagingDGR, cloudService);

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


    @Override
    public List<VirtualMachine> getVirtualMachines(String subscriptionId) throws AzureCmdException {
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

    public VirtualMachine refreshVirtualMachineInformation(VirtualMachine vm) throws AzureCmdException {
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

            vm.setDeploymentName(deployment.getName() != null ? deployment.getName() : "");
            vm.setAvailabilitySet(vmRole.getAvailabilitySetName() != null ? vmRole.getAvailabilitySetName() : "");
            vm.setSize(vmRole.getRoleSize() != null ? vmRole.getRoleSize() : "");
            vm.setStatus(getVMStatus(deployment, vmRole));

            vm.getEndpoints().clear();
            vm = loadNetworkConfiguration(vmRole, vm);

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
    public void startVirtualMachine(VirtualMachine vm) throws AzureCmdException {
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
    public void shutdownVirtualMachine(VirtualMachine vm, boolean deallocate) throws AzureCmdException {
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
    public void restartVirtualMachine(VirtualMachine vm) throws AzureCmdException {
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
    public void deleteVirtualMachine(VirtualMachine vm, boolean deleteFromStorage) throws AzureCmdException {
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


    @Override
    public byte[] downloadRDP(VirtualMachine vm) throws AzureCmdException {
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


    @Override
    public List<StorageAccount> getStorageAccounts(String subscriptionId) throws AzureCmdException {
        List<StorageAccount> saList = new ArrayList<StorageAccount>();
        StorageManagementClient client = null;

        try {
            client = getStorageManagementClient(subscriptionId);

            ArrayList<com.microsoft.windowsazure.management.storage.models.StorageAccount> storageAccounts =
                    getStorageAccounts(client).getStorageAccounts();

            if (storageAccounts == null) {
                return saList;
            }

            List<ListenableFuture<StorageAccount>> saFutureList = new ArrayList<ListenableFuture<StorageAccount>>();

            for (com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount : storageAccounts) {
                saFutureList.add(getStorageAccountAsync(subscriptionId, client, storageAccount));
            }

            saList.addAll(Futures.allAsList(saFutureList).get());

            return saList;
        } catch (ExecutionException e) {
            throw new AzureCmdException("Error retrieving Storage Accounts list", e.getCause());
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving Storage Accounts list", t);
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
    public List<VirtualMachineImage> getVirtualMachineImages(String subscriptionId) throws AzureCmdException {
        List<VirtualMachineImage> vmImageList = new ArrayList<VirtualMachineImage>();
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(subscriptionId);
            ListenableFuture<List<VirtualMachineImage>> osImagesFuture = getOSImagesAsync(client);
            ListenableFuture<List<VirtualMachineImage>> vmImagesFuture = getVMImagesAsync(client);
            vmImageList.addAll(osImagesFuture.get());
            vmImageList.addAll(vmImagesFuture.get());

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


    @Override
    public List<VirtualMachineSize> getVirtualMachineSizes(String subscriptionId) throws AzureCmdException {
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


    @Override
    public List<Location> getLocations(String subscriptionId) throws AzureCmdException {
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


    @Override
    public List<AffinityGroup> getAffinityGroups(String subscriptionId) throws AzureCmdException {
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
    public List<VirtualNetwork> getVirtualNetworks(String subscriptionId) throws AzureCmdException {
        List<VirtualNetwork> vnList = new ArrayList<VirtualNetwork>();
        NetworkManagementClient client = null;

        try {
            client = getNetworkManagementClient(subscriptionId);

            ArrayList<VirtualNetworkSite> virtualNetworkSites =
                    getNetworks(client).getVirtualNetworkSites();

            if (virtualNetworkSites == null) {
                return vnList;
            }

            for (VirtualNetworkSite virtualNetworkSite : virtualNetworkSites) {
                VirtualNetwork vn = new VirtualNetwork(
                        virtualNetworkSite.getName() != null ? virtualNetworkSite.getName() : "",
                        virtualNetworkSite.getId() != null ? virtualNetworkSite.getId() : "",
                        virtualNetworkSite.getLocation() != null ? virtualNetworkSite.getLocation() : "",
                        virtualNetworkSite.getAffinityGroup() != null ? virtualNetworkSite.getAffinityGroup() : "",
                        subscriptionId);

                if (virtualNetworkSite.getSubnets() != null) {
                    Set<String> vnSubnets = vn.getSubnets();

                    for (Subnet subnet : virtualNetworkSite.getSubnets()) {
                        if (subnet.getName() != null && !subnet.getName().isEmpty()) {
                            vnSubnets.add(subnet.getName());
                        }
                    }
                }

                vnList.add(vn);
            }

            return vnList;
        } catch (ExecutionException e) {
            throw new AzureCmdException("Error retrieving Virtual Networks list", e.getCause());
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving Virtual Networks list", t);
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
    public void createStorageAccount(StorageAccount storageAccount) throws AzureCmdException {
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
    public void createCloudService(CloudService cloudService) throws AzureCmdException {
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
    public void createVirtualMachine(VirtualMachine virtualMachine, VirtualMachineImage vmImage,
                                     StorageAccount storageAccount, String virtualNetwork,
                                     String username, String password, byte[] certificate)
            throws AzureCmdException {
        try {
            String mediaLocation = getMediaLocation(virtualMachine, storageAccount);

            createVirtualMachine(virtualMachine, vmImage, mediaLocation, virtualNetwork, username, password, certificate);
        } catch (AzureCmdException e) {
            throw e;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the VM", t);
        }
    }

    @Override
    public void createVirtualMachine(VirtualMachine virtualMachine, VirtualMachineImage vmImage,
                                     String mediaLocation, String virtualNetwork,
                                     String username, String password, byte[] certificate)
            throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            client = getComputeManagementClient(virtualMachine.getSubscriptionId());
            VirtualMachineOperations vmo = getVirtualMachineOperations(client);

            if (virtualMachine.getDeploymentName().isEmpty()) {
                createVMDeployment(vmo, virtualMachine, vmImage, mediaLocation, virtualNetwork, username, password, certificate);
            } else {
                createVM(vmo, virtualMachine, vmImage, mediaLocation, username, password, certificate);
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

    @Override

    public StorageAccount refreshStorageAccountInformation(StorageAccount storageAccount)
            throws AzureCmdException {
        StorageManagementClient client = null;

        try {
            client = getStorageManagementClient(storageAccount.getSubscriptionId());
            StorageAccountOperations sao = getStorageAccountOperations(client);
            StorageAccountGetResponse sagr = sao.get(storageAccount.getName());

            if (sagr == null) {
                throw new Exception("Unable to retrieve Operation");
            }

            OperationStatusResponse osr = getOperationStatusResponse(client, sagr);
            validateOperationStatus(osr);

            if (sagr.getStorageAccount() == null) {
                throw new Exception("Invalid Storage Account information. No Storage Account matches the specified data.");
            }

            StorageAccount sa = getStorageAccount(storageAccount.getSubscriptionId(), client, sagr.getStorageAccount());
            storageAccount.setType(sa.getType());
            storageAccount.setDescription(sa.getDescription());
            storageAccount.setLabel(sa.getLabel());
            storageAccount.setStatus(sa.getStatus());
            storageAccount.setLocation(sa.getLocation());
            storageAccount.setAffinityGroup(sa.getAffinityGroup());
            storageAccount.setPrimaryKey(sa.getPrimaryKey());
            storageAccount.setSecondaryKey(sa.getSecondaryKey());
            storageAccount.setManagementUri(sa.getManagementUri());
            storageAccount.setBlobsUri(sa.getBlobsUri());
            storageAccount.setQueuesUri(sa.getQueuesUri());
            storageAccount.setTablesUri(sa.getTablesUri());
            storageAccount.setPrimaryRegion(sa.getPrimaryRegion());
            storageAccount.setPrimaryRegionStatus(sa.getPrimaryRegionStatus());
            storageAccount.setSecondaryRegion(sa.getSecondaryRegion());
            storageAccount.setSecondaryRegionStatus(sa.getSecondaryRegionStatus());
            storageAccount.setLastFailover(sa.getLastFailover());

            return storageAccount;
        } catch (Throwable t) {
            throw new AzureCmdException("Error refreshing the Storage Account information", t);
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
    public String createServiceCertificate(String subscriptionId, String serviceName,
                                           byte[] data, String password)
            throws AzureCmdException {
        ComputeManagementClient client = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            X509Certificate cert = X509Certificate.getInstance(data);
            md.update(cert.getEncoded());
            String thumbprint = bytesToHex(md.digest());

            client = getComputeManagementClient(subscriptionId);

            ServiceCertificateOperations sco = getServiceCertificateOperations(client);
            ServiceCertificateCreateParameters sccp = new ServiceCertificateCreateParameters(data, CertificateFormat.Pfx);
            sccp.setPassword(password);

            OperationStatusResponse osr = sco.create(serviceName, sccp);
            validateOperationStatus(osr);

            return thumbprint;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Service Certificate", t);
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
    public void deleteStorageAccount(StorageAccount storageAccount) throws AzureCmdException {
        StorageManagementClient client = null;

        try {
            client = getStorageManagementClient(storageAccount.getSubscriptionId());
            StorageAccountOperations sao = getStorageAccountOperations(client);

            OperationResponse or = sao.delete(storageAccount.getName());
            OperationStatusResponse osr = getOperationStatusResponse(client, or);
            validateOperationStatus(osr);
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Storage Account", t);
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
    public List<BlobContainer> getBlobContainers(StorageAccount storageAccount)
            throws AzureCmdException {
        List<BlobContainer> bcList = new ArrayList<BlobContainer>();

        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);

            for (CloudBlobContainer container : client.listContainers(null, ContainerListingDetails.ALL, null, null)) {
                String uri = container.getUri() != null ? container.getUri().toString() : "";
                String eTag = "";
                Calendar lastModified = new GregorianCalendar();
                BlobContainerProperties properties = container.getProperties();

                if (properties != null) {
                    eTag = Strings.nullToEmpty(properties.getEtag());

                    if (properties.getLastModified() != null) {
                        lastModified.setTime(properties.getLastModified());
                    }
                }

                String publicReadAccessType = "";
                BlobContainerPermissions blobContainerPermissions = container.downloadPermissions();

                if (blobContainerPermissions != null && blobContainerPermissions.getPublicAccess() != null) {
                    publicReadAccessType = blobContainerPermissions.getPublicAccess().toString();
                }

                bcList.add(new BlobContainer(Strings.nullToEmpty(container.getName()),
                        uri,
                        eTag,
                        lastModified,
                        publicReadAccessType,
                        storageAccount.getSubscriptionId()));
            }

            return bcList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Blob Container list", t);
        }
    }


    @Override
    public BlobContainer createBlobContainer(StorageAccount storageAccount,
                                             BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            container.createIfNotExists();
            container.downloadAttributes();

            String uri = container.getUri() != null ? container.getUri().toString() : "";
            String eTag = "";
            Calendar lastModified = new GregorianCalendar();
            BlobContainerProperties properties = container.getProperties();

            if (properties != null) {
                eTag = Strings.nullToEmpty(properties.getEtag());

                if (properties.getLastModified() != null) {
                    lastModified.setTime(properties.getLastModified());
                }
            }

            String publicReadAccessType = "";
            BlobContainerPermissions blobContainerPermissions = container.downloadPermissions();

            if (blobContainerPermissions != null && blobContainerPermissions.getPublicAccess() != null) {
                publicReadAccessType = blobContainerPermissions.getPublicAccess().toString();
            }

            blobContainer.setUri(uri);
            blobContainer.setETag(eTag);
            blobContainer.setLastModified(lastModified);
            blobContainer.setPublicReadAccessType(publicReadAccessType);

            return blobContainer;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Blob Container", t);
        }
    }

    @Override
    public void deleteBlobContainer(StorageAccount storageAccount, BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            container.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Blob Container", t);
        }
    }


    @Override
    public BlobDirectory getRootDirectory(StorageAccount storageAccount, BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            CloudBlobDirectory directory = container.getDirectoryReference("");

            String uri = directory.getUri() != null ? directory.getUri().toString() : "";

            return new BlobDirectory("", uri, blobContainer.getName(), "", storageAccount.getSubscriptionId());
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the root Blob Directory", t);
        }
    }


    @Override
    public List<BlobItem> getBlobItems(StorageAccount storageAccount, BlobDirectory blobDirectory)
            throws AzureCmdException {
        List<BlobItem> biList = new ArrayList<BlobItem>();

        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = blobDirectory.getContainerName();
            String subscriptionId = storageAccount.getSubscriptionId();
            String delimiter = client.getDirectoryDelimiter();

            CloudBlobContainer container = client.getContainerReference(containerName);
            CloudBlobDirectory directory = container.getDirectoryReference(blobDirectory.getPath());

            for (ListBlobItem item : directory.listBlobs()) {
                String uri = item.getUri() != null ? item.getUri().toString() : "";

                if (item instanceof CloudBlobDirectory) {
                    CloudBlobDirectory subDirectory = (CloudBlobDirectory) item;

                    String name = extractBlobItemName(subDirectory.getPrefix(), delimiter);
                    String path = Strings.nullToEmpty(subDirectory.getPrefix());

                    biList.add(new BlobDirectory(name, uri, containerName, path, subscriptionId));
                } else if (item instanceof CloudBlob) {
                    CloudBlob blob = (CloudBlob) item;

                    String name = extractBlobItemName(blob.getName(), delimiter);
                    String path = Strings.nullToEmpty(blob.getName());
                    String type = "";
                    String cacheControlHeader = "";
                    String contentEncoding = "";
                    String contentLanguage = "";
                    String contentType = "";
                    String contentMD5Header = "";
                    String eTag = "";
                    Calendar lastModified = new GregorianCalendar();
                    long size = 0;

                    BlobProperties properties = blob.getProperties();

                    if (properties != null) {
                        if (properties.getBlobType() != null) {
                            type = properties.getBlobType().toString();
                        }

                        cacheControlHeader = Strings.nullToEmpty(properties.getCacheControl());
                        contentEncoding = Strings.nullToEmpty(properties.getContentEncoding());
                        contentLanguage = Strings.nullToEmpty(properties.getContentLanguage());
                        contentType = Strings.nullToEmpty(properties.getContentType());
                        contentMD5Header = Strings.nullToEmpty(properties.getContentMD5());
                        eTag = Strings.nullToEmpty(properties.getEtag());

                        if (properties.getLastModified() != null) {
                            lastModified.setTime(properties.getLastModified());
                        }

                        size = properties.getLength();
                    }

                    biList.add(new BlobFile(name, uri, containerName, path, type, cacheControlHeader, contentEncoding,
                            contentLanguage, contentType, contentMD5Header, eTag, lastModified, size, subscriptionId));
                }
            }

            return biList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Blob Item list", t);
        }
    }


    @Override
    public BlobDirectory createBlobDirectory(StorageAccount storageAccount,
                                             BlobDirectory parentBlobDirectory,
                                             BlobDirectory blobDirectory)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = parentBlobDirectory.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);
            CloudBlobDirectory parentDirectory = container.getDirectoryReference(parentBlobDirectory.getPath());
            CloudBlobDirectory directory = parentDirectory.getSubDirectoryReference(blobDirectory.getName());

            String uri = directory.getUri() != null ? directory.getUri().toString() : "";
            String path = Strings.nullToEmpty(directory.getPrefix());

            blobDirectory.setUri(uri);
            blobDirectory.setContainerName(containerName);
            blobDirectory.setPath(path);

            return blobDirectory;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Blob Directory", t);
        }
    }


    @Override
    public BlobFile createBlobFile(StorageAccount storageAccount,
                                   BlobDirectory parentBlobDirectory,
                                   BlobFile blobFile)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = parentBlobDirectory.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);
            CloudBlobDirectory parentDirectory = container.getDirectoryReference(parentBlobDirectory.getPath());

            CloudBlob blob = getCloudBlob(parentDirectory, blobFile);

            blob.upload(new ByteArrayInputStream(new byte[0]), 0);

            return reloadBlob(blob, containerName, blobFile);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Blob File", t);
        }
    }

    @Override
    public void deleteBlobFile(StorageAccount storageAccount, BlobFile blobFile)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = blobFile.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);

            CloudBlob blob = getCloudBlob(container, blobFile);

            blob.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Blob File", t);
        }
    }

    @Override
    public void uploadBlobFileContent(StorageAccount storageAccount,
                                      BlobContainer blobContainer,
                                      String filePath,
                                      InputStream content,
                                      CallableSingleArg<Void, Long> processBlock,
                                      long maxBlockSize,
                                      long length)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = blobContainer.getName();

            CloudBlobContainer container = client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(filePath);
            long uploadedBytes = 0;

            ArrayList<BlockEntry> blockEntries = new ArrayList<BlockEntry>();

            while (uploadedBytes < length) {
                String blockId = Base64.encode(UUID.randomUUID().toString().getBytes());
                BlockEntry entry = new BlockEntry(blockId, BlockSearchMode.UNCOMMITTED);

                long blockSize = maxBlockSize;
                if (length - uploadedBytes <= maxBlockSize) {
                    blockSize = length - uploadedBytes;
                }

                if (processBlock != null) {
                    processBlock.call(uploadedBytes);
                }

                entry.setSize(blockSize);

                blockEntries.add(entry);
                blob.uploadBlock(entry.getId(), content, blockSize);
                uploadedBytes += blockSize;
            }

            blob.commitBlockList(blockEntries);

        } catch (Throwable t) {
            throw new AzureCmdException("Error uploading the Blob File content", t);
        }
    }

    @Override
    public void downloadBlobFileContent(StorageAccount storageAccount,
                                        BlobFile blobFile,
                                        OutputStream content)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = blobFile.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);

            CloudBlob blob = getCloudBlob(container, blobFile);

            blob.download(content);
        } catch (Throwable t) {
            throw new AzureCmdException("Error downloading the Blob File content", t);
        }
    }

    @NotNull
    @Override
    public List<Queue> getQueues(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        List<Queue> qList = new ArrayList<Queue>();

        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            for (CloudQueue cloudQueue : client.listQueues(null, QueueListingDetails.ALL, null, null)) {
                String uri = cloudQueue.getUri() != null ? cloudQueue.getUri().toString() : "";

                qList.add(new Queue(Strings.nullToEmpty(cloudQueue.getName()),
                        uri,
                        cloudQueue.getApproximateMessageCount(),
                        storageAccount.getSubscriptionId()));
            }

            return qList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Queue list", t);
        }
    }

    @NotNull
    @Override
    public Queue createQueue(@NotNull StorageAccount storageAccount,
                             @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queue.getName());
            cloudQueue.createIfNotExists();
            cloudQueue.downloadAttributes();

            String uri = cloudQueue.getUri() != null ? cloudQueue.getUri().toString() : "";
            long approximateMessageCount = cloudQueue.getApproximateMessageCount();

            queue.setUri(uri);
            queue.setApproximateMessageCount(approximateMessageCount);

            return queue;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Queue", t);
        }
    }

    @Override
    public void deleteQueue(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queue.getName());
            cloudQueue.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Queue", t);
        }
    }

    @NotNull
    @Override
    public List<QueueMessage> getQueueMessages(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        List<QueueMessage> qmList = new ArrayList<QueueMessage>();

        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);
            String queueName = queue.getName();
            String subscriptionId = storageAccount.getSubscriptionId();

            CloudQueue cloudQueue = client.getQueueReference(queueName);

            for (CloudQueueMessage cqm : cloudQueue.peekMessages(32)) {
                String id = Strings.nullToEmpty(cqm.getId());
                String content = Strings.nullToEmpty(cqm.getMessageContentAsString());

                Calendar insertionTime = new GregorianCalendar();

                if (cqm.getInsertionTime() != null) {
                    insertionTime.setTime(cqm.getInsertionTime());
                }

                Calendar expirationTime = new GregorianCalendar();

                if (cqm.getExpirationTime() != null) {
                    expirationTime.setTime(cqm.getExpirationTime());
                }

                int dequeueCount = cqm.getDequeueCount();

                qmList.add(new QueueMessage(id, queueName, content, insertionTime, expirationTime, dequeueCount, subscriptionId));
            }

            return qmList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Queue Message list", t);
        }
    }

    @Override
    public void clearQueue(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queue.getName());
            cloudQueue.clear();
        } catch (Throwable t) {
            throw new AzureCmdException("Error clearing the Queue", t);
        }
    }

    @Override
    public void createQueueMessage(@NotNull StorageAccount storageAccount,
                                   @NotNull QueueMessage queueMessage,
                                   int timeToLiveInSeconds)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queueMessage.getQueueName());
            cloudQueue.addMessage(new CloudQueueMessage(queueMessage.getContent()), timeToLiveInSeconds, 0, null, null);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Queue Message", t);
        }
    }

    @NotNull
    @Override
    public QueueMessage dequeueFirstQueueMessage(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);
            String queueName = queue.getName();
            String subscriptionId = storageAccount.getSubscriptionId();

            CloudQueue cloudQueue = client.getQueueReference(queueName);
            CloudQueueMessage cqm = cloudQueue.retrieveMessage();

            String id = "";
            String content = "";
            Calendar insertionTime = new GregorianCalendar();
            Calendar expirationTime = new GregorianCalendar();
            int dequeueCount = 0;

            if (cqm != null) {
                id = Strings.nullToEmpty(cqm.getId());
                content = Strings.nullToEmpty(cqm.getMessageContentAsString());

                if (cqm.getInsertionTime() != null) {
                    insertionTime.setTime(cqm.getInsertionTime());
                }

                if (cqm.getExpirationTime() != null) {
                    expirationTime.setTime(cqm.getExpirationTime());
                }

                dequeueCount = cqm.getDequeueCount();
            }

            QueueMessage queueMessage = new QueueMessage(id, queueName, content, insertionTime, expirationTime, dequeueCount, subscriptionId);

            if (cqm != null) {
                cloudQueue.deleteMessage(cqm);
            }

            return queueMessage;
        } catch (Throwable t) {
            throw new AzureCmdException("Error dequeuing the first Queue Message", t);
        }
    }

    @NotNull
    @Override
    public List<Table> getTables(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        List<Table> tList = new ArrayList<Table>();

        try {
            CloudTableClient client = getCloudTableClient(storageAccount);

            for (String tableName : client.listTables()) {
                CloudTable cloudTable = client.getTableReference(tableName);

                String uri = cloudTable.getUri() != null ? cloudTable.getUri().toString() : "";

                tList.add(new Table(tableName,
                        uri,
                        storageAccount.getSubscriptionId()));
            }

            return tList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Table list", t);
        }
    }

    @NotNull
    @Override
    public Table createTable(@NotNull StorageAccount storageAccount,
                             @NotNull Table table)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);

            CloudTable cloudTable = client.getTableReference(table.getName());
            cloudTable.createIfNotExists();

            String uri = cloudTable.getUri() != null ? cloudTable.getUri().toString() : "";

            table.setUri(uri);

            return table;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Table", t);
        }
    }

    @Override
    public void deleteTable(@NotNull StorageAccount storageAccount, @NotNull Table table)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);

            CloudTable cloudTable = client.getTableReference(table.getName());
            cloudTable.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Table", t);
        }
    }

    @NotNull
    @Override
    public List<TableEntity> getTableEntities(@NotNull StorageAccount storageAccount, @NotNull Table table,
                                              @NotNull String filter)
            throws AzureCmdException {
        List<TableEntity> teList = new ArrayList<TableEntity>();

        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            String tableName = table.getName();
            String subscriptionId = storageAccount.getSubscriptionId();
            CloudTable cloudTable = client.getTableReference(tableName);

            TableQuery<DynamicTableEntity> tableQuery = TableQuery.from(DynamicTableEntity.class);

            if (!filter.isEmpty()) {
                tableQuery.where(filter);
            }

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            for (DynamicTableEntity dte : cloudTable.execute(tableQuery, tro, null)) {
                teList.add(getTableEntity(tableName, dte, subscriptionId));
            }

            return teList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Table Entity list", t);
        }
    }

    @NotNull
    @Override
    public TableEntity createTableEntity(@NotNull StorageAccount storageAccount, @NotNull String tableName,
                                         @NotNull String partitionKey, @NotNull String rowKey,
                                         @NotNull Map<String, Property> properties)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            String subscriptionId = storageAccount.getSubscriptionId();
            CloudTable cloudTable = client.getTableReference(tableName);

            DynamicTableEntity entity = getDynamicTableEntity(partitionKey, rowKey, properties);

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            TableResult result = cloudTable.execute(TableOperation.insert(entity, true), tro, null);
            DynamicTableEntity resultEntity = result.getResultAsType();

            return getTableEntity(tableName, resultEntity, subscriptionId);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Table Entity", t);
        }
    }

    @NotNull
    @Override
    public TableEntity updateTableEntity(@NotNull StorageAccount storageAccount, @NotNull TableEntity tableEntity)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            CloudTable cloudTable = client.getTableReference(tableEntity.getTableName());

            DynamicTableEntity entity = getDynamicTableEntity(tableEntity);

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            TableResult result = cloudTable.execute(TableOperation.replace(entity), tro, null);
            DynamicTableEntity resultEntity = result.getResultAsType();

            return getTableEntity(tableEntity.getTableName(), resultEntity, tableEntity.getSubscriptionId());
        } catch (Throwable t) {
            throw new AzureCmdException("Error updating the Table Entity", t);
        }
    }

    @Override
    public void deleteTableEntity(@NotNull StorageAccount storageAccount, @NotNull TableEntity tableEntity)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            CloudTable cloudTable = client.getTableReference(tableEntity.getTableName());

            DynamicTableEntity entity = getDynamicTableEntity(tableEntity);

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            cloudTable.execute(TableOperation.delete(entity), tro, null);
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Table Entity", t);
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


    private static StorageManagementClient getStorageManagementClient(String subscriptionId) throws Exception {
        StorageManagementClient client = AzureSDKHelper.getStorageManagementClient(subscriptionId);

        if (client == null) {
            throw new Exception("Unable to instantiate Storage Management client");
        }

        return client;
    }


    private static NetworkManagementClient getNetworkManagementClient(String subscriptionId) throws Exception {
        NetworkManagementClient client = AzureSDKHelper.getNetworkManagementClient(subscriptionId);

        if (client == null) {
            throw new Exception("Unable to instantiate Network Management client");
        }

        return client;
    }


    private static ManagementClient getManagementClient(String subscriptionId) throws Exception {
        ManagementClient client = AzureSDKHelper.getManagementClient(subscriptionId);

        if (client == null) {
            throw new Exception("Unable to instantiate Management client");
        }

        return client;
    }


    private static CloudBlobClient getCloudBlobClient(StorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = AzureSDKHelper.getCloudStorageAccount(storageAccount);

        return csa.createCloudBlobClient();
    }

    @NotNull
    private static CloudQueueClient getCloudQueueClient(StorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = AzureSDKHelper.getCloudStorageAccount(storageAccount);

        return csa.createCloudQueueClient();
    }

    @NotNull
    private static CloudTableClient getCloudTableClient(StorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = AzureSDKHelper.getCloudStorageAccount(storageAccount);

        return csa.createCloudTableClient();
    }

    @NotNull
    private static HostedServiceOperations getHostedServiceOperations(ComputeManagementClient client)
            throws Exception {
        HostedServiceOperations hso = client.getHostedServicesOperations();

        if (hso == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hso;
    }


    private static DeploymentOperations getDeploymentOperations(ComputeManagementClient client)
            throws Exception {
        DeploymentOperations dop = client.getDeploymentsOperations();

        if (dop == null) {
            throw new Exception("Unable to retrieve Deployment information");
        }

        return dop;
    }


    private static VirtualMachineOperations getVirtualMachineOperations(ComputeManagementClient client)
            throws Exception {
        VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

        if (vmo == null) {
            throw new Exception("Unable to retrieve Virtual Machines Information");
        }

        return vmo;
    }


    private static VirtualMachineOSImageOperations getVirtualMachineOSImageOperations(ComputeManagementClient client)
            throws Exception {
        VirtualMachineOSImageOperations vmosio = client.getVirtualMachineOSImagesOperations();

        if (vmosio == null) {
            throw new Exception("Unable to retrieve OS Images information");
        }

        return vmosio;
    }


    private static VirtualMachineVMImageOperations getVirtualMachineVMImageOperations(ComputeManagementClient client)
            throws Exception {
        VirtualMachineVMImageOperations vmvmio = client.getVirtualMachineVMImagesOperations();

        if (vmvmio == null) {
            throw new Exception("Unable to retrieve VM Images information");
        }

        return vmvmio;
    }


    private static RoleSizeOperations getRoleSizeOperations(ManagementClient client)
            throws Exception {
        RoleSizeOperations rso = client.getRoleSizesOperations();

        if (rso == null) {
            throw new Exception("Unable to retrieve Role Sizes information");
        }

        return rso;
    }


    private static LocationOperations getLocationsOperations(ManagementClient client)
            throws Exception {
        LocationOperations lo = client.getLocationsOperations();

        if (lo == null) {
            throw new Exception("Unable to retrieve Locations information");
        }

        return lo;
    }


    private static AffinityGroupOperations getAffinityGroupOperations(ManagementClient client)
            throws Exception {
        AffinityGroupOperations ago = client.getAffinityGroupsOperations();

        if (ago == null) {
            throw new Exception("Unable to retrieve Affinity Groups information");
        }

        return ago;
    }


    private static StorageAccountOperations getStorageAccountOperations(StorageManagementClient client)
            throws Exception {
        StorageAccountOperations sao = client.getStorageAccountsOperations();

        if (sao == null) {
            throw new Exception("Unable to retrieve Storage Accounts information");
        }

        return sao;
    }


    private static NetworkOperations getNetworkOperations(NetworkManagementClient client)
            throws Exception {
        NetworkOperations no = client.getNetworksOperations();

        if (no == null) {
            throw new Exception("Unable to retrieve Network information");
        }

        return no;
    }


    private static ServiceCertificateOperations getServiceCertificateOperations(ComputeManagementClient client)
            throws Exception {
        ServiceCertificateOperations sco = client.getServiceCertificatesOperations();

        if (sco == null) {
            throw new Exception("Unable to retrieve Service Certificate information");
        }

        return sco;
    }


    private HostedServiceListResponse getHostedServices(ComputeManagementClient client) throws Exception {
        HostedServiceOperations hso = getHostedServiceOperations(client);

        HostedServiceListResponse hslr = hso.list();

        if (hslr == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hslr;
    }


    private static ListenableFuture<DeploymentGetResponse> getDeploymentAsync(final ComputeManagementClient client,
                                                                              final String serviceName,
                                                                              final DeploymentSlot slot) {
        final SettableFuture<DeploymentGetResponse> future = SettableFuture.create();

        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
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


    private static DeploymentGetResponse getDeployment(ComputeManagementClient client,
                                                       String serviceName,
                                                       DeploymentSlot slot)
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


    private static DeploymentGetResponse getDeployment(ComputeManagementClient client,
                                                       VirtualMachine vm)
            throws Exception {
        return getDeployment(client, vm.getServiceName(), DeploymentSlot.Production);
    }


    private StorageAccountListResponse getStorageAccounts(StorageManagementClient client) throws Exception {
        StorageAccountListResponse salr = getStorageAccountOperations(client).list();

        if (salr == null) {
            throw new Exception("Unable to retrieve Storage Accounts information");
        }

        return salr;
    }


    private static ListenableFuture<StorageAccount> getStorageAccountAsync(final String subscriptionId,
                                                                           final StorageManagementClient client,
                                                                           final com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount)
            throws Exception {
        final SettableFuture<StorageAccount> future = SettableFuture.create();
        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(getStorageAccount(subscriptionId, client, storageAccount));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });
        return future;
    }


    private static StorageAccount getStorageAccount(String subscriptionId,
                                                    StorageManagementClient client,
                                                    com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount) throws Exception {
        String primaryKey = "";
        String secondaryKey = "";

        if (storageAccount.getName() != null) {
            StorageAccountGetKeysResponse sak = getStorageAccountKeys(client, storageAccount.getName());

            primaryKey = sak.getPrimaryKey();
            secondaryKey = sak.getSecondaryKey();
        }

        StorageAccountProperties sap = storageAccount.getProperties() != null ?
                storageAccount.getProperties() :
                new StorageAccountProperties();
        String blobsUri = "";
        String queuesUri = "";
        String tablesUri = "";

        ArrayList<URI> endpoints = sap.getEndpoints();

        if (endpoints != null && endpoints.size() > 0) {
            blobsUri = endpoints.get(0).toString();

            if (endpoints.size() > 1) {
                queuesUri = endpoints.get(1).toString();

                if (endpoints.size() > 2) {
                    tablesUri = endpoints.get(2).toString();
                }
            }
        }

        return new StorageAccount(
                Strings.nullToEmpty(storageAccount.getName()),
                Strings.nullToEmpty(sap.getAccountType()),
                Strings.nullToEmpty(sap.getDescription()),
                Strings.nullToEmpty(sap.getLabel()),
                sap.getStatus() != null ? sap.getStatus().toString() : "",
                Strings.nullToEmpty(sap.getLocation()),
                Strings.nullToEmpty(sap.getAffinityGroup()),
                Strings.nullToEmpty(primaryKey),
                Strings.nullToEmpty(secondaryKey),
                storageAccount.getUri() != null ? storageAccount.getUri().toString() : "",
                blobsUri,
                queuesUri,
                tablesUri,
                Strings.nullToEmpty(sap.getGeoPrimaryRegion()),
                sap.getStatusOfGeoPrimaryRegion() != null ? sap.getStatusOfGeoPrimaryRegion().toString() : "",
                Strings.nullToEmpty(sap.getGeoSecondaryRegion()),
                sap.getStatusOfGeoSecondaryRegion() != null ? sap.getStatusOfGeoSecondaryRegion().toString() : "",
                sap.getLastGeoFailoverTime() != null ? sap.getLastGeoFailoverTime() : new GregorianCalendar(),
                subscriptionId);
    }


    private static StorageAccountGetKeysResponse getStorageAccountKeys(StorageManagementClient client,
                                                                       String storageName)
            throws Exception {
        StorageAccountGetKeysResponse sagkr = getStorageAccountOperations(client).getKeys(storageName);

        if (sagkr == null) {
            throw new Exception("Unable to retrieve Storage Account Keys information");
        }

        return sagkr;
    }


    private static List<Role> getVMDeploymentRoles(DeploymentGetResponse deployment) throws Exception {
        ArrayList<Role> roles = deployment.getRoles();

        if (roles == null) {
            throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
        }

        return roles;
    }


    private NetworkListResponse getNetworks(NetworkManagementClient client) throws Exception {
        NetworkListResponse nlr = getNetworkOperations(client).list();

        if (nlr == null) {
            throw new Exception("Unable to retrieve Networks information");
        }

        return nlr;
    }

    private static void validateOperationStatus(OperationStatusResponse osr) throws Exception {
        if (osr == null) {
            throw new Exception("Unable to retrieve Operation Status");
        }

        if (osr.getError() != null) {
            throw new Exception(osr.getError().getMessage());
        }
    }

    private static OperationStatusResponse getOperationStatusResponse(ComputeManagementClient client,
                                                                      OperationResponse or)
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

    private static OperationStatusResponse getOperationStatusResponse(StorageManagementClient client,
                                                                      OperationResponse or)
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


    private static CloudService loadDeployment(DeploymentGetResponse deployment,
                                               CloudService cloudService)
            throws Exception {
        if (deployment.getDeploymentSlot() != null) {
            Deployment dep;

            switch (deployment.getDeploymentSlot()) {
                case Production:
                    dep = cloudService.getProductionDeployment();
                    break;
                case Staging:
                    dep = cloudService.getStagingDeployment();
                    break;
                default:
                    return cloudService;
            }

            dep.setName(deployment.getName() != null ? deployment.getName() : "");
            dep.setVirtualNetwork(deployment.getVirtualNetworkName() != null ? deployment.getVirtualNetworkName() : "");

            if (deployment.getRoles() != null) {
                Set<String> virtualMachines = dep.getVirtualMachines();
                Set<String> computeRoles = dep.getComputeRoles();
                Set<String> availabilitySets = dep.getAvailabilitySets();

                for (Role role : deployment.getRoles()) {
                    if (role.getRoleType() != null && role.getRoleType().equals(PERSISTENT_VM_ROLE)) {
                        if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
                            virtualMachines.add(role.getRoleName());
                        }

                        if (role.getAvailabilitySetName() != null && !role.getAvailabilitySetName().isEmpty()) {
                            availabilitySets.add(role.getAvailabilitySetName());
                        }
                    } else {
                        if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
                            computeRoles.add(role.getRoleName());
                        }
                    }
                }
            }
        }

        return cloudService;
    }



    private static List<VirtualMachine> loadVirtualMachines(ComputeManagementClient client,
                                                            String subscriptionId,
                                                            String serviceName,
                                                            List<VirtualMachine> vmList)
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
                        "",
                        role.getRoleSize() != null ? role.getRoleSize() : "",
                        getVMStatus(deployment, role),
                        subscriptionId);

                vm = loadNetworkConfiguration(role, vm);

                vmList.add(vm);
            }
        }

        return vmList;
    }


    private static VirtualMachine loadNetworkConfiguration(Role role, VirtualMachine vm) {
        if (role.getConfigurationSets() == null) {
            return vm;
        }

        List<Endpoint> endpoints = vm.getEndpoints();

        for (ConfigurationSet configurationSet : role.getConfigurationSets()) {
            if (configurationSet.getConfigurationSetType() != null
                    && configurationSet.getConfigurationSetType().equals(NETWORK_CONFIGURATION)) {
                if (configurationSet.getInputEndpoints() != null) {
                    for (InputEndpoint inputEndpoint : configurationSet.getInputEndpoints()) {
                        endpoints.add(new Endpoint(
                                inputEndpoint.getName() != null ? inputEndpoint.getName() : "",
                                inputEndpoint.getProtocol() != null ? inputEndpoint.getProtocol() : "",
                                inputEndpoint.getLocalPort(),
                                inputEndpoint.getPort()));
                    }
                }

                if (configurationSet.getSubnetNames() != null && configurationSet.getSubnetNames().size() == 1) {
                    vm.setSubnet(configurationSet.getSubnetNames().get(0));
                }

                break;
            }
        }

        return vm;
    }

    private static void deleteVMRole(ComputeManagementClient client, String serviceName,
                                     String deploymentName, String virtualMachineName,
                                     boolean deleteFromStorage)
            throws Exception {
        VirtualMachineOperations vmo = getVirtualMachineOperations(client);

        OperationStatusResponse osr = vmo.delete(serviceName, deploymentName, virtualMachineName, deleteFromStorage);

        validateOperationStatus(osr);
    }

    private static void deleteDeployment(ComputeManagementClient client, String serviceName,
                                         String deploymentName, boolean deleteFromStorage)
            throws Exception {
        DeploymentOperations dop = getDeploymentOperations(client);

        OperationStatusResponse osr = dop.deleteByName(serviceName, deploymentName, deleteFromStorage);

        validateOperationStatus(osr);
    }



    private static String getMediaLocation(VirtualMachine virtualMachine,
                                           StorageAccount storageAccount)
            throws Exception {
        Calendar calendar = GregorianCalendar.getInstance();
        String blobName = String.format("%s-%s-0-%04d%02d%02d%02d%02d%02d%04d.vhd",
                virtualMachine.getServiceName(),
                virtualMachine.getName(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND));

        CloudBlobClient cloudBlobClient = getCloudBlobClient(storageAccount);

        CloudBlobContainer container = cloudBlobClient.getContainerReference("vhds");
        container.createIfNotExists();

        return container.getUri().toString() + "/" + blobName;
    }


    private static ListenableFuture<List<VirtualMachineImage>> getOSImagesAsync(
            final ComputeManagementClient client) {
        final SettableFuture<List<VirtualMachineImage>> future = SettableFuture.create();
        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(getOSImages(client));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });
        return future;
    }


    private static List<VirtualMachineImage> getOSImages(ComputeManagementClient client)
            throws Exception {
        List<VirtualMachineImage> vmImageList = new ArrayList<VirtualMachineImage>();

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


    private static ListenableFuture<List<VirtualMachineImage>> getVMImagesAsync(
            final ComputeManagementClient client) {
        final SettableFuture<List<VirtualMachineImage>> future = SettableFuture.create();

        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(getVMImages(client));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });
        return future;
    }


    private static List<VirtualMachineImage> getVMImages(ComputeManagementClient client)
            throws Exception {
        List<VirtualMachineImage> vmImageList = new ArrayList<VirtualMachineImage>();

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


    private static List<VirtualMachineSize> loadVMSizes(ManagementClient client,
                                                        List<VirtualMachineSize> vmSizeList)
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


    private static List<Location> loadLocations(ManagementClient client,
                                                List<Location> locationList)
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


    private static List<AffinityGroup> loadAffinityGroups(ManagementClient client,
                                                          List<AffinityGroup> affinityGroupList)
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

    private static void createVM(VirtualMachineOperations vmo,
                                 VirtualMachine virtualMachine,
                                 VirtualMachineImage vmImage,
                                 String mediaLocation,
                                 String username,
                                 String password,
                                 byte[] certificate)
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

        vmcp.getConfigurationSets().add(getProvisioningConfigurationSet(virtualMachine, vmImage,
                username, password, certificate));

        if (virtualMachine.getEndpoints().size() > 0 || !virtualMachine.getSubnet().isEmpty()) {
            vmcp.getConfigurationSets().add(getNetworkConfigurationSet(virtualMachine));
        }

        OperationStatusResponse osr = vmo.create(virtualMachine.getServiceName(), virtualMachine.getDeploymentName(), vmcp);

        validateOperationStatus(osr);
    }

    private static void createVMDeployment(VirtualMachineOperations vmo,
                                           VirtualMachine virtualMachine,
                                           VirtualMachineImage vmImage,
                                           String mediaLocation,
                                           String virtualNetwork,
                                           String username,
                                           String password,
                                           byte[] certificate)
            throws Exception {
        VirtualMachineCreateDeploymentParameters vmcdp = new VirtualMachineCreateDeploymentParameters();
        vmcdp.setName(virtualMachine.getName());
        vmcdp.setLabel(virtualMachine.getName());
        vmcdp.setDeploymentSlot(DeploymentSlot.Production);

        if (!virtualNetwork.isEmpty()) {
            vmcdp.setVirtualNetworkName(virtualNetwork);
        }

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

        role.getConfigurationSets().add(getProvisioningConfigurationSet(virtualMachine, vmImage,
                username, password, certificate));

        if (virtualMachine.getEndpoints().size() > 0 || !virtualMachine.getSubnet().isEmpty()) {
            role.getConfigurationSets().add(getNetworkConfigurationSet(virtualMachine));
        }

        vmcdp.getRoles().add(role);

        OperationStatusResponse osr = vmo.createDeployment(virtualMachine.getServiceName(), vmcdp);

        validateOperationStatus(osr);
    }


    private static ConfigurationSet getProvisioningConfigurationSet(VirtualMachine virtualMachine,
                                                                    VirtualMachineImage vmImage,
                                                                    String username,
                                                                    String password,
                                                                    byte[] certificate) throws AzureCmdException {
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

            if (!password.isEmpty()) {
                provConfSet.setUserPassword(password);
                provConfSet.setDisableSshPasswordAuthentication(false);
            }

            if (certificate.length > 0) {
                String fingerprint = getManager().createServiceCertificate(virtualMachine.getSubscriptionId(),
                        virtualMachine.getServiceName(),
                        certificate,
                        "");

                SshSettings sshSettings = new SshSettings();
                String keyLocation = String.format("/home/%s/.ssh/authorized_keys", username);
                sshSettings.getPublicKeys().add(new SshSettingPublicKey(fingerprint, keyLocation));
                provConfSet.setSshSettings(sshSettings);
            }

            provConfSet.setHostName(String.format("%s-%s-%02d",
                    virtualMachine.getServiceName().substring(0, 5),
                    virtualMachine.getName().substring(0, 5),
                    1));
        }

        return provConfSet;
    }


    private static ConfigurationSet getNetworkConfigurationSet(VirtualMachine virtualMachine) {
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

        if (!virtualMachine.getSubnet().isEmpty()) {
            netConfSet.getSubnetNames().add(virtualMachine.getSubnet());
        }

        return netConfSet;
    }


    private static Status getVMStatus(DeploymentGetResponse deployment, Role role) {
        Status result = Status.Unknown;

        if (deployment.getRoleInstances() != null) {
            RoleInstance vmRoleInstance = null;

            for (RoleInstance roleInstance : deployment.getRoleInstances()) {
                if (roleInstance.getRoleName() != null && roleInstance.getRoleName().equals(role.getRoleName())) {
                    vmRoleInstance = roleInstance;
                    break;
                }
            }

            if (vmRoleInstance != null && vmRoleInstance.getInstanceStatus() != null) {
                result = getRoleStatus(vmRoleInstance.getInstanceStatus());
            }
        }

        return result;
    }


    private static Status getRoleStatus(String instanceStatus) {
        Status result = Status.Unknown;

        if (instanceStatus.equals(StatusLiterals.UNKNOWN)) {
            result = Status.Unknown;
        } else if (instanceStatus.equals(StatusLiterals.READY_ROLE)) {
            result = Status.Ready;
        } else if (instanceStatus.equals(StatusLiterals.STOPPED_VM)) {
            result = Status.Stopped;
        } else if (instanceStatus.equals(StatusLiterals.STOPPED_DEALLOCATED)) {
            result = Status.StoppedDeallocated;
        } else if (instanceStatus.equals(StatusLiterals.BUSY_ROLE)) {
            result = Status.Busy;
        } else if (instanceStatus.equals(StatusLiterals.CREATING_VM) ||
                instanceStatus.equals(StatusLiterals.CREATING_ROLE)) {
            result = Status.Creating;
        } else if (instanceStatus.equals(StatusLiterals.STARTING_VM) ||
                instanceStatus.equals(StatusLiterals.STARTING_ROLE)) {
            result = Status.Starting;
        } else if (instanceStatus.equals(StatusLiterals.STOPPING_VM) ||
                instanceStatus.equals(StatusLiterals.STOPPING_ROLE)) {
            result = Status.Stopping;
        } else if (instanceStatus.equals(StatusLiterals.DELETING_VM)) {
            result = Status.Deleting;
        } else if (instanceStatus.equals(StatusLiterals.RESTARTING_ROLE)) {
            result = Status.Restarting;
        } else if (instanceStatus.equals(StatusLiterals.CYCLING_ROLE)) {
            result = Status.Cycling;
        } else if (instanceStatus.equals(StatusLiterals.FAILED_STARTING_VM) ||
                instanceStatus.equals(StatusLiterals.FAILED_STARTING_ROLE)) {
            result = Status.FailedStarting;
        } else if (instanceStatus.equals(StatusLiterals.UNRESPONSIVE_ROLE)) {
            result = Status.Unresponsive;
        } else if (instanceStatus.equals(StatusLiterals.PREPARING)) {
            result = Status.Preparing;
        }

        return result;
    }


    private static CloudBlob getCloudBlob(CloudBlobContainer container,
                                          BlobFile blobFile)
            throws URISyntaxException, StorageException {
        CloudBlob blob;

        if (blobFile.getType().equals(BlobType.BLOCK_BLOB.toString())) {
            blob = container.getBlockBlobReference(blobFile.getPath());
        } else {
            blob = container.getPageBlobReference(blobFile.getPath());
        }

        return blob;
    }


    private static CloudBlob getCloudBlob(CloudBlobDirectory parentDirectory,
                                          BlobFile blobFile)
            throws URISyntaxException, StorageException {
        CloudBlob blob;

        if (blobFile.getType().equals(BlobType.BLOCK_BLOB.toString())) {
            blob = parentDirectory.getBlockBlobReference(blobFile.getName());
        } else {
            blob = parentDirectory.getPageBlobReference(blobFile.getName());
        }
        return blob;
    }


    private static BlobFile reloadBlob(CloudBlob blob, String containerName, BlobFile blobFile)
            throws StorageException, URISyntaxException {
        blob.downloadAttributes();

        String uri = blob.getUri() != null ? blob.getUri().toString() : "";
        String path = Strings.nullToEmpty(blob.getName());
        String type = "";
        String cacheControlHeader = "";
        String contentEncoding = "";
        String contentLanguage = "";
        String contentType = "";
        String contentMD5Header = "";
        String eTag = "";
        Calendar lastModified = new GregorianCalendar();
        long size = 0;

        BlobProperties properties = blob.getProperties();

        if (properties != null) {
            if (properties.getBlobType() != null) {
                type = properties.getBlobType().toString();
            }

            cacheControlHeader = Strings.nullToEmpty(properties.getCacheControl());
            contentEncoding = Strings.nullToEmpty(properties.getContentEncoding());
            contentLanguage = Strings.nullToEmpty(properties.getContentLanguage());
            contentType = Strings.nullToEmpty(properties.getContentType());
            contentMD5Header = Strings.nullToEmpty(properties.getContentMD5());
            eTag = Strings.nullToEmpty(properties.getEtag());

            if (properties.getLastModified() != null) {
                lastModified.setTime(properties.getLastModified());
            }

            size = properties.getLength();
        }

        blobFile.setUri(uri);
        blobFile.setPath(path);
        blobFile.setContainerName(containerName);
        blobFile.setType(type);
        blobFile.setCacheControlHeader(cacheControlHeader);
        blobFile.setContentEncoding(contentEncoding);
        blobFile.setContentLanguage(contentLanguage);
        blobFile.setContentType(contentType);
        blobFile.setContentMD5Header(contentMD5Header);
        blobFile.setETag(eTag);
        blobFile.setLastModified(lastModified);
        blobFile.setSize(size);

        return blobFile;
    }


    private static String extractBlobItemName(String path, String delimiter) {
        if (path == null) {
            return "";
        } else if (delimiter == null || delimiter.isEmpty()) {
            return path;
        } else {
            String[] parts = path.split(delimiter);

            if (parts.length == 0) {
                return "";
            } else {
                return parts[parts.length - 1];
            }
        }
    }

    @NotNull
    private static TableEntity getTableEntity(@NotNull String tableName,
                                              @NotNull DynamicTableEntity dte,
                                              @NotNull String subscriptionId) {
        String partitionKey = Strings.nullToEmpty(dte.getPartitionKey());
        String rowKey = Strings.nullToEmpty(dte.getRowKey());
        String eTag = Strings.nullToEmpty(dte.getEtag());

        Calendar timestamp = new GregorianCalendar();

        if (dte.getTimestamp() != null) {
            timestamp.setTime(dte.getTimestamp());
        }

        Map<String, Property> properties = new HashMap<String, Property>();

        if (dte.getProperties() != null) {
            for (Entry<String, EntityProperty> entry : dte.getProperties().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String key = entry.getKey();
                    Property property;

                    switch (entry.getValue().getEdmType()) {
                        case BOOLEAN:
                            property = new Property(entry.getValue().getValueAsBooleanObject());
                            break;
                        case DATE_TIME:
                            Calendar value = new GregorianCalendar();
                            value.setTime(entry.getValue().getValueAsDate());
                            property = new Property(value);
                            break;
                        case DOUBLE:
                            property = new Property(entry.getValue().getValueAsDoubleObject());
                            break;
                        case GUID:
                            property = new Property(entry.getValue().getValueAsUUID());
                            break;
                        case INT32:
                            property = new Property(entry.getValue().getValueAsIntegerObject());
                            break;
                        case INT64:
                            property = new Property(entry.getValue().getValueAsLongObject());
                            break;
                        case STRING:
                            property = new Property(entry.getValue().getValueAsString());
                            break;
                        default:
                            property = new Property(entry.getValue().getValueAsString());
                            break;
                    }

                    properties.put(key, property);
                }
            }
        }

        return new TableEntity(partitionKey, rowKey, tableName, eTag, timestamp, properties, subscriptionId);
    }

    @NotNull
    private static DynamicTableEntity getDynamicTableEntity(@NotNull TableEntity tableEntity)
            throws AzureCmdException {
        return getDynamicTableEntity(tableEntity.getPartitionKey(), tableEntity.getRowKey(),
                tableEntity.getTimestamp(), tableEntity.getETag(), tableEntity.getProperties());

    }

    @NotNull
    private static DynamicTableEntity getDynamicTableEntity(@NotNull String partitionKey,
                                                            @NotNull String rowKey,
                                                            @NotNull Map<String, Property> properties)
            throws AzureCmdException {
        return getDynamicTableEntity(partitionKey, rowKey, null, null, properties);
    }

    @NotNull
    private static DynamicTableEntity getDynamicTableEntity(@NotNull String partitionKey,
                                                            @NotNull String rowKey,
                                                            @Nullable Calendar timestamp,
                                                            @Nullable String eTag,
                                                            @NotNull Map<String, Property> properties)
            throws AzureCmdException {
        Date ts = null;

        if (timestamp != null) {
            ts = timestamp.getTime();
        }

        HashMap<String, EntityProperty> entityProperties = getEntityProperties(properties);

        return new DynamicTableEntity(partitionKey, rowKey, ts, eTag, entityProperties);
    }

    @NotNull
    private static HashMap<String, EntityProperty> getEntityProperties(@NotNull Map<String, Property> properties) throws AzureCmdException {
        HashMap<String, EntityProperty> entityProperties = new HashMap<String, EntityProperty>();

        for (Entry<String, Property> entry : properties.entrySet()) {
            String key = entry.getKey();
            Property property = entry.getValue();

            EntityProperty entityProperty;

            switch (property.getType()) {
                case Boolean:
                    entityProperty = new EntityProperty(property.getValueAsBoolean());
                    break;
                case DateTime:
                    entityProperty = new EntityProperty(property.getValueAsCalendar().getTime());
                    break;
                case Double:
                    entityProperty = new EntityProperty(property.getValueAsDouble());
                    break;
                case Uuid:
                    entityProperty = new EntityProperty(property.getValueAsUuid());
                    break;
                case Integer:
                    entityProperty = new EntityProperty(property.getValueAsInteger());
                    break;
                case Long:
                    entityProperty = new EntityProperty(property.getValueAsLong());
                    break;
                case String:
                    entityProperty = new EntityProperty(property.getValueAsString());
                    break;
                default:
                    entityProperty = new EntityProperty(property.getValueAsString());
                    break;
            }

            entityProperties.put(key, entityProperty);
        }
        return entityProperties;
    }


    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }
}