package com.microsoftopentechnologies.intellij.helpers.azure.sdk;

import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestContext;
import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestFilter;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureManager;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;

public class AuthTokenRequestFilter implements ServiceRequestFilter {
    private String subscriptionId;

    public AuthTokenRequestFilter(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void filter(ServiceRequestContext request) {
        AzureManager apiManager = AzureRestAPIManager.getManager();

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
