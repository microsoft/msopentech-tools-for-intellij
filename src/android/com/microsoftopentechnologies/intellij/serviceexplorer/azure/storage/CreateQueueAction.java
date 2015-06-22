/**
 * Copyright 2015 Microsoft Open Technologies, Inc.
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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.CreateQueueForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.QueueModule;

@Name("Create new queue")
public class CreateQueueAction extends NodeActionListener {
    private QueueModule queueModule;

    public CreateQueueAction(QueueModule queueModule) {
        this.queueModule = queueModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateQueueForm form = new CreateQueueForm();

        form.setProject((Project) queueModule.getProject());
        form.setStorageAccount(queueModule.getStorageAccount());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                queueModule.getParent().removeAllChildNodes();
                ((ClientStorageNode) queueModule.getParent()).load();
            }
        });

        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}