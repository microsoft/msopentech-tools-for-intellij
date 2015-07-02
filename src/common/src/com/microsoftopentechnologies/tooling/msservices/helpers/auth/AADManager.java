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
package com.microsoftopentechnologies.tooling.msservices.helpers.auth;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

public interface AADManager {
    @NotNull
    UserInfo authenticate(@NotNull String resource, @NotNull String title)
            throws AzureCmdException;

    void authenticate(@NotNull UserInfo userInfo,
                      @NotNull String resource,
                      @NotNull String title)
            throws AzureCmdException;

    @NotNull
    <T> T request(@NotNull UserInfo userInfo,
                  @NotNull String resource,
                  @NotNull String title,
                  @NotNull RequestCallback<T> requestCallback)
            throws AzureCmdException;

    @NotNull
    <V> ListenableFuture<V> requestFuture(@NotNull UserInfo userInfo,
                                          @NotNull String resource,
                                          @NotNull String title,
                                          @NotNull RequestCallback<ListenableFuture<V>> requestCallback);
}