package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.storage.TableFileEditor;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.TableEntity;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;

public class TableEntityForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton addPropertyButton;
    private JTable propertiesTable;

    private Project project;
    private TableEntity tableEntity;
    private StorageAccount storageAccount;
    private Runnable onAddedEntity;

    public TableEntityForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        DefaultTableModel model = new DefaultTableModel(){

           @Override
            public boolean isCellEditable(int row, int col) {
                return (col != 0) && (row > 1 || col == 3);
            }
        };

        model.setColumnIdentifiers(new String[]{
                "",
                "Name",
                "Type",
                "Value",
        });

        propertiesTable.setModel(model);

        propertiesTable.getColumn("").setCellRenderer(new DeleteButtonRenderer());
        propertiesTable.getColumn("").setMaxWidth(30);
        propertiesTable.getColumn("").setMinWidth(30);
        propertiesTable.getColumn("Type").setCellRenderer(new ComboBoxTableRenderer<Types>(Types.values()));
        propertiesTable.getColumn("Type").setCellEditor(new ComboBoxTableCellEditor());

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
    }

    public void setTableEntity(TableEntity tableEntity) {
        this.tableEntity = tableEntity;

        final DefaultTableModel model = (DefaultTableModel) propertiesTable.getModel();
        model.addRow(new Object[]{
                "",
                TableFileEditor.PARTITION_KEY,
                Types.String,
                tableEntity.getPartitionKey()
        });

        model.addRow(new Object[]{
                "",
                TableFileEditor.ROW_KEY,
                Types.String,
                tableEntity.getRowKey()
        });

        for (String propertyName : tableEntity.getProperties().keySet()) {
            model.addRow(new Object[] {
                    "",
                    propertyName,
                    Types.String,
                    tableEntity.getProperties().get(propertyName)
            });
        }

    }


    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setOnAddedEntity(Runnable onAddedEntity) {
        this.onAddedEntity = onAddedEntity;
    }


    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
        JButton deleteButton;

        public DeleteButtonRenderer() {
            deleteButton = new JButton();
            deleteButton.setIcon(UIHelper.loadIcon("storagedelete.png"));
            deleteButton.setBorderPainted(false);
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                }
            });
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int row, int i1) {
            return (row < 2) ? super.getTableCellRendererComponent(jTable, o, b, b1, row, i1) : deleteButton;
        }
    }

    public enum Types {
        String,
        Int_32
    }
}
