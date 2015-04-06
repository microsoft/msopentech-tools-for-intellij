package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CreateBlobContainerForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

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
        form.setStorageAccount(blobModule.storageAccount);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                blobModule.parent.removeAllChildNodes();
                blobModule.parent.load();
            }
        });

        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);

    }
}
