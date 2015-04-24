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

import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.DatePickerCellEditor;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TablesQueryDesigner extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton addClauseButton;
    private JTextArea queryTextArea;
    private JTable queryTable;
    private Runnable onFinish;

    public TablesQueryDesigner() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setTitle("Query Builder");
        setResizable(false);
        setPreferredSize(new Dimension(600, 400));


        DefaultTableModel model = new DefaultTableModel(){

            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };

        model.setColumnIdentifiers(new String[]{
                "",
                "And/Or",
                "Property Name",
                "Operation",
                "Value"
        });

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {
                updateQueryText();
            }
        });

        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queryTable.setModel(model);

        JTableHeader tableHeader = queryTable.getTableHeader();
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(false);

        queryTable.getColumn("").setCellRenderer(new DeleteButtonRenderer());
        queryTable.getColumn("And/Or").setCellRenderer(new ComboBoxTableRenderer(LogicalOperator.values()));
        queryTable.getColumn("And/Or").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Property Name").setCellRenderer(new ComboBoxTableRenderer(QueryField.values()));
        queryTable.getColumn("Property Name").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Operation").setCellRenderer(new ComboBoxTableRenderer(Operator.values()));
        queryTable.getColumn("Operation").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Value").setCellEditor(new DatePickerCellEditor() {
            @Override
            protected boolean isCellDate(JTable jTable, int row, int col) {
                return jTable.getValueAt(row, 2) == QueryField.Timestamp;
            }
        });


        queryTable.getColumn("").setPreferredWidth(30);
        queryTable.getColumn("And/Or").setPreferredWidth(30);
        queryTable.getColumn("Property Name").setPreferredWidth(100);

        addClauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addClause();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        queryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int row = queryTable.rowAtPoint(mouseEvent.getPoint());
                int col = queryTable.columnAtPoint(mouseEvent.getPoint());
                if (col == 0) {
                    ((DefaultTableModel) queryTable.getModel()).removeRow(row);
                }
            }
        });

        addClause();
    }

    private void updateQueryText() {
        String query = "";

        DefaultTableModel model = (DefaultTableModel) queryTable.getModel();
        for(int i = 0; i != model.getRowCount(); i++) {
            LogicalOperator logicalOperator = (LogicalOperator) model.getValueAt(i, 1);
            QueryField queryField = (QueryField) model.getValueAt(i, 2);
            Operator operator = (Operator) model.getValueAt(i, 3);
            String value = model.getValueAt(i, 4).toString();

            if(queryField == QueryField.Timestamp) {
                try {
                    Date date = new SimpleDateFormat().parse(value);
                    value = "datetime'" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(date) + "'";
                } catch (ParseException ignored) {}
            } else {
                value = "'" + value + "'";
            }

            query = query + String.format("%s %s %s %s ",
                    (i == 0) ? "" : logicalOperator.toString().toLowerCase(),
                    queryField.toString(),
                    getOperatorWCF(operator),
                    value);
        }

        queryTextArea.setText(query);
    }

    private void addClause() {
        DefaultTableModel model = (DefaultTableModel) queryTable.getModel();
        model.addRow(new Object[]{
                "",
                LogicalOperator.And,
                QueryField.PartitionKey,
                Operator.EqualsTo,
                ""
        });
    }

    private void onOK() {
        onFinish.run();
        dispose();
    }

    private String getOperatorWCF(Operator op) {
        switch (op) {
            case EqualsTo:
                return "eq";
            case GreaterThan:
                return "gt";
            case GreaterThanOrEqualsTo:
                return "ge";
            case LessThan:
                return "lt";
            case LessThanOrEqualsTo:
                return "le";
            case NotEqualsTo:
                return "ne";
        }

        return null;
    }

    private void onCancel() {
        dispose();
    }

    public String getQueryText() {
        return queryTextArea.getText();
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }


    private enum LogicalOperator {
        And,
        Or
    }

    private enum QueryField {
        PartitionKey,
        RowKey,
        Timestamp
    }

    private enum Operator {
        EqualsTo,
        GreaterThan,
        GreaterThanOrEqualsTo,
        LessThan,
        LessThanOrEqualsTo,
        NotEqualsTo;

        @Override
        public String toString() {
            switch (this) {
                case EqualsTo:
                    return "Equals To";
                case GreaterThan:
                    return "Greater Than";
                case GreaterThanOrEqualsTo:
                    return "Greater Than Or Equals To";
                case LessThan:
                    return "Less Than";
                case LessThanOrEqualsTo:
                    return "Less Than Or Equals To";
                case NotEqualsTo:
                    return "Not Equals To";
            }

            return super.toString();
        }
    }
    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
        JButton deleteButton;

        public DeleteButtonRenderer() {
            deleteButton = new JButton();
            deleteButton.setIcon(DefaultLoader.getUIHelper().loadIcon("storagedelete.png"));
            deleteButton.setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int row, int i1) {
            return deleteButton;
        }
    }
}
