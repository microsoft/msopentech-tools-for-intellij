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

package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Job;
import com.microsoftopentechnologies.intellij.model.ms.MobileService;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.Map;

public class ScheduledJobNode extends ScriptNodeBase {
    public static final String ICON_PATH = "job.png";
    protected Job job;

    public ScheduledJobNode(Node parent, Job job) {
        super(job.getName(), job.getName(), parent, ICON_PATH, false);
        this.job = job;
    }

    @Override
    protected void onNodeClick(NodeActionEvent event) {
        onNodeClickInternal(job);
    }

    @Override
    protected void downloadScript(MobileService mobileService, String scriptName, String localFilePath) throws AzureCmdException {
        AzureRestAPIManagerImpl.getManager().downloadJobScript(
                mobileService.getSubcriptionId(),
                mobileService.getName(),
                scriptName,
                localFilePath);
    }

}
