package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.CreateBlobContainerForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.BlobModule;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;

@Name("Create blob container")
public class CreateBlobContainer extends NodeActionListener {
    private BlobModule blobModule;

    public CreateBlobContainer(BlobModule blobModule) {
        this.blobModule = blobModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateBlobContainerForm form = new CreateBlobContainerForm();

        form.setProject((Project) blobModule.getProject());
        form.setStorageAccount(blobModule.getStorageAccount());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                blobModule.getParent().removeAllChildNodes();
                ((ClientStorageNode) blobModule.getParent()).load();
            }
        });

        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}