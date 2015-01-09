/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardModel;
import com.microsoftopentechnologies.intellij.model.Subscription;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineSize;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CreateVMWizardModel extends WizardModel {

    private Subscription subscription;
    private VirtualMachineImage virtualMachineImage;
    private String name;
    private VirtualMachineSize size;
    private String userName;
    private char[] password;


    public CreateVMWizardModel(Project project) {
        super(ApplicationNamesInfo.getInstance().getFullProductName() + " - Create VM Wizard");

        add(new SubscriptionStep(this));
        add(new SelectImageStep(this, project));
        add(new MachineSettingsStep(this, project));
        add(new CloudServiceStep(this, project));

    }

    public String[] getStepTitleList() {
        return new String[] {
            "Subscription",
            "Select Image",
            "Machine Settings",
            "Cloud Service",
            "Endpoints"
        };
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public VirtualMachineImage getVirtualMachineImage() {
        return virtualMachineImage;
    }

    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
        this.virtualMachineImage = virtualMachineImage;
    }

    public void configStepList(JList jList, int step) {

        jList.setListData(getStepTitleList());
        jList.setSelectedIndex(step);
        jList.setBorder(new EmptyBorder(10, 0, 10, 0));

        jList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                return super.getListCellRendererComponent(jList, "  " + o.toString() , i, b, b1);
            }
        });

        for (MouseListener mouseListener : jList.getMouseListeners()) {
            jList.removeMouseListener(mouseListener);
        }

        for (MouseMotionListener mouseMotionListener : jList.getMouseMotionListeners()) {
            jList.removeMouseMotionListener(mouseMotionListener);
        }


    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VirtualMachineSize getSize() {
        return size;
    }

    public void setSize(VirtualMachineSize size) {
        this.size = size;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }
}
