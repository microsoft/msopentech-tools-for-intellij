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

package com.microsoftopentechnologies.intellij.forms;

import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.ReadOnlyCellTableModel;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.model.LogEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.UUID;
import java.util.Vector;

public class ViewLogForm extends JDialog {
    private JTable logTable;
    private JPanel mainPanel;
    private Icon warningIcon = UIHelper.loadIcon("logwarn.png");
    private Icon errorIcon = UIHelper.loadIcon("logerr.png");
    private Icon infoIcon = UIHelper.loadIcon("loginfo.png");

    public ViewLogForm() {
        this.setContentPane(mainPanel);
        this.setTitle("Service log");

        try {
            logTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            ReadOnlyCellTableModel model = new ReadOnlyCellTableModel();

            model.addColumn("Level");
            model.addColumn("Message");
            model.addColumn("Source");
            model.addColumn("Time Stamp");

            Vector<Object> loadingRow = new Vector<Object>();
            loadingRow.add("loading...");

            model.addRow(loadingRow);
            logTable.setModel(model);

            logTable.getColumn("Level").setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                    if(value.toString().equals("information")) {
                        setIcon(UIHelper.loadIcon("loginfo.png"));
                        value = "Information";
                    } else if(value.toString().equals("error")) {
                        setIcon(UIHelper.loadIcon("logerr.png"));
                        value = "Error";
                    } else if(value.toString().equals("warning")) {
                        setIcon(UIHelper.loadIcon("logwarn.png"));
                        value = "Warning";
                    }

                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    return this;
                }
            });
        } catch (Throwable ex) {
            this.setCursor(Cursor.getDefaultCursor());
            UIHelper.showException("Error loading logs", ex);
        }
    }


    public void queryLog(UUID subscriptionId, String serviceName) {

        try {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            ReadOnlyCellTableModel model = (ReadOnlyCellTableModel) logTable.getModel();

            while(model.getRowCount() > 0)
                model.removeRow(0);

            for(LogEntry log : AzureRestAPIManager.getManager().listLog(subscriptionId, serviceName)) {

                Vector<Object> row = new Vector<Object>();
                row.add(log.getType());
                row.add(log.getMessage());
                row.add(log.getSource());
                row.add(log.getTimeCreated());

                model.addRow(row);
            }

            this.setCursor(Cursor.getDefaultCursor());
        } catch (Throwable ex) {
            this.setCursor(Cursor.getDefaultCursor());
            UIHelper.showException("Error quering logs", ex);
        }
    }

}
