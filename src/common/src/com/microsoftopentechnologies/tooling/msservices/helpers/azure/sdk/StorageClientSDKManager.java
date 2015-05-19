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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk;

import com.microsoftopentechnologies.tooling.msservices.helpers.CallableSingleArg;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.model.storage.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface StorageClientSDKManager {
    @NotNull
    ClientStorageAccount getStorageAccount(@NotNull String connectionString);

    @NotNull
    List<BlobContainer> getBlobContainers(@NotNull ClientStorageAccount storageAccount) throws AzureCmdException;

    @NotNull
    BlobContainer createBlobContainer(@NotNull ClientStorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException;

    void deleteBlobContainer(@NotNull ClientStorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException;

    @NotNull
    BlobDirectory getRootDirectory(@NotNull ClientStorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException;

    @NotNull
    List<BlobItem> getBlobItems(@NotNull ClientStorageAccount storageAccount, @NotNull BlobDirectory blobDirectory)
            throws AzureCmdException;

    @NotNull
    BlobDirectory createBlobDirectory(@NotNull ClientStorageAccount storageAccount,
                                      @NotNull BlobDirectory parentBlobDirectory,
                                      @NotNull BlobDirectory blobDirectory)
            throws AzureCmdException;

    @NotNull
    BlobFile createBlobFile(@NotNull ClientStorageAccount storageAccount,
                            @NotNull BlobDirectory parentBlobDirectory,
                            @NotNull BlobFile blobFile)
            throws AzureCmdException;

    void deleteBlobFile(@NotNull ClientStorageAccount storageAccount,
                        @NotNull BlobFile blobFile)
            throws AzureCmdException;

    void uploadBlobFileContent(@NotNull ClientStorageAccount storageAccount,
                               @NotNull BlobContainer blobContainer,
                               @NotNull String filePath,
                               @NotNull InputStream content,
                               CallableSingleArg<Void, Long> processBlockEvent,
                               long maxBlockSize,
                               long length)
            throws AzureCmdException;

    void downloadBlobFileContent(@NotNull ClientStorageAccount storageAccount,
                                 @NotNull BlobFile blobFile,
                                 @NotNull OutputStream content)
            throws AzureCmdException;

    @NotNull
    List<Queue> getQueues(@NotNull ClientStorageAccount storageAccount)
            throws AzureCmdException;

    @NotNull
    Queue createQueue(@NotNull ClientStorageAccount storageAccount,
                      @NotNull Queue queue)
            throws AzureCmdException;

    void deleteQueue(@NotNull ClientStorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    @NotNull
    List<QueueMessage> getQueueMessages(@NotNull ClientStorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    void clearQueue(@NotNull ClientStorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    void createQueueMessage(@NotNull ClientStorageAccount storageAccount,
                            @NotNull QueueMessage queueMessage,
                            int timeToLiveInSeconds)
            throws AzureCmdException;

    @NotNull
    QueueMessage dequeueFirstQueueMessage(@NotNull ClientStorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException;

    @NotNull
    List<Table> getTables(@NotNull ClientStorageAccount storageAccount)
            throws AzureCmdException;

    @NotNull
    Table createTable(@NotNull ClientStorageAccount storageAccount,
                      @NotNull Table table)
            throws AzureCmdException;

    void deleteTable(@NotNull ClientStorageAccount storageAccount, @NotNull Table table)
            throws AzureCmdException;

    @NotNull
    List<TableEntity> getTableEntities(@NotNull ClientStorageAccount storageAccount, @NotNull Table table,
                                       @NotNull String filter)
            throws AzureCmdException;

    @NotNull
    TableEntity createTableEntity(@NotNull ClientStorageAccount storageAccount, @NotNull String tableName,
                                  @NotNull String partitionKey, @NotNull String rowKey,
                                  @NotNull Map<String, TableEntity.Property> properties)
            throws AzureCmdException;

    @NotNull
    TableEntity updateTableEntity(@NotNull ClientStorageAccount storageAccount, @NotNull TableEntity tableEntity)
            throws AzureCmdException;

    void deleteTableEntity(@NotNull ClientStorageAccount storageAccount, @NotNull TableEntity tableEntity)
            throws AzureCmdException;
}