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
package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureAuthenticationMode;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Subscription;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Vector;

public class SubscriptionStep extends WizardStep<CreateVMWizardModel> {
    CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JButton buttonLogin;
    private JComboBox subscriptionComboBox;
    private JLabel userInfoLabel;

    public SubscriptionStep(final CreateVMWizardModel model) {
        super("Choose a Subscription", null, null);

        this.model = model;

        model.configStepList(createVmStepsList, 0);

        buttonLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ManageSubscriptionForm form = new ManageSubscriptionForm(null);
                DefaultLoader.getUIHelper().packAndCenterJDialog(form);
                form.setVisible(true);

                loadSubscriptions();
            }
        });

        subscriptionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getItem() instanceof Subscription) {
                    model.setSubscription((Subscription) itemEvent.getItem());
                }
            }
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        loadSubscriptions();

        return rootPanel;
    }

    private void loadSubscriptions() {
        try {
            AzureRestAPIManager manager = AzureRestAPIManagerImpl.getManager();

            if (manager.getAuthenticationMode().equals(AzureAuthenticationMode.ActiveDirectory)) {
                String upn = manager.getAuthenticationToken().getUserInfo().getUniqueName();
                userInfoLabel.setText("Signed in as: " + (upn.contains("#") ? upn.split("#")[1] : upn));
            } else {
                userInfoLabel.setText("");
            }

            ArrayList<Subscription> subscriptionList = manager.getSubscriptionList();

            final Vector<Subscription> subscriptions = new Vector<Subscription>((subscriptionList == null) ? new Vector<Subscription>() : subscriptionList);
            subscriptionComboBox.setModel(new DefaultComboBoxModel(subscriptions));

            if (!subscriptions.isEmpty()) {
                model.setSubscription(subscriptions.get(0));
            }

            model.getCurrentNavigationState().NEXT.setEnabled(!subscriptions.isEmpty());
        } catch (AzureCmdException e) {
            DefaultLoader.getUIHelper().showException("An error occurred while trying to load the subscriptions list",
                    e, "Error Loading Subscriptions", false, true);
        }
    }
}