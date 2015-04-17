package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

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

        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        queryTable.setModel(model);
        queryTable.getColumn("And/Or").setCellRenderer(new ComboBoxTableRenderer(LogicalOperators.values()));
        queryTable.getColumn("And/Or").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Property Name").setCellRenderer(new ComboBoxTableRenderer(QuerableFields.values()));
        queryTable.getColumn("Property Name").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Operation").setCellRenderer(new ComboBoxTableRenderer(Operators.values()));
        queryTable.getColumn("Operation").setCellEditor(new ComboBoxTableCellEditor());

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

        addClause();
    }

    private void addClause() {
        DefaultTableModel model = (DefaultTableModel) queryTable.getModel();
        model.addRow(new Object[]{
                "",
                LogicalOperators.And,
                QuerableFields.PartitionKey,
                Operators.EqualsTo,
                ""
        });
    }

    private void onOK() {
        onFinish.run();
        dispose();
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


    private enum LogicalOperators {
        And,
        Or
    }

    private enum QuerableFields {
        PartitionKey,
        RowKey,
        Timestamp
    }

    private enum Operators {
        EqualsTo,
        GreaterThan,
        GreaterThanOrEqualsTo,
        LessThan,
        LessThanOrEqualsTo,
        NotEqualsTo;

        @Override
        public String toString() {
            //Todo
            return super.toString() + "k";
        }
    }
}
