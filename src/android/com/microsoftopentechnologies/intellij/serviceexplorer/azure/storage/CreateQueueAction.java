/**
 * Copyright 2015 Microsoft Open Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CreateQueueForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

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
        form.setStorageAccount(queueModule.storageAccount);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                queueModule.getParent().removeAllChildNodes();
                queueModule.getParent().load();
            }
        });

        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);

    }
}
