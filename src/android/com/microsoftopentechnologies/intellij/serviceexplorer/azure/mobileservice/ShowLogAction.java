package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.ViewLogForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

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
                form.queryLog(mobileServiceNode.mobileService.getSubcriptionId(), mobileServiceNode.mobileService.getName(), mobileServiceNode.mobileService.getRuntime());
            }
        });

        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
