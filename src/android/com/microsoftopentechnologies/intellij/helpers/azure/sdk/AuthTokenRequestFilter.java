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

import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestContext;
import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestFilter;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;

public class AuthTokenRequestFilter implements ServiceRequestFilter {
    private String subscriptionId;

    public AuthTokenRequestFilter(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void filter(ServiceRequestContext request) {
        AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();

        // if there's no auth token to work with there's nothing much we can do
        if (apiManager.getAuthenticationToken() == null) {
            throw new UnsupportedOperationException("The authentication mode has been set to use AD " +
                    "but no valid access token found. Please sign in to your account.");
        }

        // if we don't have an authentication token for the subscription then acquire one
        if (apiManager.getAuthenticationTokenForSubscription(subscriptionId) == null) {
            // perform interactive authentication
            try {
                AzureRestAPIHelper.acquireTokenInteractive(subscriptionId, apiManager);
            } catch (Exception e) {
                throw new UnsupportedOperationException("An error occurred while acquiring an access token.", e);
            }
        }

        AuthenticationResult token = apiManager.getAuthenticationTokenForSubscription(subscriptionId);
        // set access token
        request.setHeader(AzureRestAPIHelper.AUTHORIZATION_HEADER, "Bearer " + token.getAccessToken());
    }
}
