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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.Nullable;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.UserInfo;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.ms.*;
import com.microsoftopentechnologies.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.vm.*;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;

import java.util.List;

public interface AzureManager {
    void authenticate() throws AzureCmdException;

    boolean authenticated();

    boolean authenticated(@NotNull String subscriptionId);

    @Nullable
    UserInfo getUserInfo();

    void clearAuthentication();

    void importPublishSettingsFile(@NotNull String publishSettingsFilePath)
            throws AzureCmdException;

    boolean usingCertificate();

    boolean usingCertificate(@NotNull String subscriptionId);

    void clearImportedPublishSettingsFiles();

    @NotNull
    List<Subscription> getFullSubscriptionList()
            throws AzureCmdException;

    @NotNull
    List<Subscription> getSubscriptionList()
            throws AzureCmdException;

    void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws AzureCmdException;

    @NotNull
    EventWaitHandle registerSubscriptionsChanged()
            throws AzureCmdException;

    void unregisterSubscriptionsChanged(@NotNull EventWaitHandle handle)
            throws AzureCmdException;

    @NotNull
    List<SqlDb> getSqlDb(@NotNull String subscriptionId, @NotNull SqlServer server)
            throws AzureCmdException;

    @NotNull
    List<SqlServer> getSqlServers(@NotNull String subscriptionId)
            throws AzureCmdException;

    @NotNull
    List<MobileService> getMobileServiceList(@NotNull String subscriptionId)
            throws AzureCmdException;

    void createMobileService(@NotNull String subscriptionId,
                             @NotNull String region,
                             @NotNull String username,
                             @NotNull String password,
                             @NotNull String mobileServiceName,
                             @Nullable String server,
                             @Nullable String database)
            throws AzureCmdException;

    void deleteMobileService(@NotNull String subscriptionId, @NotNull String mobileServiceName);

    @NotNull
    List<Table> getTableList(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException;

    void createTable(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                     @NotNull String tableName, @NotNull TablePermissions permissions)
            throws AzureCmdException;

    void updateTable(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                     @NotNull String tableName, @NotNull TablePermissions permissions)
            throws AzureCmdException;

    @NotNull
    Table showTableDetails(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                           @NotNull String tableName)
            throws AzureCmdException;

    void downloadTableScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                             @NotNull String scriptName, @NotNull String downloadPath)
            throws AzureCmdException;

    void uploadTableScript(@NotNull String subscriptionId, @NotNull String mobileServiceName,
                           @NotNull String scriptName, @NotNull String filePath)
            throws AzureCmdException;

    @NotNull
    List<CustomAPI> getAPIList(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException;

    void downloadAPIScript(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String scriptName,
                           @NotNull String downloadPath)
            throws AzureCmdException;

    void uploadAPIScript(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String scriptName,
                         @NotNull String filePath)
            throws AzureCmdException;

    void createCustomAPI(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName,
                         @NotNull CustomAPIPermissions permissions)
            throws AzureCmdException;

    void updateCustomAPI(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String tableName,
                         @NotNull CustomAPIPermissions permissions)
            throws AzureCmdException;

    @NotNull
    List<Job> listJobs(@NotNull String subscriptionId, @NotNull String mobileServiceName)
            throws AzureCmdException;

    void createJob(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String jobName,
                   int interval, @NotNull String intervalUnit, @NotNull String startDate)
            throws AzureCmdException;

    void updateJob(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String jobName,
                   int interval, @NotNull String intervalUnit, @NotNull String startDate, boolean enabled)
            throws AzureCmdException;

    void downloadJobScript(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String scriptName,
                           @NotNull String downloadPath)
            throws AzureCmdException;

    void uploadJobScript(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String scriptName,
                         @NotNull String filePath)
            throws AzureCmdException;

    @NotNull
    List<LogEntry> listLog(@NotNull String subscriptionId, @NotNull String mobileServiceName, @NotNull String runtime)
            throws AzureCmdException;

    @NotNull
    List<CloudService> getCloudServices(@NotNull String subscriptionId)
            throws AzureCmdException;

    @NotNull
    List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
            throws AzureCmdException;

    @NotNull
    VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm)
            throws AzureCmdException;

    void startVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException;

    void shutdownVirtualMachine(@NotNull VirtualMachine vm, boolean deallocate) throws AzureCmdException;

    void restartVirtualMachine(@NotNull VirtualMachine vm) throws AzureCmdException;

    void deleteVirtualMachine(@NotNull VirtualMachine vm, boolean deleteFromStorage) throws AzureCmdException;

    @NotNull
    byte[] downloadRDP(@NotNull VirtualMachine vm) throws AzureCmdException;

    @NotNull
    List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<Location> getLocations(@NotNull String subscriptionId)
            throws AzureCmdException;

    @NotNull
    List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId) throws AzureCmdException;

    void createStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException;

    void createCloudService(@NotNull CloudService cloudService)
            throws AzureCmdException;

    void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                              @NotNull StorageAccount storageAccount, @NotNull String virtualNetwork,
                              @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException;

    void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                              @NotNull String mediaLocation, @NotNull String virtualNetwork,
                              @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException;

    @NotNull
    StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount)
            throws AzureCmdException;

    String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
                                    @NotNull byte[] data, @NotNull String password)
            throws AzureCmdException;

    void deleteStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException;
}