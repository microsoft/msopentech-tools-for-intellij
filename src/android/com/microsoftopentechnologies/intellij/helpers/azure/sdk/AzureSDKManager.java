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

import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AzureSDKManager {
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
    List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId) throws AzureCmdException;
}