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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.Nullable;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.RestServiceManager.ContentType;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.RestServiceManager.HttpsURLConnectionProvider;

import javax.net.ssl.HttpsURLConnection;

public class AzureAADHelper {
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @NotNull
    public static String executeRequest(@NotNull String managementUrl,
                                        @NotNull String path,
                                        @NotNull ContentType contentType,
                                        @NotNull String method,
                                        @Nullable String postData,
                                        @NotNull String accessToken,
                                        @NotNull RestServiceManager manager)
            throws AzureCmdException {
        HttpsURLConnectionProvider sslConnectionProvider = getHttpsURLConnectionProvider(accessToken, manager);

        return manager.executeRequest(managementUrl, path, contentType, method, postData, sslConnectionProvider);
    }

    @NotNull
    public static String executePollRequest(@NotNull String managementUrl,
                                            @NotNull String path,
                                            @NotNull ContentType contentType,
                                            @NotNull String method,
                                            @Nullable String postData,
                                            @NotNull String pollPath,
                                            @NotNull String accessToken,
                                            @NotNull RestServiceManager manager)
            throws AzureCmdException {
        HttpsURLConnectionProvider sslConnectionProvider = getHttpsURLConnectionProvider(accessToken, manager);

        return manager.executePollRequest(managementUrl, path, contentType, method, postData, pollPath, sslConnectionProvider);
    }

    @NotNull
    private static HttpsURLConnectionProvider getHttpsURLConnectionProvider(
            @NotNull final String accessToken,
            @NotNull final RestServiceManager manager) {
        return new HttpsURLConnectionProvider() {
            @Override
            @NotNull
            public HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                                       @NotNull String path,
                                                       @NotNull ContentType contentType)
                    throws AzureCmdException {
                HttpsURLConnection sslConnection = manager.getSSLConnection(managementUrl, path, contentType);
                sslConnection.addRequestProperty(AUTHORIZATION_HEADER, "Bearer " + accessToken);

                return sslConnection;
            }
        };
    }
}