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

package com.microsoftopentechnologies.intellij.helpers.o365;

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;
import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.microsoft.directoryservices.*;
import com.microsoft.directoryservices.odata.ApplicationFetcher;
import com.microsoft.directoryservices.odata.DirectoryClient;
import com.microsoft.directoryservices.odata.DirectoryObjectOperations;
import com.microsoft.services.odata.ODataCollectionFetcher;
import com.microsoft.services.odata.ODataEntityFetcher;
import com.microsoft.services.odata.ODataOperations;
import com.microsoftopentechnologies.intellij.components.AppSettingsNames;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.components.PluginSettings;
import com.microsoftopentechnologies.intellij.helpers.StringHelper;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationContext;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.helpers.aadauth.PromptValue;
import com.microsoftopentechnologies.intellij.helpers.graph.PluginDependencyResolver;
import com.microsoftopentechnologies.intellij.helpers.graph.ServicePermissionEntry;
import com.microsoftopentechnologies.intellij.model.Office365Permission;
import com.microsoftopentechnologies.intellij.model.Office365PermissionList;
import com.microsoftopentechnologies.intellij.model.Office365Service;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class Office365RestAPIManager implements Office365Manager {
    public static final String GRAPH_API_URI_TEMPLATE = "{base_uri}{tenant_domain}?api-version={api_version}";
    public static final String PROJECT_APP_ID = "com.microsoftopentechnologies.intellij.ProjectAppId";

    private static Office365Manager instance;

    private AuthenticationResult authenticationToken;
    private ReentrantLock authenticationTokenLock = new ReentrantLock();

    private String tenantDomain;
    private ReentrantLock tenantDomainLock = new ReentrantLock();

    private String graphApiUri;
    private ReentrantLock graphApiUriLock = new ReentrantLock();

    private ReentrantLock refreshTokenLock = new ReentrantLock();

    private DirectoryClient directoryDataServiceClient;
    private ReentrantLock directoryDataServiceClientLock = new ReentrantLock();

    public class ServiceAppIds {
        public static final String EXCHANGE = "00000002-0000-0ff1-ce00-000000000000";
        public static final String SHARE_POINT = "00000003-0000-0ff1-ce00-000000000000";
        public static final String AZURE_ACTIVE_DIRECTORY = "00000002-0000-0000-c000-000000000000";
    }

    private Office365RestAPIManager() {
    }

    @NotNull
    public static Office365Manager getManager() {
        if (instance == null) {
            instance = new Office365RestAPIManager();
        }

        return instance;
    }

    @Override
    public boolean authenticated() throws ParseException {
        return getAuthenticationToken() != null;
    }

    private void resetState() {
        setTenantDomain(null);
        setGraphApiUri(null);
        setDirectoryDataServiceClient(null);
    }

    @Override
    public void setAuthenticationToken(AuthenticationResult token) {
        // if the authentication token is changing then pretty
        // much all other state needs to be reset
        resetState();

        if (token != null) {
            Gson gson = new Gson();
            String json = gson.toJson(token, AuthenticationResult.class);
            PropertiesComponent.getInstance().setValue(AppSettingsNames.O365_AUTHENTICATION_TOKEN, json);
        } else {
            PropertiesComponent.getInstance().unsetValue(AppSettingsNames.O365_AUTHENTICATION_TOKEN);
        }

        // reference assignments in java are atomic; so we don't need a
        // try/finally block here to ensure that the lock isn't left locked
        authenticationTokenLock.lock();
        authenticationToken = token;
        authenticationTokenLock.unlock();
    }

    @Override
    public AuthenticationResult getAuthenticationToken() {
        if (authenticationToken == null) {
            String json = PropertiesComponent.getInstance().getValue(AppSettingsNames.O365_AUTHENTICATION_TOKEN);
            if (!StringHelper.isNullOrWhiteSpace(json)) {
                Gson gson = new Gson();
                setAuthenticationToken(gson.fromJson(json, AuthenticationResult.class));
            }
        }
        return authenticationToken;
    }

    @Override
    public void authenticate() throws IOException, ExecutionException, InterruptedException, ParseException {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        AuthenticationContext context = null;
        try {
            context = new AuthenticationContext(settings.getAdAuthority());
            ListenableFuture<AuthenticationResult> future = context.acquireTokenInteractiveAsync(
                    authenticated() ? getTenantDomain() : settings.getTenantName(),
                    settings.getGraphApiUri(),
                    settings.getClientId(),
                    settings.getRedirectUri(),
                    null,
                    PromptValue.login);
            setAuthenticationToken(future.get());
        } finally {
            if (context != null) {
                context.dispose();
            }
        }
    }

    private String getGraphApiUri() throws ParseException {
        if (graphApiUri == null) {
            PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
            setGraphApiUri(GRAPH_API_URI_TEMPLATE.
                    replace("{base_uri}", settings.getGraphApiUri()).
                    replace("{tenant_domain}", getTenantDomain()).
                    replace("{api_version}", settings.getGraphApiVersion()));
        }

        return graphApiUri;
    }

    private void setGraphApiUri(String graphApiUri) {
        graphApiUriLock.lock();
        this.graphApiUri = graphApiUri;
        graphApiUriLock.unlock();
    }

    // NOTE: The result of calling getDirectoryClient should never be cached. This is because of the following
    // reasons:
    //  [a] every directory client object is associated with an authentication token
    //  [b] as part of execution of the method, tokens might expire and be renewed in which case a new directory
    //      client will be instantiated; if we use cached objects then we'll continue using the client with the
    //      expired token instead of the new one
    private DirectoryClient getDirectoryClient() throws ParseException {
        if (directoryDataServiceClient == null) {
            PluginDependencyResolver dependencyResolver = new PluginDependencyResolver(getAuthenticationToken().getAccessToken());
            setDirectoryDataServiceClient(new DirectoryClient(getGraphApiUri(), dependencyResolver));
        }

        return directoryDataServiceClient;
    }

    private void setDirectoryDataServiceClient(DirectoryClient directoryDataServiceClient) {
        directoryDataServiceClientLock.lock();
        this.directoryDataServiceClient = directoryDataServiceClient;
        directoryDataServiceClientLock.unlock();
    }

    private <T> void requestWithInteractiveToken(
            RequestCallback<T> requestCallback,
            final SettableFuture<T> wrappedFuture) throws ParseException {

        // do interactive auth
        try {
            authenticate();
        } catch (IOException e) {
            wrappedFuture.setException(e);
            return;
        } catch (ExecutionException e) {
            wrappedFuture.setException(e);
            return;
        } catch (InterruptedException e) {
            wrappedFuture.setException(e);
            return;
        } catch (ParseException e) {
            wrappedFuture.setException(e);
            return;
        }

        Futures.addCallback(requestCallback.execute(), new FutureCallback<T>() {
            @Override
            public void onSuccess(T val) {
                wrappedFuture.set(val);
            }

            @Override
            public void onFailure(Throwable throwable) {
                wrappedFuture.setException(throwable);
            }
        });
    }

    private <T> void requestWithRefreshToken(
            final RequestCallback<T> requestCallback,
            final SettableFuture<T> wrappedFuture) throws ParseException {

        // acquire token via refresh token
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        AuthenticationContext context = null;

        // Now there might be multiple concurrent requests all of which are likely
        // to end up needing a new access token all at the same time. In this case
        // we don't want to issue multiple requests redeeming refresh tokens as that
        // is wasteful. To prevent that we serialize the code block below using a
        // re-entrant lock. We do this by first acquiring the current authentication
        // token before acquiring the lock which may or may not have a value. After the
        // lock has been acquired, the following are the possibilities:
        //    +-------------------+------------------+--------+----------------------+-------------------------+
        //    | Value before lock | Value after lock | Equal? | Issue Token Request? |         Remarks         |
        //    +-------------------+------------------+--------+----------------------+-------------------------+
        //    | null              | null             | yes    | yes                  |                         |
        //    | null              | not null         | no     | no                   |                         |
        //    | not null          | null             | no     | no                   | This indicates an error |
        //    | not null          | not null         | no     | no                   |                         |
        //    | not null          | not null         | yes    | yes                  |                         |
        //    +-------------------+------------------+--------+----------------------+-------------------------+
        AuthenticationResult token = getAuthenticationToken();
        refreshTokenLock.lock();
        try {
            if (AuthenticationResult.equals(token, getAuthenticationToken())) {
                context = new AuthenticationContext(settings.getAdAuthority());
                setAuthenticationToken(context.acquireTokenByRefreshToken(
                        getAuthenticationToken(),
                        getTenantDomain(),
                        settings.getGraphApiUri(),
                        settings.getClientId()));
            }
        } catch (IOException e) {
            wrappedFuture.setException(e);
            return;
        } catch (ParseException e) {
            wrappedFuture.setException(e);
            return;
        } finally {
            // we release the lock before we do anything else as
            // we don't want a lock leaking in case the statements
            // following throw
            refreshTokenLock.unlock();
            if (context != null) {
                context.dispose();
            }
        }

        Futures.addCallback(requestCallback.execute(), new FutureCallback<T>() {
            @Override
            public void onSuccess(T val) {
                wrappedFuture.set(val);
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (isErrorUnauthorized(throwable)) {
                    try {
                        requestWithInteractiveToken(requestCallback, wrappedFuture);
                    } catch (ParseException e) {
                        wrappedFuture.setException(throwable);
                    }
                } else {
                    wrappedFuture.setException(throwable);
                }
            }
        });
    }

    private <T> ListenableFuture<T> requestWithToken(final RequestCallback<T> requestCallback) throws ParseException {
        final SettableFuture<T> wrappedFuture = SettableFuture.create();
        Futures.addCallback(requestCallback.execute(), new FutureCallback<T>() {
            @Override
            public void onSuccess(T val) {
                wrappedFuture.set(val);
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (isErrorUnauthorized(throwable)) {
                    try {
                        requestWithRefreshToken(requestCallback, wrappedFuture);
                    } catch (ParseException e) {
                        wrappedFuture.setException(throwable);
                    }
                } else {
                    wrappedFuture.setException(throwable);
                }
            }
        });

        return wrappedFuture;
    }

    private boolean isErrorUnauthorized(Throwable throwable) {
        return throwable.getMessage().contains("Authentication_ExpiredToken");
    }

    @NotNull
    @Override
    public ListenableFuture<List<Application>> getApplicationList() throws ParseException {
        return requestWithToken(new RequestCallback<List<Application>>() {
            @Override
            public ListenableFuture<List<Application>> execute() throws ParseException {
                return getAllObjects(getDirectoryClient().getapplications());
            }
        });
    }

    @Override
    public ListenableFuture<Application> getApplicationByObjectId(final String objectId) throws ParseException {
        return requestWithToken(new RequestCallback<Application>() {
            @Override
            public ListenableFuture<Application> execute() throws ParseException {
                return getDirectoryClient().getapplications().getById(objectId).read();
            }
        });
    }

    @Override
    public ListenableFuture<List<ServicePermissionEntry>> getO365PermissionsForApp(final String objectId) throws ParseException {
        return requestWithToken(new RequestCallback<List<ServicePermissionEntry>>() {
            @Override
            public ListenableFuture<List<ServicePermissionEntry>> execute() throws ParseException {
                return Futures.transform(getApplicationByObjectId(objectId), new AsyncFunction<Application, List<ServicePermissionEntry>>() {
                    @Override
                    public ListenableFuture<List<ServicePermissionEntry>> apply(Application application) throws Exception {

                        final String[] filterAppIds = new String[]{
                                ServiceAppIds.SHARE_POINT,
                                ServiceAppIds.EXCHANGE,
                                ServiceAppIds.AZURE_ACTIVE_DIRECTORY
                        };

                        // build initial list of permission from the app's permissions
                        final List<ServicePermissionEntry> servicePermissions = getO365PermissionsFromResourceAccess(application.getrequiredResourceAccess(), filterAppIds);

                        // get permissions list from O365 service principals
                        return Futures.transform(getServicePrincipalsForO365(), new AsyncFunction<List<ServicePrincipal>, List<ServicePermissionEntry>>() {
                            @Override
                            public ListenableFuture<List<ServicePermissionEntry>> apply(List<ServicePrincipal> servicePrincipals) throws Exception {

                                for (final ServicePrincipal servicePrincipal : servicePrincipals) {
                                    // lookup this service principal in app's list of resources; if it's not found add an entry
                                    ServicePermissionEntry servicePermissionEntry = Iterables.find(servicePermissions, new Predicate<ServicePermissionEntry>() {
                                        @Override
                                        public boolean apply(ServicePermissionEntry servicePermissionEntry) {
                                            return servicePermissionEntry.getKey().getId().equals(servicePrincipal.getappId());
                                        }
                                    }, null);

                                    if (servicePermissionEntry == null) {
                                        servicePermissions.add(servicePermissionEntry = new ServicePermissionEntry(
                                                new Office365Service(),
                                                new Office365PermissionList()
                                        ));
                                    }

                                    Office365Service service = servicePermissionEntry.getKey();
                                    Office365PermissionList permissionList = servicePermissionEntry.getValue();
                                    service.setId(servicePrincipal.getappId());
                                    service.setName(servicePrincipal.getdisplayName());

                                    List<OAuth2Permission> permissions = servicePrincipal.getoauth2Permissions();
                                    for (final OAuth2Permission permission : permissions) {
                                        // lookup permission in permissionList
                                        Office365Permission office365Permission = Iterables.find(permissionList, new Predicate<Office365Permission>() {
                                            @Override
                                            public boolean apply(Office365Permission office365Permission) {
                                                return office365Permission.getId().equals(permission.getid().toString());
                                            }
                                        }, null);

                                        if (office365Permission == null) {
                                            permissionList.add(office365Permission = new Office365Permission());
                                            office365Permission.setEnabled(false);
                                        }

                                        office365Permission.setId(permission.getid().toString());
                                        office365Permission.setName(getPermissionDisplayName(permission.getvalue()));
                                        office365Permission.setDescription(permission.getuserConsentDisplayName());
                                    }
                                }

                                return Futures.immediateFuture(servicePermissions);
                            }
                        });
                    }
                });
            }
        });
    }

    private String getPermissionDisplayName(String displayName) {
        // replace '.' and '_' with space characters and title case the display name
        return Joiner.on(' ').
                join(Iterables.transform(
                                Splitter.on(' ').split(
                                        CharMatcher.anyOf("._").
                                                replaceFrom(displayName, ' ')),
                                new Function<String, String>() {
                                    @Override
                                    public String apply(String str) {
                                        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
                                    }
                                }
                        )
                );
    }

    private List<ServicePermissionEntry> getO365PermissionsFromResourceAccess(
            List<RequiredResourceAccess> requiredResourceAccesses,
            String[] filterAppIds) {

        List<ServicePermissionEntry> entryList = Lists.newArrayList();
        if (requiredResourceAccesses == null) {
            return entryList;
        }

        for (final RequiredResourceAccess requiredResourceAccess : requiredResourceAccesses) {
            // we're interested in this resource only if it is one of the app id's in "filterAppIds"
            boolean isO365Resource = Iterators.any(Iterators.forArray(filterAppIds), new Predicate<String>() {
                @Override
                public boolean apply(String appId) {
                    return requiredResourceAccess.getresourceAppId().equals(appId);
                }
            });
            if (!isO365Resource) {
                continue;
            }

            Office365Service service = new Office365Service();
            Office365PermissionList permissions = new Office365PermissionList();
            entryList.add(new ServicePermissionEntry(service, permissions));

            service.setId(requiredResourceAccess.getresourceAppId());
            List<ResourceAccess> resourceAccesses = requiredResourceAccess.getresourceAccess();
            if (resourceAccesses == null) {
                continue;
            }
            for (ResourceAccess resourceAccess : resourceAccesses) {
                Office365Permission permission = new Office365Permission(
                        resourceAccess.getid().toString(),
                        "", "",
                        resourceAccess.gettype().equals("Scope")
                );
                permissions.add(permission);
            }
        }

        return entryList;
    }

    @Override
    public ListenableFuture<Application> setO365PermissionsForApp(Application application, List<ServicePermissionEntry> permissionEntryList) throws ParseException {
        List<RequiredResourceAccess> requiredResourceAccesses = application.getrequiredResourceAccess();
        if (requiredResourceAccesses == null) {
            application.setrequiredResourceAccess(requiredResourceAccesses = Lists.newArrayList());
        }

        for (ServicePermissionEntry permissionEntry : permissionEntryList) {
            final Office365Service service = permissionEntry.getKey();

            // filter permissions for enabled permissions
            Iterable<Office365Permission> permissionList = Iterables.filter(permissionEntry.getValue(), new Predicate<Office365Permission>() {
                @Override
                public boolean apply(Office365Permission office365Permission) {
                    return office365Permission.isEnabled();
                }
            });

            // transform Office365Permission objects into ResourceAccess objects
            List<ResourceAccess> resourceAccessList = Lists.newArrayList(Iterables.transform(permissionList, new Function<Office365Permission, ResourceAccess>() {
                @Override
                public ResourceAccess apply(Office365Permission office365Permission) {
                    ResourceAccess resourceAccess = new ResourceAccess();
                    resourceAccess.setid(UUID.fromString(office365Permission.getId()));
                    resourceAccess.settype("Scope");
                    return resourceAccess;
                }
            }));

            // get reference to service from app in case it exists
            RequiredResourceAccess requiredResourceAccess = Iterables.find(requiredResourceAccesses, new Predicate<RequiredResourceAccess>() {
                @Override
                public boolean apply(RequiredResourceAccess requiredResourceAccess) {
                    return requiredResourceAccess.getresourceAppId().equals(service.getId());
                }
            }, null);

            if (requiredResourceAccess == null && !resourceAccessList.isEmpty()) {
                requiredResourceAccesses.add(requiredResourceAccess = new RequiredResourceAccess());
                requiredResourceAccess.setresourceAppId(service.getId());
            }

            if (requiredResourceAccess != null) {
                if (resourceAccessList.isEmpty()) {
                    // remove requiredResourceAccess from requiredResourceAccesses
                    requiredResourceAccesses.remove(requiredResourceAccess);
                } else {
                    requiredResourceAccess.setresourceAccess(resourceAccessList);
                }
            }
        }

        return updateApplication(application);
    }

    @Override
    public ListenableFuture<Application> updateApplication(final Application application) throws ParseException {
        return requestWithToken(new RequestCallback<Application>() {
            @Override
            public ListenableFuture<Application> execute() throws ParseException {
                ApplicationFetcher appFetcher = getDirectoryClient().getapplications().getById(application.getobjectId());
                return appFetcher.update(application);
            }
        });
    }

    @NotNull
    @Override
    public ListenableFuture<List<ServicePrincipal>> getServicePrincipals() throws ParseException {
        return requestWithToken(new RequestCallback<List<ServicePrincipal>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute() throws ParseException {
                return getAllObjects(getDirectoryClient().getservicePrincipals());
            }
        });
    }

    public ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForO365() throws ParseException {
        return requestWithToken(new RequestCallback<List<ServicePrincipal>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute() throws ParseException {
                // build the filter
                String[] appIds = new String[]{
                        ServiceAppIds.AZURE_ACTIVE_DIRECTORY,
                        ServiceAppIds.EXCHANGE,
                        ServiceAppIds.SHARE_POINT
                };
                String filter = "appId eq '" + Joiner.on("' or appId eq '").join(appIds) + "'";
                return getDirectoryClient().getservicePrincipals().filter(filter).read();
            }
        });
    }

    private <T> ListenableFuture<T> getFirstItem(ListenableFuture<List<T>> future) {
        return Futures.transform(future, new AsyncFunction<List<T>, T>() {
            @Override
            public ListenableFuture<T> apply(List<T> items) throws Exception {
                return Futures.immediateFuture((items != null && items.size() > 0) ? items.get(0) : null);
            }
        });
    }

    public ListenableFuture<List<OAuth2PermissionGrant>> getPermissionGrants() throws ParseException {
        return requestWithToken(new RequestCallback<List<OAuth2PermissionGrant>>() {
            @Override
            public ListenableFuture<List<OAuth2PermissionGrant>> execute() throws ParseException {
                return getDirectoryClient().getoauth2PermissionGrants().read();
            }
        });
    }

    private <E extends DirectoryObject, F extends ODataEntityFetcher<E, ? extends DirectoryObjectOperations>, O extends ODataOperations>
    ListenableFuture<List<E>> getAllObjects(final ODataCollectionFetcher<E, F, O> fetcher) {

        return Futures.transform(fetcher.read(), new AsyncFunction<List<E>, List<E>>() {
            @Override
            public ListenableFuture<List<E>> apply(List<E> entities) throws Exception {
                return Futures.successfulAsList(Lists.transform(entities, new Function<E, ListenableFuture<? extends E>>() {
                    @Override
                    public ListenableFuture<? extends E> apply(E e) {
                        return fetcher.getById(e.getobjectId()).read();
                    }
                }));
            }
        });
    }

    @Override
    public ListenableFuture<Application> registerApplication(@NotNull final Application application) throws ParseException {
        return requestWithToken(new RequestCallback<Application>() {
            @Override
            public ListenableFuture<Application> execute() throws ParseException {
                // register the app and then create a service principal for the app if there isn't already one
                return Futures.transform(getDirectoryClient().getapplications().add(application), new AsyncFunction<Application, Application>() {
                    @Override
                    public ListenableFuture<Application> apply(final Application application) throws Exception {
                        return Futures.transform(getServicePrincipalsForApp(application), new AsyncFunction<List<ServicePrincipal>, Application>() {
                            @Override
                            public ListenableFuture<Application> apply(List<ServicePrincipal> servicePrincipals) throws Exception {
                                if (servicePrincipals.size() == 0) {
                                    return createServicePrincipalForApp(application);
                                }

                                return Futures.immediateFuture(application);
                            }
                        });
                    }
                });
            }
        });
    }

    private ListenableFuture<Application> createServicePrincipalForApp(final Application application) throws ParseException {
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setappId(application.getappId());

        servicePrincipal.setaccountEnabled(true);

        return Futures.transform(getDirectoryClient().getservicePrincipals().add(servicePrincipal), new AsyncFunction<ServicePrincipal, Application>() {
            @Override
            public ListenableFuture<Application> apply(ServicePrincipal servicePrincipal) throws Exception {
                return Futures.immediateFuture(application);
            }
        });
    }

    @Override
    public ListenableFuture<Application> getApplicationForProject(Project project) throws ParseException {
        final String appId = PropertiesComponent.getInstance(project).getValue(PROJECT_APP_ID);
        if (StringHelper.isNullOrWhiteSpace(appId)) {
            return Futures.immediateFuture(null);
        }

        return requestWithToken(new RequestCallback<Application>() {
            @Override
            public ListenableFuture<Application> execute() throws ParseException {
                return getFirstItem(
                        getDirectoryClient().
                                getapplications().
                                filter("appId eq '" + appId + "'").
                                read());
            }
        });
    }

    @Override
    public void setApplicationForProject(Project project, Application application) {
        PropertiesComponent.getInstance(project).setValue(PROJECT_APP_ID, application.getappId());
    }

    @NotNull
    @Override
    public ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForApp(@NotNull final Application application) throws ParseException {
        return requestWithToken(new RequestCallback<List<ServicePrincipal>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute() throws ParseException {
                return getDirectoryClient().
                        getservicePrincipals().
                        filter("appId eq '" + application.getappId() + "'").
                        read();
            }
        });
    }

    @Override
    public ListenableFuture<List<ServicePrincipal>> getO365ServicePrincipalsForApp(@NotNull final Application application) throws ParseException {
        return requestWithToken(new RequestCallback<List<ServicePrincipal>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute() throws ParseException {
                @SuppressWarnings("unchecked")
                ListenableFuture<List<ServicePrincipal>>[] futures = new ListenableFuture[]{
                        getServicePrincipalsForApp(application),
                        getServicePrincipalsForO365()
                };

                final String[] filterAppIds = new String[]{
                        ServiceAppIds.SHARE_POINT,
                        ServiceAppIds.EXCHANGE,
                        ServiceAppIds.AZURE_ACTIVE_DIRECTORY
                };

                return Futures.transform(Futures.allAsList(futures), new AsyncFunction<List<List<ServicePrincipal>>, List<ServicePrincipal>>() {
                    @Override
                    public ListenableFuture<List<ServicePrincipal>> apply(List<List<ServicePrincipal>> lists) throws Exception {
                        // According to Guava documentation for allAsList, the list of results is in the
                        // same order as the input list. So first we get the service principals for the app
                        // filtered for O365 and Graph service principals.
                        final List<ServicePrincipal> servicePrincipalsForApp = Lists.newArrayList(Iterables.filter(lists.get(0), new Predicate<ServicePrincipal>() {
                            @Override
                            public boolean apply(final ServicePrincipal servicePrincipal) {
                                // we are only interested in O365 and Graph service principals
                                return Iterators.any(Iterators.forArray(filterAppIds), new Predicate<String>() {
                                    @Override
                                    public boolean apply(String appId) {
                                        return appId.equals(servicePrincipal.getappId());
                                    }
                                });
                            }
                        }));

                        // next we get the O365/graph service principals
                        final List<ServicePrincipal> servicePrincipalsForO365 = lists.get(1);

                        // then we add service principals from servicePrincipalsForO365 to servicePrincipalsForApp
                        // where the service principal is not available in the latter
                        Iterable<ServicePrincipal> servicePrincipalsToBeAdded = Iterables.filter(servicePrincipalsForO365, new Predicate<ServicePrincipal>() {
                            @Override
                            public boolean apply(ServicePrincipal servicePrincipal) {
                                return !servicePrincipalsForApp.contains(servicePrincipal);
                            }
                        });
                        Iterables.addAll(servicePrincipalsForApp, servicePrincipalsToBeAdded);

                        // assign the appid to the service principal and reset permissions on new service principals;
                        // we do Lists.newArrayList calls below to create a copy of the service lists because Lists.transform
                        // invokes the transformation function lazily and this causes problems for us; we force immediate
                        // evaluation of our transfomer by copying the elements to a new list
                        List<ServicePrincipal> servicePrincipals = Lists.newArrayList(Lists.transform(servicePrincipalsForApp, new Function<ServicePrincipal, ServicePrincipal>() {
                            @Override
                            public ServicePrincipal apply(ServicePrincipal servicePrincipal) {
                                if (!servicePrincipal.getappId().equals(application.getappId())) {
                                    servicePrincipal.setappId(application.getappId());
                                    servicePrincipal.setoauth2Permissions(Lists.newArrayList(Lists.transform(servicePrincipal.getoauth2Permissions(), new Function<OAuth2Permission, OAuth2Permission>() {
                                        @Override
                                        public OAuth2Permission apply(OAuth2Permission oAuth2Permission) {
                                            oAuth2Permission.setisEnabled(false);
                                            return oAuth2Permission;
                                        }
                                    })));
                                }

                                return servicePrincipal;
                            }
                        }));

                        return Futures.immediateFuture(servicePrincipals);
                    }
                });
            }
        });
    }

    @Override
    public ListenableFuture<List<ServicePrincipal>> addServicePrincipals(
            @NotNull final List<ServicePrincipal> servicePrincipals) throws ParseException {

        return requestWithToken(new RequestCallback<List<ServicePrincipal>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute() throws ParseException {
                List<ListenableFuture<ServicePrincipal>> futures = Lists.transform(
                        servicePrincipals,
                        new Function<ServicePrincipal, ListenableFuture<ServicePrincipal>>() {
                            @Override
                            public ListenableFuture<ServicePrincipal> apply(ServicePrincipal servicePrincipal) {
                                try {
                                    return getDirectoryClient().getservicePrincipals().add(servicePrincipal);
                                } catch (ParseException e) {
                                    return Futures.immediateFailedFuture(e);
                                }
                            }
                        }
                );

                return Futures.allAsList(futures);
            }
        });
    }

    private String getTenantDomain() throws ParseException {
        if (authenticationToken == null) {
            throw new IllegalStateException("authenticationToken is null");
        }

        if (tenantDomain == null) {
            String upn = authenticationToken.getUserInfo().getUpn();
            if (!StringHelper.isNullOrWhiteSpace(upn)) {
                ArrayList<String> tokens = Lists.newArrayList(Splitter.on('@').split(upn));
                if (tokens.size() != 2) {
                    throw new ParseException("Invalid UPN format in authentication token.", 0);
                }

                setTenantDomain(tokens.get(1));
            }
        }

        return tenantDomain;
    }

    private void setTenantDomain(String tenantDomain) {
        tenantDomainLock.lock();
        this.tenantDomain = tenantDomain;
        tenantDomainLock.unlock();
    }
}