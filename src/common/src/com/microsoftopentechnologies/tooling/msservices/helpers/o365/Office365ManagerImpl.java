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

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.project.Project;
import com.microsoft.directoryservices.*;
import com.microsoft.directoryservices.odata.ApplicationFetcher;
import com.microsoft.directoryservices.odata.DirectoryClient;
import com.microsoft.directoryservices.odata.DirectoryObjectOperations;
import com.microsoft.services.odata.ODataCollectionFetcher;
import com.microsoft.services.odata.ODataEntityFetcher;
import com.microsoft.services.odata.ODataOperations;
import com.microsoftopentechnologies.tooling.msservices.components.AppSettingsNames;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.components.PluginSettings;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.Nullable;
import com.microsoftopentechnologies.tooling.msservices.helpers.StringHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.AADManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.auth.UserInfo;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.graph.PluginDependencyResolver;
import com.microsoftopentechnologies.tooling.msservices.helpers.graph.ServicePermissionEntry;
import com.microsoftopentechnologies.tooling.msservices.model.Office365Permission;
import com.microsoftopentechnologies.tooling.msservices.model.Office365PermissionList;
import com.microsoftopentechnologies.tooling.msservices.model.Office365Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Office365ManagerImpl implements Office365Manager {
    public class ServiceAppIds {
        public static final String EXCHANGE = "00000002-0000-0ff1-ce00-000000000000";
        public static final String SHARE_POINT = "00000003-0000-0ff1-ce00-000000000000";
        public static final String AZURE_ACTIVE_DIRECTORY = "00000002-0000-0000-c000-000000000000";
    }

    public static final String GRAPH_API_URI_TEMPLATE = "{base_uri}{tenant_domain}?api-version={api_version}";
    public static final String PROJECT_APP_ID = "com.microsoftopentechnologies.intellij.ProjectAppId";

    private static Office365Manager instance;
    private static Gson gson;

    private AADManager aadManager;

    private ReentrantReadWriteLock authDataLock = new ReentrantReadWriteLock(false);

    private UserInfo userInfo;
    private String accessToken;
    private DirectoryClient directoryDataServiceClient;

    private Office365ManagerImpl() {
        aadManager = AADManagerImpl.getManager();

        String json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.O365_USER_INFO);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                UserInfo userInfo = gson.fromJson(json, UserInfo.class);
                setUserInfo(userInfo);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.O365_USER_INFO);
            }
        }
    }

    @NotNull
    public static synchronized Office365Manager getManager() {
        if (instance == null) {
            gson = new Gson();
            instance = new Office365ManagerImpl();
        }

        return instance;
    }

    @Override
    public void authenticate() throws AzureCmdException {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

        UserInfo userInfo = aadManager.authenticate(settings.getGraphApiUri(), "Sign in to your Office 365 account");

        setUserInfo(userInfo);
    }

    @Override
    public boolean authenticated() {
        return getUserInfo() != null;
    }

    @Override
    public void clearAuthentication() {
        setUserInfo(null);
    }

    @NotNull
    @Override
    public ListenableFuture<List<Application>> getApplicationList() {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<Application>>>() {
            @Override
            public ListenableFuture<List<Application>> execute()
                    throws Throwable {
                return getAllObjects(getDirectoryClient().getapplications());
            }
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Application> getApplicationByObjectId(@NotNull final String objectId) {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<Application>>() {
            @Override
            public ListenableFuture<Application> execute()
                    throws Throwable {
                return getDirectoryClient().getapplications().getById(objectId).read();
            }
        });
    }

    @Override
    @NotNull
    public ListenableFuture<List<ServicePermissionEntry>> getO365PermissionsForApp(@NotNull final String objectId) {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<ServicePermissionEntry>>>() {
            @Override
            public ListenableFuture<List<ServicePermissionEntry>> execute()
                    throws Throwable {
                return Futures.transform(getApplicationByObjectId(objectId),
                        new AsyncFunction<Application, List<ServicePermissionEntry>>() {
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
    @NotNull
    public ListenableFuture<Application> setO365PermissionsForApp(
            @NotNull Application application,
            @NotNull List<ServicePermissionEntry> permissionEntryList) {
        List<RequiredResourceAccess> requiredResourceAccesses = application.getrequiredResourceAccess();
        if (requiredResourceAccesses == null) {
            application.setrequiredResourceAccess(requiredResourceAccesses = Lists.newArrayList());
        }

        for (ServicePermissionEntry permissionEntry : permissionEntryList) {
            final Office365Service service = permissionEntry.getKey();

            // filter permissions for enabled permissions
            Iterable<Office365Permission> permissionList = Iterables.filter(permissionEntry.getValue(),
                    new Predicate<Office365Permission>() {
                        @Override
                        public boolean apply(Office365Permission office365Permission) {
                            return office365Permission.isEnabled();
                        }
                    });

            // transform Office365Permission objects into ResourceAccess objects
            List<ResourceAccess> resourceAccessList = Lists.newArrayList(Iterables.transform(permissionList,
                    new Function<Office365Permission, ResourceAccess>() {
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
    @NotNull
    public ListenableFuture<Application> updateApplication(@NotNull final Application application) {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<Application>>() {
            @Override
            public ListenableFuture<Application> execute()
                    throws Throwable {
                ApplicationFetcher appFetcher = getDirectoryClient().getapplications().getById(application.getobjectId());
                return appFetcher.update(application);
            }
        });
    }

    @Override
    @NotNull
    public ListenableFuture<List<ServicePrincipal>> getServicePrincipals() {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<ServicePrincipal>>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute()
                    throws Throwable {
                return getAllObjects(getDirectoryClient().getservicePrincipals());
            }
        });
    }

    @Override
    @NotNull
    public ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForO365() {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<ServicePrincipal>>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute()
                    throws Throwable {
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

    @Override
    @NotNull
    public ListenableFuture<List<OAuth2PermissionGrant>> getPermissionGrants() {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<OAuth2PermissionGrant>>>() {
            @Override
            public ListenableFuture<List<OAuth2PermissionGrant>> execute()
                    throws Throwable {
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
    @NotNull
    public ListenableFuture<Application> registerApplication(@NotNull final Application application) {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<Application>>() {
            @Override
            public ListenableFuture<Application> execute()
                    throws Throwable {
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

    private ListenableFuture<Application> createServicePrincipalForApp(final Application application) throws AzureCmdException {
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
    @NotNull
    public ListenableFuture<Application> getApplicationForProject(@NotNull Project project) {
        final String appId = DefaultLoader.getIdeHelper().getProperty(project, PROJECT_APP_ID);
        if (StringHelper.isNullOrWhiteSpace(appId)) {
            return Futures.immediateFuture(null);
        }

        return requestFutureWithToken(new RequestCallback<ListenableFuture<Application>>() {
            @Override
            public ListenableFuture<Application> execute()
                    throws Throwable {
                return getFirstItem(
                        getDirectoryClient().
                                getapplications().
                                filter("appId eq '" + appId + "'").
                                read());
            }
        });
    }

    @Override
    public void setApplicationForProject(@NotNull Project project, @NotNull Application application) {
        DefaultLoader.getIdeHelper().setProperty(project, PROJECT_APP_ID, application.getappId());
        project.save();
    }

    @NotNull
    @Override
    public ListenableFuture<List<ServicePrincipal>> getServicePrincipalsForApp(@NotNull final Application application) {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<ServicePrincipal>>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute()
                    throws Throwable {
                return getDirectoryClient().
                        getservicePrincipals().
                        filter("appId eq '" + application.getappId() + "'").
                        read();
            }
        });
    }

    @Override
    @NotNull
    public ListenableFuture<List<ServicePrincipal>> getO365ServicePrincipalsForApp(@NotNull final Application application) {
        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<ServicePrincipal>>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute()
                    throws Throwable {
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
    @NotNull
    public ListenableFuture<List<ServicePrincipal>> addServicePrincipals(
            @NotNull final List<ServicePrincipal> servicePrincipals) {

        return requestFutureWithToken(new RequestCallback<ListenableFuture<List<ServicePrincipal>>>() {
            @Override
            public ListenableFuture<List<ServicePrincipal>> execute()
                    throws Throwable {
                List<ListenableFuture<ServicePrincipal>> futures = Lists.transform(
                        servicePrincipals,
                        new Function<ServicePrincipal, ListenableFuture<ServicePrincipal>>() {
                            @Override
                            public ListenableFuture<ServicePrincipal> apply(ServicePrincipal servicePrincipal) {
                                return getDirectoryClient().getservicePrincipals().add(servicePrincipal);
                            }
                        }
                );

                return Futures.allAsList(futures);
            }
        });
    }

    @Nullable
    private UserInfo getUserInfo() {
        authDataLock.readLock().lock();

        try {
            return userInfo;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setUserInfo(@Nullable UserInfo userInfo) {
        authDataLock.writeLock().lock();

        try {
            this.userInfo = userInfo;
            setAccessToken(null);

            String json = gson.toJson(userInfo, UserInfo.class);
            DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.O365_USER_INFO, json);
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @Nullable
    private String getAccessToken() {
        authDataLock.readLock().lock();

        try {
            return accessToken;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setAccessToken(@Nullable String accessToken) {
        authDataLock.writeLock().lock();

        try {
            this.accessToken = accessToken;

            DirectoryClient directoryDataServiceClient = null;

            if (accessToken != null) {
                PluginDependencyResolver dependencyResolver = new PluginDependencyResolver(accessToken);
                directoryDataServiceClient = new DirectoryClient(getGraphApiUri(), dependencyResolver);
            }

            setDirectoryDataServiceClient(directoryDataServiceClient);
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    // NOTE: The result of calling getDirectoryClient should never be cached. This is because of the following
    // reasons:
    //  [a] every directory client object is associated with an authentication token
    //  [b] as part of execution of the method, tokens might expire and be renewed in which case a new directory
    //      client will be instantiated; if we use cached objects then we'll continue using the client with the
    //      expired token instead of the new one
    private DirectoryClient getDirectoryClient() {
        authDataLock.readLock().lock();

        try {
            return directoryDataServiceClient;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setDirectoryDataServiceClient(DirectoryClient directoryDataServiceClient) {
        authDataLock.writeLock().lock();

        try {
            this.directoryDataServiceClient = directoryDataServiceClient;
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    private String getGraphApiUri() {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

        return GRAPH_API_URI_TEMPLATE.
                replace("{base_uri}", settings.getGraphApiUri()).
                replace("{tenant_domain}", getTenantDomain()).
                replace("{api_version}", settings.getGraphApiVersion());
    }

    private String getTenantDomain() {
        UserInfo userInfo = getUserInfo();

        if (userInfo == null) {
            throw new IllegalStateException("user is null");
        }

        return userInfo.getTenantId();
    }

    @NotNull
    private <V> ListenableFuture<V> requestFutureWithToken(@NotNull final RequestCallback<ListenableFuture<V>> requestCallback) {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

        com.microsoftopentechnologies.tooling.msservices.helpers.auth.RequestCallback<ListenableFuture<V>> aadRequestCB =
                new com.microsoftopentechnologies.tooling.msservices.helpers.auth.RequestCallback<ListenableFuture<V>>() {
                    @NotNull
                    @Override
                    public ListenableFuture<V> execute(@NotNull String accessToken)
                            throws Throwable {
                        if (!accessToken.equals(getAccessToken())) {
                            authDataLock.writeLock().lock();

                            try {
                                if (!accessToken.equals(getAccessToken())) {
                                    setAccessToken(accessToken);
                                }
                            } finally {
                                authDataLock.writeLock().unlock();
                            }
                        }

                        return requestCallback.execute();
                    }
                };

        return aadManager.requestFuture(userInfo,
                settings.getGraphApiUri(),
                "Sign in to your Office 365 account",
                aadRequestCB);
    }
}