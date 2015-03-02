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

import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.forms.ManageSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Subscription;

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

    public SubscriptionStep(final CreateVMWizardModel model) {
        super("Choose a Subscription", null, null);

        this.model = model;

        model.configStepList(createVmStepsList, 0);


        buttonLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ManageSubscriptionForm form = new ManageSubscriptionForm(null);
                UIHelper.packAndCenterJDialog(form);
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

        // model.getCurrentNavigationState().NEXT.setEnabled(false);

        return rootPanel;
    }

    private void loadSubscriptions() {
        try {
            ArrayList<Subscription> subscriptionList = AzureRestAPIManagerImpl.getManager().getSubscriptionList();

            final Vector<Subscription> subscriptions = new Vector<Subscription>((subscriptionList == null) ? new Vector<Subscription>() : subscriptionList);
            subscriptionComboBox.setModel(new DefaultComboBoxModel(subscriptions));

            if(!subscriptions.isEmpty()) {
                model.setSubscription(subscriptions.get(0));
            }


            model.getCurrentNavigationState().NEXT.setEnabled(!subscriptions.isEmpty());

        } catch (AzureCmdException e) {
            UIHelper.showException("Error retrieving subscription list", e);
        }
    }
}
