package com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm;

import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.intellij.wizards.createvm.CreateVMWizard;

@Name("Create VM")
public class CreateVMAction extends NodeActionListener {
    public CreateVMAction(VMServiceModule node) {
        super(node);
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateVMWizard createVMWizard = new CreateVMWizard((VMServiceModule) e.getAction().getNode());
        createVMWizard.show();
    }
}
