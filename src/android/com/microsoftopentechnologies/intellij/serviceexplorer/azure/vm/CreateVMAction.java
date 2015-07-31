/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm;

import com.microsoftopentechnologies.intellij.wizards.createvm.CreateVMWizard;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;

@Name("Create VM")
public class CreateVMAction extends NodeActionListener {
    private VMServiceModule vmServiceModule;

    public CreateVMAction(VMServiceModule vmServiceModule) {
        this.vmServiceModule = vmServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateVMWizard createVMWizard = new CreateVMWizard((VMServiceModule) e.getAction().getNode());
        createVMWizard.show();
    }
}