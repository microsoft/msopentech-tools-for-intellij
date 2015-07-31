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
package com.microsoftopentechnologies.intellij.helpers.storage;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoftopentechnologies.intellij.forms.TableEntityForm;
import com.microsoftopentechnologies.intellij.forms.TablesQueryDesigner;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Table;
import com.microsoftopentechnologies.tooling.msservices.model.storage.TableEntity;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.*;

public class TableFileEditor implements FileEditor {
    public static final String PARTITION_KEY = "Partition key";
    public static final String ROW_KEY = "Row key";
    private static final String TIMESTAMP = "Timestamp";

    private ClientStorageAccount storageAccount;
    private Project project;
    private Table table;
    private JPanel mainPanel;
    private JButton refreshButton;
    private JButton newEntityButton;
    private JButton deleteButton;
    private JTextField queryTextField;
    private JButton queryButton;
    private JButton queryDesignerButton;
    private JTable entitiesTable;
    private List<TableEntity> tableEntities;

    private EventWaitHandle subscriptionsChanged;
    private boolean registeredSubscriptionsChanged;
    private final Object subscriptionsChangedSync = new Object();

    public TableFileEditor() {
        ActionListener queryActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fillGrid();
            }
        };

        queryButton.addActionListener(queryActionListener);
        refreshButton.addActionListener(queryActionListener);

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteSelection();
            }
        });

        newEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final TableEntityForm form = new TableEntityForm();
                form.setProject(project);
                form.setTableName(table.getName());
                form.setStorageAccount(storageAccount);
                form.setTableEntity(null);
                form.setTableEntityList(tableEntities);

                form.setTitle("Add Entity");

                form.setOnFinish(new Runnable() {
                    @Override
                    public void run() {
                        tableEntities.add(form.getTableEntity());

                        refreshGrid();
                    }
                });

                UIHelperImpl.packAndCenterJDialog(form);

                form.setVisible(true);
            }
        });

        queryDesignerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final TablesQueryDesigner form = new TablesQueryDesigner();

                form.setOnFinish(new Runnable() {
                    @Override
                    public void run() {
                        queryTextField.setText(form.getQueryText());
                    }
                });


                UIHelperImpl.packAndCenterJDialog(form);
                form.setVisible(true);
            }
        });

        entitiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        entitiesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        entitiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                deleteButton.setEnabled(entitiesTable.getSelectedRows().length > 0);
            }
        });

        entitiesTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getComponent() instanceof JTable) {
                    int r = entitiesTable.rowAtPoint(me.getPoint());

                    if (r >= 0 && r < entitiesTable.getRowCount()) {
                        entitiesTable.setRowSelectionInterval(r, r);
                    } else {
                        entitiesTable.clearSelection();
                    }


                    if (me.getClickCount() == 2) {
                        editEntity();
                    }

                    if (me.getButton() == 3) {
                        JPopupMenu popup = createTablePopUp();
                        popup.show(me.getComponent(), me.getX(), me.getY());
                    }
                }
            }
        });

        entitiesTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    editEntity();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });

        try {
            registerSubscriptionsChanged();
        } catch (AzureCmdException ignored) {
        }
    }

    private JPopupMenu createTablePopUp() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editMenu = new JMenuItem("Edit");
        editMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                editEntity();
            }
        });

        JMenuItem deleteMenu = new JMenuItem("Delete");
        deleteMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteSelection();
            }
        });

        menu.add(editMenu);
        menu.add(deleteMenu);

        return menu;
    }

    private void editEntity() {
        TableEntity[] selectedEntities = getSelectedEntities();

        if (selectedEntities != null && selectedEntities.length > 0) {
            final TableEntity selectedEntity = selectedEntities[0];

            final TableEntityForm form = new TableEntityForm();
            form.setProject(project);
            form.setTableName(table.getName());
            form.setStorageAccount(storageAccount);
            form.setTableEntity(selectedEntity);

            form.setTitle("Edit Entity");

            form.setOnFinish(new Runnable() {
                @Override
                public void run() {
                    tableEntities.set(entitiesTable.getSelectedRow(), form.getTableEntity());
                    refreshGrid();
                }
            });

            UIHelperImpl.packAndCenterJDialog(form);

            form.setVisible(true);
        }
    }

    public void fillGrid() {
        final String queryText = queryTextField.getText();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading entities", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    tableEntities = StorageClientSDKManagerImpl.getManager().getTableEntities(storageAccount, table, queryText);

                    refreshGrid();
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error querying entities", e, "Service Explorer", false, true);
                }
            }
        });
    }

    private void refreshGrid() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> columnData = new LinkedHashMap<String, List<String>>();
                columnData.put(PARTITION_KEY, new ArrayList<String>());
                columnData.put(ROW_KEY, new ArrayList<String>());
                columnData.put(TIMESTAMP, new ArrayList<String>());

                for (TableEntity tableEntity : tableEntities) {
                    columnData.get(PARTITION_KEY).add(tableEntity.getPartitionKey());
                    columnData.get(ROW_KEY).add(tableEntity.getRowKey());
                    columnData.get(TIMESTAMP).add(new SimpleDateFormat().format(tableEntity.getTimestamp().getTime()));

                    for (String entityColumn : tableEntity.getProperties().keySet()) {
                        if (!columnData.keySet().contains(entityColumn)) {
                            columnData.put(entityColumn, new ArrayList<String>());
                        }
                    }

                }

                for (TableEntity tableEntity : tableEntities) {
                    for (String column : columnData.keySet()) {
                        if (!column.equals(PARTITION_KEY) && !column.equals(ROW_KEY) && !column.equals(TIMESTAMP)) {
                            columnData.get(column).add(tableEntity.getProperties().containsKey(column)
                                    ? getFormattedProperty(tableEntity.getProperties().get(column))
                                    : "");
                        }
                    }
                }

                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int i, int i1) {
                        return false;
                    }
                };

                for (String column : columnData.keySet()) {
                    model.addColumn(column, columnData.get(column).toArray());
                }

                entitiesTable.setModel(model);

                for (int i = 0; i != entitiesTable.getColumnCount(); i++) {
                    entitiesTable.getColumnModel().getColumn(i).setPreferredWidth(100);
                }
            }
        });
    }

    private void deleteSelection() {
        final TableEntity[] selectedEntities = getSelectedEntities();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Deleting entities", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(false);

                try {
                    if (selectedEntities != null) {
                        for (int i = 0; i < selectedEntities.length; i++) {
                            progressIndicator.setFraction((double) i / selectedEntities.length);

                            StorageClientSDKManagerImpl.getManager().deleteTableEntity(storageAccount, selectedEntities[i]);
                        }

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tableEntities.removeAll(Arrays.asList(selectedEntities));

                                refreshGrid();
                            }
                        });
                    }
                } catch (AzureCmdException ex) {
                    DefaultLoader.getUIHelper().showException("Error deleting entities", ex, "Service Explorer", false, true);
                }
            }
        });
    }

    private TableEntity[] getSelectedEntities() {
        if (tableEntities == null) {
            return null;
        }

        int partitionIdIndex = -1;
        int rowIdIndex = -1;

        for (int i = 0; i < entitiesTable.getColumnCount(); i++) {
            String columnName = entitiesTable.getColumnName(i);

            if (columnName.equals(PARTITION_KEY)) {
                partitionIdIndex = i;
            }

            if (columnName.equals(ROW_KEY)) {
                rowIdIndex = i;
            }
        }

        ArrayList<TableEntity> selectedEntities = new ArrayList<TableEntity>();

        for (int i : entitiesTable.getSelectedRows()) {
            for (TableEntity tableEntity : tableEntities) {
                String partitionValue = entitiesTable.getValueAt(i, partitionIdIndex).toString();
                String rowIdValue = entitiesTable.getValueAt(i, rowIdIndex).toString();

                if (tableEntity.getPartitionKey().equals(partitionValue)
                        && tableEntity.getRowKey().equals(rowIdValue)) {
                    selectedEntities.add(tableEntity);
                }
            }
        }

        return selectedEntities.toArray(new TableEntity[selectedEntities.size()]);
    }

    @NotNull
    public static String getFormattedProperty(@NotNull TableEntity.Property property) {
        try {
            switch (property.getType()) {
                case Boolean:
                    return property.getValueAsBoolean().toString();
                case DateTime:
                    return new SimpleDateFormat().format(property.getValueAsCalendar().getTime());
                case Double:
                    return property.getValueAsDouble().toString();
                case Integer:
                    return property.getValueAsInteger().toString();
                case Long:
                    return property.getValueAsLong().toString();
                case Uuid:
                    return property.getValueAsUuid().toString();
                case String:
                    return property.getValueAsString();
            }
        } catch (AzureCmdException ignored) {
        }

        return "";
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return mainPanel;
    }

    @NotNull
    @Override
    public String getName() {
        return table.getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
        try {
            unregisterSubscriptionsChanged();
        } catch (AzureCmdException ignored) {
        }
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
    }

    private void registerSubscriptionsChanged()
            throws AzureCmdException {
        synchronized (subscriptionsChangedSync) {
            if (subscriptionsChanged == null) {
                subscriptionsChanged = AzureManagerImpl.getManager().registerSubscriptionsChanged();
            }

            registeredSubscriptionsChanged = true;

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        subscriptionsChanged.waitEvent(new Runnable() {
                            @Override
                            public void run() {
                                if (registeredSubscriptionsChanged) {
                                    Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(project, storageAccount, table);

                                    if (openedFile != null) {
                                        DefaultLoader.getIdeHelper().closeFile(project, openedFile);
                                    }
                                }
                            }
                        });
                    } catch (AzureCmdException ignored) {
                    }
                }
            });
        }
    }

    private void unregisterSubscriptionsChanged()
            throws AzureCmdException {
        synchronized (subscriptionsChangedSync) {
            registeredSubscriptionsChanged = false;

            if (subscriptionsChanged != null) {
                AzureManagerImpl.getManager().unregisterSubscriptionsChanged(subscriptionsChanged);
                subscriptionsChanged = null;
            }
        }
    }
}