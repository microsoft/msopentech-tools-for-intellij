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

import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.HostedServiceOperations;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class AzureSDKManagerImpl implements AzureSDKManager {

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId) throws AzureCmdException {
        List<VirtualMachine> vmList = new ArrayList<VirtualMachine>();

        try {
            ComputeManagementClient client = AzureSDKHelper.getComputeManagementClient(subscriptionId);
            if (client == null) {
                return vmList;
            }

            HostedServiceOperations hso = client.getHostedServicesOperations();

            if (hso == null) {
                return vmList;
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
                vmList = processDeploymentSlot(client, subscriptionId, hostedService, DeploymentSlot.Production, vmList);
                vmList = processDeploymentSlot(client, subscriptionId, hostedService, DeploymentSlot.Staging, vmList);
            }
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the VM list", t);
        }

        return vmList;
    }

    @NotNull
    private List<VirtualMachine> processDeploymentSlot(@NotNull ComputeManagementClient client, @NotNull String subscriptionId, @NotNull HostedService hostedService,
                                                       @NotNull DeploymentSlot slot, @NotNull List<VirtualMachine> vmList) throws IOException, ServiceException, ParserConfigurationException, SAXException, URISyntaxException {
        try {
            DeploymentGetResponse deployment = client.getDeploymentsOperations().getBySlot(hostedService.getServiceName(), slot);

            if (deployment == null) {
                return vmList;
            }

            for (Role role : deployment.getRoles()) {
                if (role.getRoleType() != null
                        && role.getRoleType().equals("PersistentVMRole")) {
                    VirtualMachine vm = new VirtualMachine(
                            role.getRoleName() != null ? role.getRoleName() : "",
                            hostedService.getServiceName() != null ? hostedService.getServiceName() : "",
                            deployment.getUri() != null ? deployment.getUri().toString() : "",
                            slot.toString(),
                            role.getRoleSize() != null ? role.getRoleSize() : "",
                            deployment.getStatus() != null ? deployment.getStatus().toString() : "",
                            subscriptionId);

                    vm = loadEndpoints(role, vm);

                    vmList.add(vm);
                }
            }
        } catch (ServiceException se) {
            if (se.getHttpStatusCode() != 404) {
                throw se;
            }
        }

        return vmList;
    }

    @NotNull
    private VirtualMachine loadEndpoints(@NotNull Role role, @NotNull VirtualMachine vm) {
        if (role.getConfigurationSets() == null) {
            return vm;
        }

        List<Endpoint> endpoints = vm.getEndpoints();

        for (ConfigurationSet configurationSet : role.getConfigurationSets()) {
            if (configurationSet.getConfigurationSetType() != null
                    && configurationSet.getConfigurationSetType().equals("NetworkConfiguration")
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
}