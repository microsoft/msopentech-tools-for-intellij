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

import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.helpers.ReadOnlyCellTableModel;
import com.microsoftopentechnologies.tooling.msservices.model.ms.LogEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.UUID;
import java.util.Vector;

public class ViewLogForm extends JDialog {
    private JTable logTable;
    private JPanel mainPanel;
    private Icon warningIcon = UIHelperImpl.loadIcon("logwarn.png");
    private Icon errorIcon = UIHelperImpl.loadIcon("logerr.png");
    private Icon infoIcon = UIHelperImpl.loadIcon("loginfo.png");

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
                        setIcon(UIHelperImpl.loadIcon("loginfo.png"));
                        value = "Information";
                    } else if(value.toString().equals("error")) {
                        setIcon(UIHelperImpl.loadIcon("logerr.png"));
                        value = "Error";
                    } else if(value.toString().equals("warning")) {
                        setIcon(UIHelperImpl.loadIcon("logwarn.png"));
                        value = "Warning";
                    }

                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    return this;
                }
            });
        } catch (Throwable ex) {
            this.setCursor(Cursor.getDefaultCursor());
            DefaultLoader.getUIHelper().showException("Error loading logs", ex);
        }
    }


    public void queryLog(UUID subscriptionId, String serviceName, String runtime) {

        try {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            ReadOnlyCellTableModel model = (ReadOnlyCellTableModel) logTable.getModel();

            while(model.getRowCount() > 0)
                model.removeRow(0);

            for(LogEntry log : AzureRestAPIManagerImpl.getManager().listLog(subscriptionId, serviceName, runtime)) {

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
            DefaultLoader.getUIHelper().showException("Error quering logs", ex);
        }
    }

}
