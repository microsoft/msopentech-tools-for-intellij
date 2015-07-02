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
package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoftopentechnologies.intellij.helpers.LinkListener;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIHelper;
import com.microsoftopentechnologies.tooling.msservices.model.ms.SqlDb;
import com.microsoftopentechnologies.tooling.msservices.model.ms.SqlServer;
import com.microsoftopentechnologies.tooling.msservices.model.Subscription;
import com.microsoftopentechnologies.tooling.msservices.model.vm.Location;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class CreateMobileServiceForm extends JDialog {

    private JPanel mainPanel;
    private JTextField nameTextField;
    private JComboBox regionComboBox;
    private JComboBox serverComboBox;
    private JTextField serverUserNameTextField;
    private JPasswordField serverPasswordPasswordField;
    private JButton btnCloseButton;
    private JPasswordField serverPasswordConfirmationPasswordField;
    private JLabel lblPrivacy;
    private JButton btnCreate;
    private JLabel lblPricing;
    private JLabel lblPasswordConfirmation;
    private JComboBox subscriptionComboBox;
    private Runnable serviceCreated;


    public CreateMobileServiceForm() {

        final JDialog form = this;

        this.setContentPane(mainPanel);
        this.setResizable(false);
        this.setModal(true);
        this.setTitle("Create Mobile Service");


        lblPrivacy.addMouseListener(new LinkListener("http://msdn.microsoft.com/en-us/vstudio/dn425032.aspx"));
        lblPricing.addMouseListener(new LinkListener("http://www.azure.com/en-us/pricing/details/mobile-services/"));

        mainPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                form.setVisible(false);
                form.dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        btnCloseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                form.setVisible(false);
                form.dispose();
            }
        });


        subscriptionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {

                    subcriptionSelected((Subscription) subscriptionComboBox.getSelectedItem());
                }
            }
        });

        regionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                for (int i = 0; i < serverComboBox.getItemCount(); i++) {
                    if (serverComboBox.getItemAt(i) instanceof SqlDb) {
                        SqlDb sqlDb = (SqlDb) serverComboBox.getItemAt(i);
                        if (sqlDb.getServer().getRegion().equals(regionComboBox.getSelectedItem().toString())) {
                            serverComboBox.setSelectedIndex(i);
                            return;
                        }
                    }
                }

                serverComboBox.setSelectedIndex(serverComboBox.getItemCount() - 1);

            }
        });

        serverComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    updateVisibleFields(itemEvent.getItem());
                }
            }
        });

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Subscription> subsList = AzureManagerImpl.getManager().getSubscriptionList();
                    DefaultComboBoxModel subscriptionDefaultComboBoxModel = new DefaultComboBoxModel(subsList.toArray(new Subscription[subsList.size()]));
                    subscriptionComboBox.setModel(subscriptionDefaultComboBoxModel);

                    if (subsList.size() > 0) {
                        subcriptionSelected(subsList.get(0));
                    }

                } catch (Throwable e) {

                    form.setCursor(Cursor.getDefaultCursor());
                    DefaultLoader.getUIHelper().showException("Error retrieving the subscription list: ", e, "Error retrieving the subscription list");
                }
            }
        });

        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            String id = ((Subscription) subscriptionComboBox.getSelectedItem()).getId();
                            String name = nameTextField.getText();
                            String region = regionComboBox.getSelectedItem().toString();
                            String server = (serverComboBox.getSelectedItem() instanceof SqlDb ? ((SqlDb) serverComboBox.getSelectedItem()).getServer().getName() : null);
                            String admin = serverUserNameTextField.getText();
                            String pass = new String(serverPasswordPasswordField.getPassword());
                            String db = (serverComboBox.getSelectedItem() instanceof SqlDb ? ((SqlDb) serverComboBox.getSelectedItem()).getName() : null);
                            String conf = new String(serverPasswordConfirmationPasswordField.getPassword());

                            String error = "";

                            if (name.isEmpty()) {
                                error += "The service name must not be empty \n";
                            }

                            if (region.isEmpty()) {
                                error += "A region must be selected \n";
                            }

                            if (admin.isEmpty()) {
                                error += "User name must not be empty \n";
                            }

                            if (pass.isEmpty()) {
                                error += "Password must not be empty \n";
                            }

                            if (server != null && db == null) {
                                error += "Database must not be empty \n";
                            }

                            if (!error.isEmpty()) {
                                JOptionPane.showMessageDialog(form, error, "Error creating the service", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            if (!nameTextField.getText().matches("^[A-Za-z][A-Za-z0-9-]+[A-Za-z0-9]$")) {
                                JOptionPane.showMessageDialog(form, "Invalid service name. Service name must start with a letter, \n" +
                                        "contain only letters, numbers, and hyphens, " +
                                        "and end with a letter or number.", "Error creating the service", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            if (server == null) {
                                if (!pass.matches("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$") || pass.contains(admin)) {
                                    JOptionPane.showMessageDialog(form, "Invalid password. The password must: \n" +
                                            " - Not contain all login name\n" +
                                            " - Have at least one upper case english letter\n" +
                                            " - Have at least one lower case english letter\n" +
                                            " - Have at least one digit\n" +
                                            " - Have at least one special character\n" +
                                            " - Be minimum 8 in length", "Error creating the service", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }

                                if (!pass.equals(conf)) {
                                    JOptionPane.showMessageDialog(form, "Password confirmation should match password", "Error creating the service", JOptionPane.ERROR_MESSAGE);
                                    form.setCursor(Cursor.getDefaultCursor());
                                    return;
                                }
                            }


                            form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));


                            if (AzureRestAPIHelper.existsMobileService(name)) {
                                JOptionPane.showMessageDialog(form, "The service name is used by another mobile service", "Error creating the service", JOptionPane.ERROR_MESSAGE);
                                form.setCursor(Cursor.getDefaultCursor());
                                return;
                            }

                            AzureManagerImpl.getManager().createMobileService(id, region, admin, pass, name, server, db);

                            serviceCreated.run();

                            form.setCursor(Cursor.getDefaultCursor());

                            form.setVisible(false);
                            form.dispose();
                        } catch (Throwable e) {
                            form.setCursor(Cursor.getDefaultCursor());

                            DefaultLoader.getUIHelper().showException("An error occurred while trying to create the mobile service.",
                                    e,
                                    "Error Creating Mobile Service",
                                    false,
                                    true);
                        }
                    }
                });
            }
        });
    }

    private void updateVisibleFields(Object selectedServer) {
        boolean isExistingDb = selectedServer instanceof SqlDb;

        lblPasswordConfirmation.setVisible(!isExistingDb);
        serverPasswordConfirmationPasswordField.setVisible(!isExistingDb);

        if (isExistingDb) {
            SqlDb db = (SqlDb) selectedServer;
            serverUserNameTextField.setText(db.getServer().getAdmin());
        } else {
            serverUserNameTextField.setText("");
        }
    }

    private void subcriptionSelected(final Subscription subscription) {
        regionComboBox.setModel(new DefaultComboBoxModel(new String[]{"(loading...)"}));

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Location> locations = AzureManagerImpl.getManager().getLocations(subscription.getId());

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            regionComboBox.setModel(new DefaultComboBoxModel(locations.toArray()));
                        }
                    }, ModalityState.any());
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error retrieving the location list: ", e, "Error retrieving the location list");
                }
            }
        });

        serverComboBox.setModel(new DefaultComboBoxModel(new String[]{"(loading...)"}));

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<SqlDb> databaseList = new ArrayList<SqlDb>();
                    List<SqlServer> sqlServerList = AzureManagerImpl.getManager().getSqlServers(subscription.getId());

                    ArrayList<Future<List<SqlDb>>> futures = new ArrayList<Future<List<SqlDb>>>();

                    for (final SqlServer server : sqlServerList) {
                        futures.add(ApplicationManager.getApplication().executeOnPooledThread(new Callable<List<SqlDb>>() {
                            @Override
                            public List<SqlDb> call() throws Exception {
                                return AzureManagerImpl.getManager().getSqlDb(subscription.getId(), server);
                            }
                        }));
                    }

                    for (Future<List<SqlDb>> future : futures) {
                        for (SqlDb sqlDb : future.get()) {
                            if (!sqlDb.getEdition().equals("System"))
                                databaseList.add(sqlDb);
                        }
                    }

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel(databaseList.toArray());

                            defaultComboBoxModel.addElement("<< Create a Free SQL Database >>");
                            serverComboBox.setModel(defaultComboBoxModel);

                            updateVisibleFields(defaultComboBoxModel.getSelectedItem());
                        }
                    }, ModalityState.any());
                } catch (Exception e) {
                    DefaultLoader.getUIHelper().showException("Error retrieving the server and database list: ", e, "Error retrieving the server and database list");
                }
            }
        });
    }

    public void setServiceCreated(Runnable serviceCreated) {
        this.serviceCreated = serviceCreated;
    }
}