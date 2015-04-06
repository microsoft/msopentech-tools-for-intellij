package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CreateStorageAccountForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Subscription;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.util.ArrayList;

@Name("Create Storage Account")
public class CreateStorageAccountAction extends NodeActionListener {

    private StorageModule storageModule;

    public CreateStorageAccountAction(StorageModule storageModule) {
        this.storageModule = storageModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // check if we have a valid subscription handy
        AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();

        if (apiManager.getAuthenticationMode() == AzureAuthenticationMode.Unknown) {
            DefaultLoader.getUIHelper().showException("Please configure an Azure subscription by right-clicking on the \"Azure\" " +
                    "node and selecting \"Manage subscriptions\".", null, "No Azure subscription found");
            return;
        }

        try {
            ArrayList<Subscription> subscriptions = apiManager.getSubscriptionList();

            if (subscriptions == null || subscriptions.isEmpty()) {
                DefaultLoader.getUIHelper().showException("No active Azure subscription was found. Please enable one more Azure " +
                                "subscriptions by right-clicking on the \"Azure\" " +
                                "node and selecting \"Manage subscriptions\".",
                        null, "No active Azure subscription found");
                return;
            }
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException("An error occurred while creating the storage account.", e1);
        }

        CreateStorageAccountForm createStorageAccountForm = new CreateStorageAccountForm();
        createStorageAccountForm.fillFields(null, (Project) storageModule.getProject());
        DefaultLoader.getUIHelper().packAndCenterJDialog(createStorageAccountForm);

            createStorageAccountForm.setOnCreate(new Runnable() {
                @Override
                public void run() {
                    try {
                        storageModule.refreshItems();
                    } catch (AzureCmdException ex) {
                        DefaultLoader.getUIHelper().showException("Error refreshing storage accounts", ex, "Service Explorer", false, true);
                    }
                }
            });
        createStorageAccountForm.setVisible(true);
    }
}
