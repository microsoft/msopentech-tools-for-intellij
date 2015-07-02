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
package com.microsoftopentechnologies.tooling.msservices.helpers.o365;

import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.project.Project;
import com.microsoft.directoryservices.Application;
import com.microsoft.directoryservices.OAuth2PermissionGrant;
import com.microsoft.directoryservices.ServicePrincipal;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.graph.ServicePermissionEntry;

import java.util.List;

public interface Office365Manager {
    void authenticate() throws AzureCmdException;

    boolean authenticated();

    void clearAuthentication();

    @NotNull
    ListenableFuture<List<Application>> getApplicationList();

    @NotNull
    ListenableFuture<Application> getApplicationByObjectId(@NotNull String objectId);

    @NotNull
    ListenableFuture<List<ServicePermissionEntry>> getO365PermissionsForApp(@NotNull String objectId);

    @NotNull
    ListenableFuture<Application> setO365PermissionsForApp(@NotNull Application application,
                                                           @NotNull List<ServicePermissionEntry> permissionEntryList);

    @NotNull
    ListenableFuture<Application> updateApplication(@NotNull Application application);

    @NotNull
    ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForO365();

    @NotNull
    ListenableFuture<List<ServicePrincipal>> getServicePrincipals();

    @NotNull
    ListenableFuture<List<OAuth2PermissionGrant>> getPermissionGrants();

    @NotNull
    ListenableFuture<Application> registerApplication(@NotNull Application application);

    void setApplicationForProject(@NotNull Project project, @NotNull Application application);

    @NotNull
    ListenableFuture<Application> getApplicationForProject(@NotNull Project project);

    @NotNull
    ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForApp(@NotNull Application application);

    @NotNull
    ListenableFuture<List<ServicePrincipal>> getO365ServicePrincipalsForApp(@NotNull Application application);

    @NotNull
    ListenableFuture<List<ServicePrincipal>> addServicePrincipals(@NotNull List<ServicePrincipal> servicePrincipals);
}