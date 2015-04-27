package com.microsoftopentechnologies.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.AzureServiceModule;

@Name("Manage Subscriptions")
public class ManageSubscriptionsAction extends NodeActionListener {
    private AzureServiceModule azureServiceModule;

    public ManageSubscriptionsAction(AzureServiceModule azureServiceModule) {
        this.azureServiceModule = azureServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        ManageSubscriptionForm form = new ManageSubscriptionForm((Project) azureServiceModule.getProject());
        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);
        azureServiceModule.load();
    }
}
