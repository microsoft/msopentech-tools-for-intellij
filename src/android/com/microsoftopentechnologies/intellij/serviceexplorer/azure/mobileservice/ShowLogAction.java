package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.ViewLogForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;

@Name("Show log")
public class ShowLogAction extends NodeActionListener {
    private MobileServiceNode mobileServiceNode;

    public ShowLogAction(MobileServiceNode mobileServiceNode) {
        this.mobileServiceNode = mobileServiceNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ViewLogForm form = new ViewLogForm();

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                form.queryLog(mobileServiceNode.getMobileService().getSubcriptionId(), mobileServiceNode.getMobileService().getName(),
                        mobileServiceNode.getMobileService().getRuntime());
            }
        });

        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
