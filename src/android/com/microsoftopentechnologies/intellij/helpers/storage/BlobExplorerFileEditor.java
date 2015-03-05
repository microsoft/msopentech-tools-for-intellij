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
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;


public class BlobExplorerFileEditor implements FileEditor {
    private JPanel mainPanel;
    private JTextField queryTextField;
    private JTable blobListTable;
    private JButton queryButton;
    private JButton refreshButton;
    private JButton uploadButton;
    private JButton deleteButton;
    private JButton openButton;
    private JButton saveAsButton;
    private JButton backButton;
    private JLabel pathLabel;

    private StorageAccount storageAccount;
    private BlobContainer blobContainer;
    private Project project;

    private LinkedList<BlobDirectory> directoryQueue = new LinkedList<BlobDirectory>();

    public BlobExplorerFileEditor() {
        blobListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableModel model =  new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }

            public Class getColumnClass(int column) {
                return (column == 0) ? Icon.class : String.class;
            }
        };

        model.addColumn("");
        model.addColumn("Name");
        model.addColumn("Size");
        model.addColumn("Last Modified (UTC)");
        model.addColumn("Content Type");
        model.addColumn("URL");

        blobListTable.setModel(model);
        blobListTable.getColumnModel().getColumn(0).setMinWidth(20);
        blobListTable.getColumnModel().getColumn(0).setMaxWidth(20);
        blobListTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        blobListTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        blobListTable.getColumnModel().getColumn(3).setPreferredWidth(15);
        blobListTable.getColumnModel().getColumn(4).setPreferredWidth(40);
    }

    public void fillGrid() {

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading blobs...", false) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);

                    if(directoryQueue.peekLast() == null) {
                        directoryQueue.addLast(AzureSDKManagerImpl.getManager().getRootDirectory(storageAccount, blobContainer));
                    }

                    final List<BlobItem> blobItems = AzureSDKManagerImpl.getManager().getBlobItems(storageAccount, directoryQueue.peekLast());

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {


                            pathLabel.setText(directoryQueue.peekLast().getPath());
                            DefaultTableModel model = (DefaultTableModel) blobListTable.getModel();

                            while(model.getRowCount() > 0) {
                                model.removeRow(0);
                            }

                            for (BlobItem blobItem : blobItems) {
                                if(blobItem instanceof BlobDirectory) {
                                    model.addRow(new Object[]{
                                            UIHelper.loadIcon("storagefolder.png"),
                                            blobItem.getName(),
                                            "",
                                            "",
                                            "",
                                            blobItem.getUri()
                                    });
                                } else {
                                    BlobFile blobFile = (BlobFile) blobItem;

                                    model.addRow(new String[]{
                                            "",
                                            blobFile.getName(),
                                            UIHelper.readableFileSize(blobFile.getSize()),
                                            new SimpleDateFormat().format(blobFile.getLastModified().getTime()),
                                            blobFile.getContentType(),
                                            blobFile.getUri()
                                    });
                                }
                            }

                            blobListTable.removeMouseListener(blobListTable.getMouseListeners()[0]);
                            blobListTable.addMouseListener(new MouseAdapter() {
                                public void mousePressed(MouseEvent me) {
                                    JTable table =(JTable) me.getSource();
                                    Point p = me.getPoint();
                                    int row = table.rowAtPoint(p);
                                    if (me.getClickCount() == 2) {
                                        BlobItem blobItem = blobItems.get(row);

                                        if(blobItem instanceof BlobDirectory) {
                                            directoryQueue.addLast((BlobDirectory) blobItem);

                                            fillGrid();
                                        }
                                    }
                                }
                            });

                        }
                    });

                } catch (AzureCmdException ex) {
                    UIHelper.showException("Error querying blob list.", ex, "Error querying blobs", false, true);
                }
            }
        });

    }


    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return blobListTable;
    }

    @NotNull
    @Override
    public String getName() {
        return blobContainer.getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {}

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {}

    @Override
    public void deselectNotify() {}

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {}

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {}

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
    public void dispose() {}

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {}

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setBlobContainer(BlobContainer blobContainer) {
        this.blobContainer = blobContainer;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
