package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.microsoftopentechnologies.intellij.forms.JobForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.ScheduledJobNode;

@Name("Edit job")
public class EditJobAction extends NodeActionListener {
    private ScheduledJobNode scheduledJobNode;

    public EditJobAction(ScheduledJobNode scheduledJobNode) {
        this.scheduledJobNode = scheduledJobNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = (MobileServiceNode) scheduledJobNode.findParentByType(MobileServiceNode.class);
        final MobileService mobileService = mobileServiceNode.getMobileService();

        final JobForm form = new JobForm();
        form.setJob(scheduledJobNode.getJob());
        form.setServiceName(mobileService.getName());
        form.setTitle("Edit job");
        form.setSubscriptionId(mobileService.getSubcriptionId());
        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                scheduledJobNode.setJob(form.getEditingJob());
            }
        });
        form.pack();
        form.setVisible(true);
    }
}
