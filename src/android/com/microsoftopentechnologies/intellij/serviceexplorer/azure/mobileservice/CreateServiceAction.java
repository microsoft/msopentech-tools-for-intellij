package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CreateMobileServiceForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Subscription;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceModule;

import java.util.ArrayList;

@Name("Create service")
public class CreateServiceAction extends NodeActionListener {
    private MobileServiceModule mobileServiceModule;

    public CreateServiceAction(MobileServiceModule mobileServiceModule) {
        this.mobileServiceModule = mobileServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // check if we have a valid subscription handy
        AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();
        if(apiManager.getAuthenticationMode() == AzureAuthenticationMode.Unknown) {
            DefaultLoader.getUIHelper().showException("Please configure an Azure subscription by right-clicking on the \"Azure\" " +
                    "node and selecting \"Manage subscriptions\".", null, "No Azure subscription found");
            return;
        }

        try {
            ArrayList<Subscription> subscriptions = apiManager.getSubscriptionList();
            if(subscriptions == null || subscriptions.isEmpty()) {
                DefaultLoader.getUIHelper().showException("No active Azure subscription was found. Please enable one more Azure " +
                                "subscriptions by right-clicking on the \"Azure\" " +
                                "node and selecting \"Manage subscriptions\".",
                        null, "No active Azure subscription found");
                return;
            }
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException("An error occurred while creating the mobile service.", e1);
        }

        CreateMobileServiceForm form = new CreateMobileServiceForm();
        form.setServiceCreated(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mobileServiceModule.refreshItems();
                        } catch (AzureCmdException e1) {
                            DefaultLoader.getUIHelper().showException("An error occurred while creating the mobile service.", e1);
                        }
                    }
                });
            }
        });

        form.setModal(true);
        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
