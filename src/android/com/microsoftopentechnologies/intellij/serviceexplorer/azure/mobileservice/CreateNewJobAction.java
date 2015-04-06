package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.JobForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Job;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

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
        form.setServiceName(mobileServiceNode.mobileService.getName());
        form.setSubscriptionId(mobileServiceNode.mobileService.getSubcriptionId());
        form.setTitle("Create new Job");

        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                // refresh the jobs node
                mobileServiceNode.jobsNode.removeAllChildNodes();
                try {
                    mobileServiceNode.loadServiceNode(
                            AzureRestAPIManagerImpl.getManager().listJobs(
                                    mobileServiceNode.mobileService.getSubcriptionId(),
                                    mobileServiceNode.mobileService.getName()),
                            "_jobs",
                            MobileServiceNode.SCHEDULED_JOBS,
                            mobileServiceNode.jobsNode,
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

        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
