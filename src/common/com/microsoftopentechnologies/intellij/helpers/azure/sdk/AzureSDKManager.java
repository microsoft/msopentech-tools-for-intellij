/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.helpers.azure.sdk;

import com.microsoftopentechnologies.intellij.helpers.CallableSingleArg;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.model.storage.*;
import com.microsoftopentechnologies.intellij.model.storage.TableEntity.Property;
import com.microsoftopentechnologies.intellij.model.vm.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface AzureSDKManager {

    List<CloudService> getCloudServices(String subscriptionId) throws AzureCmdException;


    List<VirtualMachine> getVirtualMachines(String subscriptionId) throws AzureCmdException;


    VirtualMachine refreshVirtualMachineInformation(VirtualMachine vm) throws AzureCmdException;

    void startVirtualMachine(VirtualMachine vm) throws AzureCmdException;

    void shutdownVirtualMachine(VirtualMachine vm, boolean deallocate) throws AzureCmdException;

    void restartVirtualMachine(VirtualMachine vm) throws AzureCmdException;

    void deleteVirtualMachine(VirtualMachine vm, boolean deleteFromStorage) throws AzureCmdException;


    byte[] downloadRDP(VirtualMachine vm) throws AzureCmdException;


    List<StorageAccount> getStorageAccounts(String subscriptionId) throws AzureCmdException;


    List<VirtualMachineImage> getVirtualMachineImages(String subscriptionId) throws AzureCmdException;


    List<VirtualMachineSize> getVirtualMachineSizes(String subscriptionId) throws AzureCmdException;


    List<Location> getLocations(String subscriptionId) throws AzureCmdException;


    List<AffinityGroup> getAffinityGroups(String subscriptionId) throws AzureCmdException;


    List<VirtualNetwork> getVirtualNetworks(String subscriptionId) throws AzureCmdException;

    void createStorageAccount(StorageAccount storageAccount) throws AzureCmdException;

    void createCloudService(CloudService cloudService) throws AzureCmdException;

    void createVirtualMachine(VirtualMachine virtualMachine, VirtualMachineImage vmImage,
                              StorageAccount storageAccount, String virtualNetwork,
                              String username, String password, byte[] certificate)
            throws AzureCmdException;

    void createVirtualMachine(VirtualMachine virtualMachine, VirtualMachineImage vmImage,
                              String mediaLocation, String virtualNetwork,
                              String username, String password, byte[] certificate)
            throws AzureCmdException;


    StorageAccount refreshStorageAccountInformation(StorageAccount storageAccount) throws AzureCmdException;

    String createServiceCertificate(String subscriptionId, String serviceName,
                                    byte[] data, String password)
            throws AzureCmdException;

    void deleteStorageAccount(StorageAccount storageAccount) throws AzureCmdException;


    List<BlobContainer> getBlobContainers(StorageAccount storageAccount) throws AzureCmdException;


    BlobContainer createBlobContainer(StorageAccount storageAccount, BlobContainer blobContainer)
            throws AzureCmdException;

    void deleteBlobContainer(StorageAccount storageAccount, BlobContainer blobContainer)
            throws AzureCmdException;


    BlobDirectory getRootDirectory(StorageAccount storageAccount, BlobContainer blobContainer)
            throws AzureCmdException;


    List<BlobItem> getBlobItems(StorageAccount storageAccount, BlobDirectory blobDirectory)
            throws AzureCmdException;


    BlobDirectory createBlobDirectory(StorageAccount storageAccount,
                                      BlobDirectory parentBlobDirectory,
                                      BlobDirectory blobDirectory)
            throws AzureCmdException;


    BlobFile createBlobFile(StorageAccount storageAccount,
                            BlobDirectory parentBlobDirectory,
                            BlobFile blobFile)
            throws AzureCmdException;

    void deleteBlobFile(StorageAccount storageAccount,
                        BlobFile blobFile)
            throws AzureCmdException;


    void uploadBlobFileContent(StorageAccount storageAccount,
                               BlobContainer blobContainer,
                               String filePath,
                               InputStream content,
                               CallableSingleArg<Void, Long> processBlockEvent,
                               long maxBlockSize,
                               long length)
            throws AzureCmdException;

    void downloadBlobFileContent(StorageAccount storageAccount,
                                 BlobFile blobFile,
                                 OutputStream content)
            throws AzureCmdException;


    List<Queue> getQueues(StorageAccount storageAccount)
            throws AzureCmdException;


    Queue createQueue(StorageAccount storageAccount,
                      Queue queue)
            throws AzureCmdException;

    void deleteQueue(StorageAccount storageAccount, Queue queue)
            throws AzureCmdException;


    List<QueueMessage> getQueueMessages(StorageAccount storageAccount, Queue queue)
            throws AzureCmdException;

    void clearQueue(StorageAccount storageAccount, Queue queue)
            throws AzureCmdException;

    void createQueueMessage(StorageAccount storageAccount,
                            QueueMessage queueMessage,
                            int timeToLiveInSeconds)
            throws AzureCmdException;


    QueueMessage dequeueFirstQueueMessage(StorageAccount storageAccount, Queue queue)
            throws AzureCmdException;


    List<Table> getTables(StorageAccount storageAccount)
            throws AzureCmdException;


    Table createTable(StorageAccount storageAccount,
                      Table table)
            throws AzureCmdException;

    void deleteTable(StorageAccount storageAccount, Table table)
            throws AzureCmdException;


    List<TableEntity> getTableEntities(StorageAccount storageAccount, Table table,
                                       String filter)
            throws AzureCmdException;


    TableEntity createTableEntity(StorageAccount storageAccount, String tableName,
                                  String partitionKey, String rowKey,
                                  Map<String, Property> properties)
            throws AzureCmdException;


    TableEntity updateTableEntity(StorageAccount storageAccount, TableEntity tableEntity)
            throws AzureCmdException;

    void deleteTableEntity(StorageAccount storageAccount, TableEntity tableEntity)
            throws AzureCmdException;
}