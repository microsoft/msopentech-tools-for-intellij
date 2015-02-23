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
import com.microsoftopentechnologies.intellij.model.ms.Subscription;
import com.microsoftopentechnologies.intellij.model.vm.*;
import com.microsoftopentechnologies.intellij.serviceexplorer.azure.vm.VMServiceModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.SimpleDateFormat;

public class CreateVMWizardModel extends WizardModel {

    private final String BASE_HTML_VM_IMAGE = "<html>\n" +
            "<body style=\"padding: 5px; width: 250px\">\n" +
            "    <p style=\"font-family: 'Segoe UI';font-size: 14pt;font-weight: bold;\">#TITLE#</p>\n" +
            "    <p style=\"font-family: 'Segoe UI';font-size: 11pt; width:200px \">#DESCRIPTION#</p>\n" +
            "    <p>\n" +
            "        <table style='width:200px'>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;width:60px;vertical-align:top;\"><b>PUBLISHED</b></td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;\">#PUBLISH_DATE#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;vertical-align:top;\"><b>PUBLISHER</b></td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;\">#PUBLISH_NAME#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;vertical-align:top;\"><b>OS FAMILY</b></td>\n" +
            "                <td style =\"font-family: 'Segoe UI';font-size: 12pt;\">#OS#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;v-align:top;font-weight:bold;\">LOCATION</td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 12pt;\">#LOCATION#</td>\n" +
            "            </tr>\n" +
            "        </table>\n" +
            "    </p>\n" +
            "    #PRIVACY#\n" +
            "    #LICENCE#\n" +
            "</body>\n" +
            "</html>";

    private Subscription subscription;
    private VirtualMachineImage virtualMachineImage;
    private String name;
    private VirtualMachineSize size;
    private String userName;
    private char[] password;
    private CloudService cloudService;
    private StorageAccount storageAccount;
    private String availabilitySet;
    private Endpoint[] endpoints;


    public CreateVMWizardModel(VMServiceModule node) {
        super(ApplicationNamesInfo.getInstance().getFullProductName() + " - Create new Virtual Machine");

        Project project = node.getProject();

        add(new SubscriptionStep(this));
        add(new SelectImageStep(this, project));
        add(new MachineSettingsStep(this, project));
        add(new CloudServiceStep(this, project));
        add(new EndpointStep(this, project, node));

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

    public String getHtmlFromVMImage(VirtualMachineImage virtualMachineImage) {
        String html = BASE_HTML_VM_IMAGE;
        html = html.replace("#TITLE#",virtualMachineImage.getLabel());
        html = html.replace("#DESCRIPTION#",virtualMachineImage.getDescription());
        html = html.replace("#PUBLISH_DATE#", new SimpleDateFormat("dd-M-yyyy").format(virtualMachineImage.getPublishedDate().getTime()));
        html = html.replace("#PUBLISH_NAME#",virtualMachineImage.getPublisherName());
        html = html.replace("#OS#",virtualMachineImage.getOperatingSystemType());
        html = html.replace("#LOCATION#",virtualMachineImage.getLocation());

        html = html.replace("#PRIVACY#",virtualMachineImage.getPrivacyUri().isEmpty()
                ? ""
                : "<p><a href='" + virtualMachineImage.getPrivacyUri() + "' style=\"font-family: 'Segoe UI';font-size: 12pt;\">Privacy statement</a></p>");


        html = html.replace("#LICENCE#",virtualMachineImage.getEulaUri().isEmpty()
                ? ""
                : "<p><a href='" + virtualMachineImage.getEulaUri() + "' style=\"font-family: 'Segoe UI';font-size: 12pt;\">Licence agreement</a></p>");

        return html;
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
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
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

    public String getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(String availabilitySet) {
        this.availabilitySet = availabilitySet;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public CloudService getCloudService() {
        return cloudService;
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public Endpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoint[] endpoints) {
        this.endpoints = endpoints;
    }
}
