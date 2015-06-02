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

import javax.net.ssl.HttpsURLConnection;

public interface RestServiceManager {
    interface HttpsURLConnectionProvider {
        @NotNull
        HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                            @NotNull String path,
                                            @NotNull ContentType contentType)
                throws AzureCmdException;
    }

    enum ContentType {
        Json("application/json"),
        Xml("application/xml"),
        Text("text/plain");

        @NotNull
        private final String value;

        ContentType(@NotNull String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @NotNull
    String executeRequest(@NotNull String managementUrl,
                          @NotNull String path,
                          @NotNull ContentType contentType,
                          @NotNull String method,
                          @Nullable String postData,
                          @NotNull HttpsURLConnectionProvider sslConnectionProvider)
            throws AzureCmdException;

    @NotNull
    String executePollRequest(@NotNull String managementUrl,
                              @NotNull String path,
                              @NotNull ContentType contentType,
                              @NotNull String method,
                              @Nullable String postData,
                              @NotNull String pollPath,
                              @NotNull HttpsURLConnectionProvider sslConnectionProvider)
            throws AzureCmdException;

    @NotNull
    HttpsURLConnection getSSLConnection(@NotNull String managementUrl,
                                        @NotNull String path,
                                        @NotNull ContentType contentType)
            throws AzureCmdException;
}