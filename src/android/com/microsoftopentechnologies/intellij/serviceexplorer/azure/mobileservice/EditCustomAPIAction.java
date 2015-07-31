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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.CustomAPINode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;

@Name("Edit Custom API")
public class EditCustomAPIAction extends NodeActionListener {
    private CustomAPINode customAPINode;

    public EditCustomAPIAction(CustomAPINode customAPINode) {
        this.customAPINode = customAPINode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = customAPINode.findParentByType(MobileServiceNode.class);
        MobileService mobileService = mobileServiceNode.getMobileService();

        final CustomAPIForm form = new CustomAPIForm();
        form.setEditingCustomAPI(customAPINode.getCustomAPI());
        form.setServiceName(mobileService.getName());

        form.setSubscriptionId(mobileService.getSubcriptionId());
        form.setProject((Project) customAPINode.getProject());
        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                customAPINode.setCustomAPI(form.getEditingCustomAPI());
            }
        });
        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}