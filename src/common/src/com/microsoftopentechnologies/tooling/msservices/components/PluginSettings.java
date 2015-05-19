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

package com.microsoftopentechnologies.tooling.msservices.components;

public class PluginSettings {
    private String clientId;
    private String tenantName;
    private String redirectUri;
    private String azureServiceManagementUri;
    private String graphApiUri;
    private String adAuthority;
    private String graphApiVersion;
    private String pluginVersion;

    public String getClientId() {
        return clientId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAzureServiceManagementUri() {
        return azureServiceManagementUri;
    }

    public String getGraphApiUri() {
        return graphApiUri;
    }

    public String getAdAuthority() {
        return adAuthority;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }
}
