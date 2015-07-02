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

import com.microsoftopentechnologies.intellij.forms.JobForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Job;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.ScheduledJobNode;

import java.lang.reflect.InvocationTargetException;

@Name("Create new job")
public class CreateNewJobAction extends NodeActionListener {
    private MobileServiceNode mobileServiceNode;

    public CreateNewJobAction(MobileServiceNode mobileServiceNode) {
        this.mobileServiceNode = mobileServiceNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        JobForm form = new JobForm();
        form.setServiceName(mobileServiceNode.getMobileService().getName());
        form.setSubscriptionId(mobileServiceNode.getMobileService().getSubcriptionId());
        form.setTitle("Create new Job");

        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                // refresh the jobs node
                mobileServiceNode.getJobsNode().removeAllChildNodes();
                try {
                    mobileServiceNode.loadServiceNode(
                            AzureManagerImpl.getManager().listJobs(
                                    mobileServiceNode.getMobileService().getSubcriptionId(),
                                    mobileServiceNode.getMobileService().getName()),
                            "_jobs",
                            MobileServiceNode.SCHEDULED_JOBS,
                            mobileServiceNode.getJobsNode(),
                            ScheduledJobNode.class,
                            Job.class);
                } catch (NoSuchMethodException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (IllegalAccessException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (InvocationTargetException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (InstantiationException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (AzureCmdException e1) {
                    mobileServiceNode.handleError(e1);
                }
            }
        });

        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
