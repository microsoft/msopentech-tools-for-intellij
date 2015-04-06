package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.CustomAPI;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.lang.reflect.InvocationTargetException;

@Name("Create API")
public class CreateAPIAction extends NodeActionListener {
    private MobileServiceNode mobileServiceNode;

    public CreateAPIAction(MobileServiceNode mobileServiceNode) {
        this.mobileServiceNode = mobileServiceNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CustomAPIForm form = new CustomAPIForm();
        form.setServiceName(mobileServiceNode.mobileService.getName());
        form.setSubscriptionId(mobileServiceNode.mobileService.getSubcriptionId());
        form.setProject((Project) mobileServiceNode.getProject());

        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                // refresh the apis node
                mobileServiceNode.customAPIsNode.removeAllChildNodes();
                try {
                    mobileServiceNode.loadServiceNode(
                            AzureRestAPIManagerImpl.getManager().getAPIList(
                                    mobileServiceNode.mobileService.getSubcriptionId(),
                                    mobileServiceNode.mobileService.getName()),
                            "_apis",
                            MobileServiceNode.CUSTOM_APIS,
                            mobileServiceNode.customAPIsNode,
                            CustomAPINode.class,
                            CustomAPI.class);
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
