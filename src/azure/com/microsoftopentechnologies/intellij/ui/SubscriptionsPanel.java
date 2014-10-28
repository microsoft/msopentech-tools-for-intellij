/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.ui;

import javax.swing.*;

import com.microsoftopentechnologies.intellij.util.MethodUtils;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoftopentechnologies.model.Subscription;
import com.microsoftopentechnologies.deploy.propertypages.SubscriptionPropertyPageTableElement;
import com.microsoftopentechnologies.deploy.util.PublishData;
import com.microsoftopentechnologies.intellij.AzureSettings;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class SubscriptionsPanel implements AzureAbstractPanel {
    private JPanel contentPane;
    private JTable subscriptionsTable;
    private JButton importButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private Project myProject;

    public SubscriptionsPanel(Project project) {
        this.myProject = project;
        init();
    }

    protected void init() {
        subscriptionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subscriptionsTable.setModel(new SubscriptionsTableModel(getTableContent()));
        for (int i = 0; i < subscriptionsTable.getColumnModel().getColumnCount(); i++) {
            TableColumn each = subscriptionsTable.getColumnModel().getColumn(i);
            each.setPreferredWidth(SubscriptionsTableModel.getColumnWidth(i, 450));
        }
        importButton.addActionListener(createImportSubscriptionAction());
        removeButton.addActionListener(createRemoveButtonListener());
        removeButton.setEnabled(false);
        subscriptionsTable.getSelectionModel().addListSelectionListener(createSubscriptionsTableListener());
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return message("cmhLblSubscrpt");
    }

    public boolean doOKAction() {
        return true;
    }

    public String getSelectedValue() {
        return null;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    private ActionListener createRemoveButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int curSelIndex = subscriptionsTable.getSelectedRow();
                if (curSelIndex > -1) {
                    String id = (String) subscriptionsTable.getModel().getValueAt(curSelIndex, 1);
                    WizardCacheManager.removeSubscription(id);
                    AzureSettings.getSafeInstance(myProject).savePublishDatas();
                    ((SubscriptionsTableModel) subscriptionsTable.getModel()).setSubscriptions(getTableContent());
                    ((SubscriptionsTableModel) subscriptionsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    private ListSelectionListener createSubscriptionsTableListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(subscriptionsTable.getSelectedRow() > -1);
            }
        };
    }

    private ActionListener createImportSubscriptionAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ImportSubscriptionDialog importSubscriptionDialog = new ImportSubscriptionDialog();
                importSubscriptionDialog.show();
                if (importSubscriptionDialog.isOK()) {
                    String fileName = importSubscriptionDialog.getPublishSettingsPath();
                    MethodUtils.handleFile(fileName, myProject);
                    ((SubscriptionsTableModel) subscriptionsTable.getModel()).setSubscriptions(getTableContent());
                    ((SubscriptionsTableModel) subscriptionsTable.getModel()).fireTableDataChanged();
                }
            }
        };
    }

    /**
     * Method prepares storage account list to show in table.
     */
    private List<SubscriptionPropertyPageTableElement> getTableContent() {
        Collection<PublishData> publishDatas = WizardCacheManager.getPublishDatas();
        List<SubscriptionPropertyPageTableElement> tableRowElements = new ArrayList<SubscriptionPropertyPageTableElement>();
        for (PublishData pd : publishDatas) {
            for (Subscription sub : pd.getPublishProfile().getSubscriptions()) {
                SubscriptionPropertyPageTableElement el = new SubscriptionPropertyPageTableElement();
                el.setSubscriptionId(sub.getId());
                el.setSubscriptionName(sub.getName());
                if (!tableRowElements.contains(el)) {
                    tableRowElements.add(el);
                }
            }
        }
        return tableRowElements;
    }

//    public String getSelectedValue() {
//        int selectedIndex = subscriptionsTable.getSelectedRow();
//        if (selectedIndex >= 0) {
//            return ((SubscriptionsTableModel) subscriptionsTable.getModel()).getAccountNameAtIndex(subscriptionsTable.getSelectedRow());
//        }
//        return null;
//    }

    private static class SubscriptionsTableModel extends AbstractTableModel {
        public static final String[] COLUMNS = new String[]{message("subscriptionColName"), message("subscriptionIdColName")};
        private java.util.List<SubscriptionPropertyPageTableElement> subscriptions;

        public SubscriptionsTableModel(List<SubscriptionPropertyPageTableElement> subscriptions) {
            this.subscriptions = subscriptions;
        }

        public void setSubscriptions(List<SubscriptionPropertyPageTableElement> subscriptions) {
            this.subscriptions = subscriptions;
        }

//        public String getAccountNameAtIndex(int index) {
//            return subscriptions.get(index).getStorageName();
//        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public static int getColumnWidth(int column, int totalWidth) {
            switch (column) {
                case 0:
                    return (int) (totalWidth * 0.4);
                default:
                    return (int) (totalWidth * 0.6);
            }
        }

        public int getRowCount() {
            return subscriptions.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            SubscriptionPropertyPageTableElement subscription = subscriptions.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return subscription.getSubscriptionName();
                case 1:
                    return subscription.getSubscriptionId();
            }
            return null;
        }
    }
}

