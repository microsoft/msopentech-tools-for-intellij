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

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microsoft.directoryservices.Application;
import com.microsoftopentechnologies.intellij.forms.CreateOffice365ApplicationForm;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.CreateNewOffice365AppForm;
import com.microsoftopentechnologies.intellij.forms.PermissionsEditorForm;
import com.microsoftopentechnologies.intellij.helpers.ReadOnlyCellTableModel;
import com.microsoftopentechnologies.intellij.helpers.StringHelper;
import com.microsoftopentechnologies.intellij.helpers.graph.ServicePermissionEntry;
import com.microsoftopentechnologies.intellij.helpers.o365.Office365Manager;
import com.microsoftopentechnologies.intellij.helpers.o365.Office365RestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.intellij.model.Office365Permission;
import com.microsoftopentechnologies.intellij.model.Office365PermissionList;
import com.microsoftopentechnologies.intellij.model.Office365Service;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class Office365Step extends WizardStep<AddServiceWizardModel> {
    private class AppPermissionsTM extends AbstractTableModel {
        List<ServicePermissionEntry> servicePermissionEntries;

        public AppPermissionsTM(@NotNull List<ServicePermissionEntry> servicePermissionEntries) {
            this.servicePermissionEntries = servicePermissionEntries;
        }

        public List<ServicePermissionEntry> getPermissionEntries() {
            return servicePermissionEntries;
        }

        @Override
        public int getRowCount() {
            return servicePermissionEntries.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return servicePermissionEntries.get(rowIndex).getKey();
            } else if (columnIndex == 1) {
                return servicePermissionEntries.get(rowIndex).getValue();
            } else {
                throw new IndexOutOfBoundsException("columnIndex");
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if(columnIndex == 1) {
                servicePermissionEntries.get(rowIndex).setValue((Office365PermissionList)value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Office365Service.class;
            } else if (columnIndex == 1) {
                return Office365PermissionList.class;
            } else {
                throw new IndexOutOfBoundsException("columnIndex");
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Service";
            } else if (columnIndex == 1) {
                return "Permissions";
            } else {
                throw new IndexOutOfBoundsException("columnIndex");
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0) {
                return false;
            }
            else if (columnIndex == 1) {
                return true;
            }
            throw new IndexOutOfBoundsException("columnIndex");
        }
    }

    private class AppPermissionsCR extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private JPanel panel;
        private Office365Service service;
        private Office365PermissionList permissionSet;
        private JLabel permissionsLabel;
        private JTable tblAppPermissions;
        private int currentRow, currentCol;

        public AppPermissionsCR(JTable tblAppPermissions) {
            this.tblAppPermissions = tblAppPermissions;
            FormLayout formLayout = new FormLayout(
                    "fill:70px:grow, fill:30px",
                    "center:d:noGrow"
            );
            panel = new JPanel(formLayout);
            panel.setFocusable(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, (Office365PermissionList) value, row, column, isSelected);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row; currentCol = column;
            return getTableCellComponent(table, (Office365PermissionList) value, row, column, isSelected);
        }

        @Override
        public Object getCellEditorValue() {
            return permissionSet;
        }

        private Component getTableCellComponent(JTable table, Office365PermissionList permissionSet, int row, int column, boolean isSelected) {
            this.permissionSet = permissionSet;
            this.service = (Office365Service)table.getModel().getValueAt(row, 0);

            // build the label text
            Iterable<Office365Permission> enabledPermissions = Iterables.filter(this.permissionSet, new Predicate<Office365Permission>() {
                @Override
                public boolean apply(Office365Permission office365Permission) {
                    return office365Permission.isEnabled();
                }
            });

            String permissions = Joiner.on(", ").join(Iterables.transform(enabledPermissions, new Function<Office365Permission, String>() {
                @Override
                public String apply(Office365Permission office365Permission) {
                    return office365Permission.getName();
                }
            }));
            if(StringHelper.isNullOrWhiteSpace(permissions)) {
                permissions = "No permissions assigned";
            }

            // setting this to true causes the panel to not draw a background;
            // if we don't do this then the panel draws the default dialog
            // background color which looks ugly in a light colored theme
            panel.setOpaque(false);

            // create the label and the button
            if(permissionsLabel == null) {
                panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

                CellConstraints constraints = new CellConstraints();
                permissionsLabel = new JBLabel();
                panel.add(permissionsLabel, constraints.xy(1, 1));

                JButton button = new JButton("...");
                button.setOpaque(true);
                panel.add(button, constraints.xy(2, 1));

                button.addActionListener(new ShowPermissionsDialogActionListener());
            }

            permissionsLabel.setText(permissions);
            permissionsLabel.setToolTipText(permissions);
            return panel;
        }

        class ShowPermissionsDialogActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                // this is not exactly intuitive but when you click the button on the table cell
                // this is the method that gets called; so we pop up the permissions form here
                PermissionsEditorForm permissionsEditorForm = new PermissionsEditorForm(service.getName(), permissionSet);
                UIHelper.packAndCenterJDialog(permissionsEditorForm);
                permissionsEditorForm.setVisible(true);

                if(permissionsEditorForm.getDialogResult() == PermissionsEditorForm.DialogResult.OK) {
                    // update our permissions
                    permissionSet = permissionsEditorForm.getPermissions();
                    tblAppPermissions.getModel().setValueAt(permissionSet, currentRow, currentCol);
                }
                fireEditingStopped();
            }
        }
    }

    private final AddServiceWizardModel model;

    private JPanel rootPanel;
    private JComboBox cmbApps;
    private JTable tblAppPermissions;
    private JButton btnAddApp;
    private JButton btnSignOut;

    public Office365Step(final String title, final AddServiceWizardModel model) {
        super("Microsoft Services", title, null);
        this.model = model;

        this.tblAppPermissions.setFocusable(false);
        this.tblAppPermissions.setRowHeight(35);
        this.tblAppPermissions.setIntercellSpacing(new Dimension(5, 2));
        this.tblAppPermissions.setDefaultRenderer(Office365PermissionList.class, new AppPermissionsCR(tblAppPermissions));
        this.tblAppPermissions.setDefaultEditor(Office365PermissionList.class, new AppPermissionsCR(tblAppPermissions));

        this.cmbApps.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Application app = (Application) cmbApps.getSelectedItem();
                refreshPermissions(app);
            }
        });

        btnAddApp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CreateOffice365ApplicationForm form = new CreateOffice365ApplicationForm();
                form.setModal(true);
                DefaultLoader.getUIHelper().packAndCenterJDialog(form);
                form.setVisible(true);

                if(form.getDialogResult() == CreateOffice365ApplicationForm.DialogResult.OK) {
                    refreshApps(form.getApplication().getappId());
                }
            }
        });

        btnSignOut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // clear the authentication token
                Office365RestAPIManager.getManager().setAuthenticationToken(null);

                // refresh apps to cause the sign in popup to be displayed
                refreshApps(null);
            }
        });

        refreshApps(null);
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        rootPanel.revalidate();
        return rootPanel;
    }

    @Override
    public WizardStep onNext(AddServiceWizardModel model) {
        model.setOfficeApp((Application)cmbApps.getSelectedItem());
        AppPermissionsTM permissionsModel = (AppPermissionsTM) tblAppPermissions.getModel();
        model.setOfficePermissions(permissionsModel.getPermissionEntries());

        return super.onNext(model);
    }

    private void refreshApps(final String selectedAppId) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                fillApps(selectedAppId);
            }
        });
    }

    private void refreshPermissions(@NotNull final Application app) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    fillPermissions(app);
                } catch (ParseException e) {
                    UIHelper.showException("An error occurred while fetching permissions for Office 365 services.", e);
                }
            }
        });
    }

    private class StringComboBoxItemRenderer extends ListCellRendererWrapper<String> {
        @Override
        public void customize(JList jList, String s, int i, boolean b, boolean b2) {
            setText(s);
        }
    }

    private void fillApps(final String selectedAppId) {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                cmbApps.setRenderer(new StringComboBoxItemRenderer());
                cmbApps.setModel(new DefaultComboBoxModel(new String[]{"(loading...)"}));
                cmbApps.setEnabled(false);

                ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
                messageTableModel.addColumn("Message");
                Vector<String> vector = new Vector<String>();
                vector.add("(loading... )");
                messageTableModel.addRow(vector);
                tblAppPermissions.setModel(messageTableModel);
            }
        }, ModalityState.any());

        final Office365Manager manager = Office365RestAPIManager.getManager();

        try {
            if (!manager.authenticated()) {
                manager.authenticate();

                // if we still don't have an authentication token then the
                // user has cancelled out of login; so we cancel out of this
                // wizard
                if(manager.getAuthenticationToken() == null) {
                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            model.cancel();
                        }
                    }, ModalityState.any());
                    return;
                }
            }

            Futures.addCallback(manager.getApplicationList(), new FutureCallback<List<Application>>() {
                @Override
                public void onSuccess(final List<Application> applications) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (applications.size() > 0) {
                                cmbApps.setRenderer(new ListCellRendererWrapper<Application>() {
                                    @Override
                                    public void customize(JList jList, Application application, int i, boolean b, boolean b2) {
                                        setText(application.getdisplayName());
                                    }
                                });

                                cmbApps.setModel(new DefaultComboBoxModel(applications.toArray()));
                                cmbApps.setEnabled(true);

                                int selectedIndex = 0;
                                if(!StringHelper.isNullOrWhiteSpace(selectedAppId)) {
                                    selectedIndex = Iterables.indexOf(applications, new Predicate<Application>() {
                                        @Override
                                        public boolean apply(Application application) {
                                            return application.getappId().equals(selectedAppId);
                                        }
                                    });
                                }
                                cmbApps.setSelectedIndex(Math.max(0, selectedIndex));
                            } else {
                                cmbApps.setRenderer(new StringComboBoxItemRenderer());
                                cmbApps.setModel(new DefaultComboBoxModel(new String[]{"No apps configured)"}));
                                cmbApps.setEnabled(false);

                                ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
                                messageTableModel.addColumn("Message");
                                Vector<String> vector = new Vector<String>();
                                vector.add("There are no applications configured.");
                                messageTableModel.addRow(vector);
                                tblAppPermissions.setModel(messageTableModel);
                                //tblAppPermissions.setEnabled(false);
                            }
                        }
                    }, ModalityState.any());
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            UIHelper.showException("An error occurred while fetching the list of applications.", throwable);
                        }
                    }, ModalityState.any());
                }
            });
        }
        catch (Throwable throwable) {
            UIHelper.showException("An error occurred while trying to authenticate with Office 365", throwable);
            return;
        }
    }

    private void fillPermissions(@NotNull Application app) throws ParseException {
        // show a status message while we're fetching permissions
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
                messageTableModel.addColumn("Message");
                Vector<String> vector = new Vector<String>();
                vector.add("(loading... )");
                messageTableModel.addRow(vector);
                tblAppPermissions.setModel(messageTableModel);

                //tblAppPermissions.setEnabled(false);
            }
        }, ModalityState.any());

        Futures.addCallback(Office365RestAPIManager.getManager().getO365PermissionsForApp(app.getobjectId()), new FutureCallback<List<ServicePermissionEntry>>() {
            @Override
            public void onSuccess(final List<ServicePermissionEntry> servicePermissionEntries) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (servicePermissionEntries.size() > 0) {
                            tblAppPermissions.setModel(new AppPermissionsTM(servicePermissionEntries));
                            tblAppPermissions.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                            final TableColumn servicesColumn = tblAppPermissions.getColumnModel().getColumn(0);
                            servicesColumn.setMinWidth(100);
                            servicesColumn.setMaxWidth(250);
                            servicesColumn.setPreferredWidth(185);
                        } else {
                            ReadOnlyCellTableModel messageTableModel = new ReadOnlyCellTableModel();
                            messageTableModel.addColumn("Message");
                            Vector<String> vector = new Vector<String>();
                            vector.add("There are no Office 365 application permissions.");
                            messageTableModel.addRow(vector);
                            tblAppPermissions.setModel(messageTableModel);
                        }
                    }
                }, ModalityState.any());
            }

            @Override
            public void onFailure(Throwable throwable) {
                UIHelper.showException("An error occurred while fetching permissions for Office 365 services.", throwable);
            }
        });
    }
}