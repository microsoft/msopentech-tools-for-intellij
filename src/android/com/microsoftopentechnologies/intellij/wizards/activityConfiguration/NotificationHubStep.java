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

package com.microsoftopentechnologies.intellij.wizards.activityConfiguration;

import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationHubStep extends WizardStep<AddServiceWizardModel> {
    private static final ServiceType serviceType = ServiceType.NotificationHub;

    private JPanel rootPanel;
    private JList listServices;
    private JTextField textSenderID;
    private JTextField textConnectionString;
    private JTextField textHubName;
    private AddServiceWizardModel model;

    public NotificationHubStep(final String title, final AddServiceWizardModel model) {
        super(title, null, null);
        this.model = model;
        List<String> listServicesData = new ArrayList<String>();

        int boldIndex = -1;
        int index = 0;
        for (ServiceType serviceType : this.model.getServiceTypes()) {
            listServicesData.add(serviceType.getDisplayName());

            if (serviceType.equals(NotificationHubStep.serviceType)) {
                boldIndex = index;
            }

            index++;
        }

        final String boldValue = listServicesData.get(boldIndex);
        DefaultListCellRenderer customListCellRenderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (boldValue.equals(value)) {// <= put your logic here
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        };
        this.listServices.setCellRenderer(customListCellRenderer);

        DefaultListSelectionModel customListSelectionModel = new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {

            }

            @Override
            public void addSelectionInterval(int index0, int index1) {

            }
        };
        this.listServices.setSelectionModel(customListSelectionModel);

        this.listServices.setListData(listServicesData.toArray(new String[1]));

        final NotificationHubStep step = this;

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                step.validateTextFields();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                step.validateTextFields();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                step.validateTextFields();
            }
        };

        textSenderID.getDocument().addDocumentListener(documentListener);
        textConnectionString.getDocument().addDocumentListener(documentListener);
        textHubName.getDocument().addDocumentListener(documentListener);
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        rootPanel.revalidate();
        validateTextFields();
        return rootPanel;
    }

    @Override
    public WizardStep onNext(AddServiceWizardModel model) {
        model.setSenderId(this.textSenderID.getText());
        model.setConnectionString(this.textConnectionString.getText());
        model.setHubName(this.textHubName.getText());
        return super.onNext(model);
    }

    @Override
    public WizardStep onPrevious(AddServiceWizardModel model) {
        model.setSenderId(this.textSenderID.getText());
        model.setConnectionString(this.textConnectionString.getText());
        model.setHubName(this.textHubName.getText());
        return super.onPrevious(model);
    }

    private void validateTextFields() {
        this.model.getCurrentNavigationState().NEXT.setEnabled(this.textSenderID.getText().length() > 0
                && this.textConnectionString.getText().length() > 0
                && this.textHubName.getText().length() > 0);
    }
}