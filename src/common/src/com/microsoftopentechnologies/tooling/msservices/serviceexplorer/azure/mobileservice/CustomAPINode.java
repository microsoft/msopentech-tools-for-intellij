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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice;

import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.CustomAPI;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;

public class CustomAPINode extends ScriptNodeBase {
    public static final String ICON_PATH = "api.png";
    protected CustomAPI customAPI;

    public CustomAPINode(Node parent, CustomAPI customAPI) {
        super(customAPI.getName(), customAPI.getName(), parent, ICON_PATH);
        this.customAPI = customAPI;
    }

    @Override
    protected void onNodeClick(NodeActionEvent event) {
        onNodeClickInternal(customAPI);
    }

    @Override
    protected void downloadScript(MobileService mobileService, String scriptName, String localFilePath)
            throws AzureCmdException {
        AzureManagerImpl.getManager().downloadAPIScript(
                mobileService.getSubcriptionId(),
                mobileService.getName(),
                scriptName,
                localFilePath);
    }

    public CustomAPI getCustomAPI() {
        return customAPI;
    }

    public void setCustomAPI(CustomAPI customAPI) {
        this.customAPI = customAPI;
    }
}