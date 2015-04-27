package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
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
        MobileServiceNode mobileServiceNode = (MobileServiceNode) customAPINode.findParentByType(MobileServiceNode.class);
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
        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
