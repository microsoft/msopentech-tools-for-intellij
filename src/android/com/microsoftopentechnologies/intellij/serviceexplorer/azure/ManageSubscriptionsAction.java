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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureServiceModule;

@Name("Manage Subscriptions")
public class ManageSubscriptionsAction extends NodeActionListener {
    private AzureServiceModule azureServiceModule;

    public ManageSubscriptionsAction(AzureServiceModule azureServiceModule) {
        this.azureServiceModule = azureServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        ManageSubscriptionForm form = new ManageSubscriptionForm((Project) azureServiceModule.getProject());
        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
        //azureServiceModule.load();
    }
}