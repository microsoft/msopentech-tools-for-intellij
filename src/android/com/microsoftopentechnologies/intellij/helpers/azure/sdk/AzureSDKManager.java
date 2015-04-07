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

import com.microsoftopentechnologies.intellij.helpers.CallableSingleArg;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.model.storage.*;
import com.microsoftopentechnologies.intellij.model.vm.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface AzureSDKManager {
    @NotNull
    List<CloudService> getCloudServices(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm) throws AzureCmdException;

    void startVirtualMachine(@NotNull VirtualMachine vm) throws AzureCmdException;

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
    List<Location> getLocations(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId) throws AzureCmdException;

    @NotNull
    List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId) throws AzureCmdException;

    void createStorageAccount(@NotNull StorageAccount storageAccount) throws AzureCmdException;

    void createCloudService(@NotNull CloudService cloudService) throws AzureCmdException;

    void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                              @NotNull StorageAccount storageAccount, @NotNull String virtualNetwork,
                              @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException;

    void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                              @NotNull String mediaLocation, @NotNull String virtualNetwork,
                              @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException;

    @NotNull
    StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount) throws AzureCmdException;

    String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
                                    @NotNull byte[] data, @NotNull String password)
            throws AzureCmdException;

    void deleteStorageAccount(@NotNull StorageAccount storageAccount) throws AzureCmdException;

    @NotNull
    List<BlobContainer> getBlobContainers(@NotNull StorageAccount storageAccount) throws AzureCmdException;

    @NotNull
    BlobContainer createBlobContainer(@NotNull StorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException;

    void deleteBlobContainer(@NotNull StorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException;

    @NotNull
    BlobDirectory getRootDirectory(@NotNull StorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException;

    @NotNull
    List<BlobItem> getBlobItems(@NotNull StorageAccount storageAccount, @NotNull BlobDirectory blobDirectory)
            throws AzureCmdException;

    @NotNull
    BlobDirectory createBlobDirectory(@NotNull StorageAccount storageAccount,
                                      @NotNull BlobDirectory parentBlobDirectory,
                                      @NotNull BlobDirectory blobDirectory)
            throws AzureCmdException;

    @NotNull
    BlobFile createBlobFile(@NotNull StorageAccount storageAccount,
                            @NotNull BlobDirectory parentBlobDirectory,
                            @NotNull BlobFile blobFile)
            throws AzureCmdException;

    void deleteBlobFile(@NotNull StorageAccount storageAccount,
                        @NotNull BlobFile blobFile)
            throws AzureCmdException;


    public void uploadBlobFileContent(@NotNull StorageAccount storageAccount,
                                      @NotNull BlobContainer blobContainer,
                                      @NotNull String filePath,
                                      @NotNull InputStream content,
                                      CallableSingleArg<Void, Long> processBlockEvent,
                                      long maxBlockSize,
                                      long length)
            throws AzureCmdException;

    void downloadBlobFileContent(@NotNull StorageAccount storageAccount,
                                 @NotNull BlobFile blobFile,
                                 @NotNull OutputStream content)
            throws AzureCmdException;

    @NotNull
    List<Queue> getQueues(@NotNull StorageAccount storageAccount)
            throws AzureCmdException;

    @NotNull
    Queue createQueue(@NotNull StorageAccount storageAccount,
                      @NotNull Queue queue)
            throws AzureCmdException;

    void deleteQueue(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    @NotNull
    List<QueueMessage> getQueueMessages(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    void clearQueue(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    @NotNull
    QueueMessage createQueueMessage(@NotNull StorageAccount storageAccount,
                                    @NotNull QueueMessage queueMessage)
            throws AzureCmdException;
}