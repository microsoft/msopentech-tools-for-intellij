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

import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.intellij.forms.ViewLogForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;

@Name("Show log")
public class ShowLogAction extends NodeActionListener {
    private MobileServiceNode mobileServiceNode;

    public ShowLogAction(MobileServiceNode mobileServiceNode) {
        this.mobileServiceNode = mobileServiceNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ViewLogForm form = new ViewLogForm();

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                form.queryLog(mobileServiceNode.getMobileService().getSubcriptionId(),
                        mobileServiceNode.getMobileService().getName(),
                        mobileServiceNode.getMobileService().getRuntime());
            }
        });

        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}