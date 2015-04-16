package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.helpers.storage.TableFileEditor;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.TableEntity;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.DateSelectionModel;
import org.jetbrains.annotations.NotNull;
import sun.reflect.misc.ReflectUtil;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
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
    private StorageAccount storageAccount;
    private Runnable onFinish;
    private String tableName;

    private static String[] INVALID_KEYWORDS = {
        "abstract","as","base","bool","break","byte","case","catch","char","checked","class","const","continue","decimal","default","delegate","do","double","else",
        "enum","event","explicit","extern","false","finally","fixed","float","for","foreach","goto","if","implicit",
        "in","int","interface","internal","is","lock","long","namespace","new","null","object","operator","out","override","params","private","protected","public",
        "readonly","ref","return","sbyte","sealed","short","sizeof","stackalloc","static","string","struct","switch","this","throw","true",
        "try","typeof","uint","ulong","unchecked","unsafe","ushort","using","virtual","void","volatile","while"
    };


    public TableEntityForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setResizable(false);
        setPreferredSize(new Dimension(500, 400));

        DefaultTableModel model = new DefaultTableModel(){

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

        propertiesTable.getColumn("Value").setCellEditor(new ValueCellEditor());

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

        if(tableEntity != null) {
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
        final String partitionKey =  model.getValueAt(0, 3).toString();
        final String rowKey =  model.getValueAt(1, 3).toString();
        final Map<String, TableEntity.Property> properties = new LinkedHashMap<String, TableEntity.Property>();

        String errors = "";

        for(int row = 2; row != model.getRowCount(); row++) {
            TableEntity.PropertyType propertyType = (TableEntity.PropertyType) model.getValueAt(row, 2);
            String name = model.getValueAt(row, 1).toString();
            String value = model.getValueAt(row, 3).toString();

            if(!isValidPropertyName(name)){
                errors = errors + String.format("The property name \"%s\" is invalid\n", name);
            }

            TableEntity.Property property = getProperty(value, propertyType);
            if(property == null) {
                errors = errors + String.format("The field %s has an invalid value for its type.\n", name);
            } else {
                properties.put(name, property);
            }


        }

        if(errors.length() > 0) {
            JOptionPane.showMessageDialog(this, errors, "Service Explorer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, tableEntity == null ? "Creating entity" : "Updating entity", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    if (tableEntity == null) {
                        tableEntity = AzureSDKManagerImpl.getManager().createTableEntity(storageAccount,
                                tableName,
                                partitionKey,
                                rowKey,
                                properties);
                    } else {
                        tableEntity.getProperties().clear();
                        tableEntity.getProperties().putAll(properties);
                        tableEntity = AzureSDKManagerImpl.getManager().updateTableEntity(storageAccount, tableEntity);
                    }


                    onFinish.run();

                } catch (AzureCmdException e) {
                    UIHelper.showException("Error creating entity", e, "Service Explorer", false, true);
                }
            }
        });

        dispose();
    }



    private TableEntity.Property getProperty(String value, TableEntity.PropertyType propertyType) {
        try{
            switch(propertyType) {
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
        if(propertyName.matches("^[0-9]\\w*")) {
            return false;
        }
        //Validate special characters
        if(!propertyName.matches("[_\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}][\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\\p{Cf}]*")) {
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

    public void setStorageAccount(StorageAccount storageAccount) {
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
            deleteButton.setIcon(UIHelper.loadIcon("storagedelete.png"));
            deleteButton.setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int row, int i1) {
            return (row < 2) ? super.getTableCellRendererComponent(jTable, o, b, b1, row, i1) : deleteButton;
        }
    }


    private class ValueCellEditor extends DefaultCellEditor {

        Constructor constructor;
        Object value;

        public ValueCellEditor() {
            super(new JTextField());
            this.getComponent().setName("Table.editor");
        }

        @Override
        public boolean stopCellEditing() {
            String var1 = (String)super.getCellEditorValue();

            try {
                if("".equals(var1)) {
                    if(this.constructor.getDeclaringClass() == String.class) {
                        this.value = var1;
                    }

                    super.stopCellEditing();
                }

                SwingUtilities2.checkAccess(this.constructor.getModifiers());
                this.value = this.constructor.newInstance(var1);
            } catch (Exception var3) {
                ((JComponent)this.getComponent()).setBorder(new LineBorder( JBColor.RED));
                return false;
            }

            return super.stopCellEditing();
        }

        @Override
        public Component getTableCellEditorComponent(JTable jTable, Object value, boolean b, int row, int col) {
            this.value = null;
            ((JComponent)this.getComponent()).setBorder(new LineBorder(JBColor.BLACK));

            try {
                Class columnClass = jTable.getColumnClass(col);
                if(columnClass == Object.class) {
                    columnClass = String.class;
                }

                ReflectUtil.checkPackageAccess(columnClass);
                SwingUtilities2.checkAccess(columnClass.getModifiers());
                this.constructor = columnClass.getConstructor(String.class);
            } catch (Exception ignored) {
                return null;
            }

            final Component component = super.getTableCellEditorComponent(jTable, value, b, row, col);

            TableEntity.PropertyType type = (TableEntity.PropertyType) jTable.getValueAt(row, 2);
            if(type != TableEntity.PropertyType.DateTime) {
                return component;
            }

            JButton button = new JButton("...");
            button.setPreferredSize(new Dimension(button.getPreferredSize().height, 40));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final JXMonthView monthView = new JXMonthView();
                    monthView.setSelectionMode(DateSelectionModel.SelectionMode.SINGLE_SELECTION);
                    monthView.setTraversable(true);
                    monthView.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            String date = new SimpleDateFormat().format(monthView.getSelectionDate());
                            ((JTextField) component).setText(date);
                        }
                    });

                    JDialog frame = new JDialog();
                    frame.getContentPane().add(monthView);
                    frame.setModal(true);
                    frame.setAlwaysOnTop(true);
                    frame.setMinimumSize(monthView.getPreferredSize());
                    UIHelper.packAndCenterJDialog(frame);
                    frame.setVisible(true);
                }
            });

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(component, BorderLayout.CENTER);
            panel.add(button, BorderLayout.LINE_END);

            return panel;

        }

        public Object getCellEditorValue() {
            return this.value;
        }

    }
}
