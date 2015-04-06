package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.model.ms.MobileService;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

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
        form.setEditingCustomAPI(customAPINode.customAPI);
        form.setServiceName(mobileService.getName());

        form.setSubscriptionId(mobileService.getSubcriptionId());
        form.setProject((Project) customAPINode.getProject());
        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                customAPINode.customAPI = form.getEditingCustomAPI();
            }
        });
        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
