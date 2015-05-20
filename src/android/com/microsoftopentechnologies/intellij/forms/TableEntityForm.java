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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.microsoftopentechnologies.intellij.helpers.DatePickerCellEditor;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.intellij.helpers.storage.TableFileEditor;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.TableEntity;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TableEntityForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton addPropertyButton;
    private JTable propertiesTable;

    private Project project;
    private TableEntity tableEntity;
    private ClientStorageAccount storageAccount;
    private Runnable onFinish;
    private String tableName;

    private static String[] INVALID_KEYWORDS = {
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else",
            "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit",
            "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator", "out", "override", "params", "private", "protected", "public",
            "readonly", "ref", "return", "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw", "true",
            "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "virtual", "void", "volatile", "while"
    };

    public TableEntityForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setResizable(false);
        setPreferredSize(new Dimension(500, 400));

        DefaultTableModel model = new DefaultTableModel() {

            @Override
            public boolean isCellEditable(int row, int col) {
                return (col != 0) && (row > 1 || (col == 3 && tableEntity == null));
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
        propertiesTable.getColumn("Type").setMaxWidth(100);
        propertiesTable.getColumn("Type").setMinWidth(100);
        propertiesTable.getColumn("Type").setCellRenderer(new ComboBoxTableRenderer<TableEntity.PropertyType>(TableEntity.PropertyType.values()));
        propertiesTable.getColumn("Type").setCellEditor(new ComboBoxTableCellEditor());

        propertiesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        propertiesTable.getColumn("Value").setCellEditor(new DatePickerCellEditor() {
            @Override
            protected boolean isCellDate(JTable table, int row, int col) {
                return (table.getValueAt(row, 2) == TableEntity.PropertyType.DateTime);
            }
        });

        propertiesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int row = propertiesTable.rowAtPoint(mouseEvent.getPoint());
                int col = propertiesTable.columnAtPoint(mouseEvent.getPoint());
                if (col == 0 && row > 1) {
                    ((DefaultTableModel) propertiesTable.getModel()).removeRow(row);
                }
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

        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final DefaultTableModel model = (DefaultTableModel) propertiesTable.getModel();
                model.addRow(new Object[]{
                        "",
                        "",
                        TableEntity.PropertyType.String,
                        ""
                });
            }
        });
    }

    public void setTableEntity(TableEntity tableEntity) {
        this.tableEntity = tableEntity;

        final DefaultTableModel model = (DefaultTableModel) propertiesTable.getModel();
        model.addRow(new Object[]{
                "",
                TableFileEditor.PARTITION_KEY,
                TableEntity.PropertyType.String,
                tableEntity == null ? "" : tableEntity.getPartitionKey()
        });

        model.addRow(new Object[]{
                "",
                TableFileEditor.ROW_KEY,
                TableEntity.PropertyType.String,
                tableEntity == null ? "" : tableEntity.getRowKey()
        });

        if (tableEntity != null) {
            for (String propertyName : tableEntity.getProperties().keySet()) {
                model.addRow(new Object[]{
                        "",
                        propertyName,
                        tableEntity.getProperties().get(propertyName).getType(),
                        TableFileEditor.getFormattedProperty(tableEntity.getProperties().get(propertyName))
                });
            }
        }
    }

    private void onOK() {
        final TableModel model = propertiesTable.getModel();
        final String partitionKey = model.getValueAt(0, 3).toString();
        final String rowKey = model.getValueAt(1, 3).toString();
        final Map<String, TableEntity.Property> properties = new LinkedHashMap<String, TableEntity.Property>();

        String errors = "";

        for (int row = 2; row != model.getRowCount(); row++) {
            TableEntity.PropertyType propertyType = (TableEntity.PropertyType) model.getValueAt(row, 2);
            String name = model.getValueAt(row, 1).toString();
            String value = model.getValueAt(row, 3).toString();

            if (!isValidPropertyName(name)) {
                errors = errors + String.format("The property name \"%s\" is invalid\n", name);
            }

            TableEntity.Property property = getProperty(value, propertyType);
            if (property == null) {
                errors = errors + String.format("The field %s has an invalid value for its type.\n", name);
            } else {
                properties.put(name, property);
            }
        }

        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this, errors, "Service Explorer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, tableEntity == null ? "Creating entity" : "Updating entity", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    if (tableEntity == null) {
                        tableEntity = StorageClientSDKManagerImpl.getManager().createTableEntity(storageAccount,
                                tableName,
                                partitionKey,
                                rowKey,
                                properties);
                    } else {
                        tableEntity.getProperties().clear();
                        tableEntity.getProperties().putAll(properties);
                        tableEntity = StorageClientSDKManagerImpl.getManager().updateTableEntity(storageAccount, tableEntity);
                    }

                    onFinish.run();
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error creating entity", e, "Service Explorer", false, true);
                }
            }
        });

        dispose();
    }

    private TableEntity.Property getProperty(String value, TableEntity.PropertyType propertyType) {
        try {
            switch (propertyType) {
                case Boolean:
                    return new TableEntity.Property(Boolean.parseBoolean(value));
                case Integer:
                    return new TableEntity.Property(Integer.parseInt(value));
                case Double:
                    return new TableEntity.Property(Double.parseDouble(value));
                case DateTime:

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new SimpleDateFormat().parse(value));

                    return new TableEntity.Property(calendar);
                case Uuid:
                    return new TableEntity.Property(UUID.fromString(value));
                case Long:
                    return new TableEntity.Property(Long.parseLong(value));
                default:
                    return new TableEntity.Property(value);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isValidPropertyName(String propertyName) {
        //Validate starting with number
        if (propertyName.matches("^[0-9]\\w*")) {
            return false;
        }
        //Validate special characters
        if (!propertyName.matches("[_\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}][\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\\p{Cf}]*")) {
            return false;
        }
        //Validate invalid keywords
        return !Arrays.asList(INVALID_KEYWORDS).contains(propertyName);
    }

    private void onCancel() {
        dispose();
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public TableEntity getTableEntity() {
        return tableEntity;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
        JButton deleteButton;

        public DeleteButtonRenderer() {
            deleteButton = new JButton();
            deleteButton.setIcon(UIHelperImpl.loadIcon("storagedelete.png"));
            deleteButton.setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int row, int i1) {
            return (row < 2) ? super.getTableCellRendererComponent(jTable, o, b, b1, row, i1) : deleteButton;
        }
    }
}