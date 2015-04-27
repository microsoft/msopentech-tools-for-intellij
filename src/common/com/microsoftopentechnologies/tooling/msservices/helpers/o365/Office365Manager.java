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

package com.microsoftopentechnologies.tooling.msservices.helpers.o365;

import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.project.Project;
import com.microsoft.directoryservices.Application;
import com.microsoft.directoryservices.OAuth2PermissionGrant;
import com.microsoft.directoryservices.ServicePrincipal;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.tooling.msservices.helpers.graph.ServicePermissionEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public interface Office365Manager {
    void setAuthenticationToken(AuthenticationResult token);
    AuthenticationResult getAuthenticationToken();

    boolean authenticated() throws ParseException;

    void authenticate() throws IOException, ExecutionException, InterruptedException, ParseException;

    @NotNull
    ListenableFuture<List<Application>> getApplicationList() throws ParseException;

    ListenableFuture<Application> getApplicationByObjectId(String objectId) throws ParseException;

    ListenableFuture<List<ServicePermissionEntry>> getO365PermissionsForApp(String objectId) throws ParseException;

    ListenableFuture<Application> setO365PermissionsForApp(Application application, List<ServicePermissionEntry> permissionEntryList) throws ParseException;

    ListenableFuture<Application> updateApplication(Application application) throws ParseException;

    ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForO365() throws ParseException;

    ListenableFuture<List<ServicePrincipal>> getServicePrincipals() throws ParseException;

    ListenableFuture<List<OAuth2PermissionGrant>> getPermissionGrants() throws ParseException;

    ListenableFuture<Application> registerApplication(@NotNull Application application) throws ParseException;

    void setApplicationForProject(Project project, Application application);

    ListenableFuture<Application> getApplicationForProject(Project project) throws ParseException;

    @NotNull
    ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForApp(@NotNull Application application) throws ParseException;

    ListenableFuture<List<ServicePrincipal>> getO365ServicePrincipalsForApp(@NotNull final Application application) throws ParseException;

    ListenableFuture<List<ServicePrincipal>> addServicePrincipals(@NotNull List<ServicePrincipal> servicePrincipals) throws ParseException;
}