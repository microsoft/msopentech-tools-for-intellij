package com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm;

import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.intellij.wizards.createvm.CreateVMWizard;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;

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
