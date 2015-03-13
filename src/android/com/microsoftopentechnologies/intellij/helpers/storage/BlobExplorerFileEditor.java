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
import com.microsoftopentechnologies.intellij.helpers.CallableSingleArg;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;


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
    private List<BlobItem> blobItems;

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

        blobListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                boolean directorySelected = isDirectorySelected() && blobListTable.getSelectedRow() >= 0;

                deleteButton.setEnabled(!directorySelected);
                openButton.setEnabled(!directorySelected);
                saveAsButton.setEnabled(!directorySelected);
            }
        });

        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
        blobListTable.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();

        sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                directoryQueue.pollLast();

                fillGrid();
            }
        });

        blobListTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if(me.getComponent() instanceof JTable) {
                    int r = blobListTable.rowAtPoint(me.getPoint());
                    if (r >= 0 && r < blobListTable.getRowCount()) {
                        blobListTable.setRowSelectionInterval(r, r);
                    } else {
                        blobListTable.clearSelection();
                    }

                    int rowIndex = blobListTable.getSelectedRow();
                    if (rowIndex < 0)
                        return;

                    if (me.getClickCount() == 2) {
                        tableSelection();
                    }

                    if(me.getButton() == 3) {

                        BlobFile fileSelection = getFileSelection();
                        if(fileSelection != null) {
                            JPopupMenu popup = createTablePopUp();
                            popup.show(me.getComponent(), me.getX(), me.getY());
                        }
                    }
                }
            }
        });

        blobListTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {}

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    tableSelection();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {}
        });

        ActionListener queryAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fillGrid();
            }
        };

        refreshButton.addActionListener(queryAction);
        queryButton.addActionListener(queryAction);

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteSelectedFile();
            }
        });

        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveAsSelectedFile();
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                downloadSelectedFile();
            }
        });

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                uploadFile();
            }
        });
    }

    public void fillGrid() {

        setUIState(true);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading blobs...", false) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);

                    if(directoryQueue.peekLast() == null) {
                        directoryQueue.addLast(AzureSDKManagerImpl.getManager().getRootDirectory(storageAccount, blobContainer));
                    }

                    blobItems = AzureSDKManagerImpl.getManager().getBlobItems(storageAccount, directoryQueue.peekLast());
                    if(!queryTextField.getText().isEmpty()) {
                        for (int i = blobItems.size() - 1; i >= 0; i--) {
                            BlobItem blobItem = blobItems.get(i);
                            if(blobItem instanceof BlobFile && !blobItem.getName().startsWith(queryTextField.getText())) {
                                blobItems.remove(i);
                            }
                        }

                    }


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

                            setUIState(false);

                            blobListTable.clearSelection();
                        }
                    });

                } catch (AzureCmdException ex) {
                    UIHelper.showException("Error querying blob list.", ex, "Error querying blobs", false, true);
                }
            }
        });

    }

    private void setUIState(boolean loading) {
        if(loading) {
            blobListTable.setEnabled(false);
            backButton.setEnabled(false);
            queryButton.setEnabled(false);
            refreshButton.setEnabled(false);
            uploadButton.setEnabled(false);
            deleteButton.setEnabled(false);
            openButton.setEnabled(false);
            saveAsButton.setEnabled(false);

        } else {
            blobListTable.setEnabled(true);
            queryButton.setEnabled(true);
            refreshButton.setEnabled(true);
            uploadButton.setEnabled(true);

            backButton.setEnabled(directoryQueue.size() > 1);
        }
    }

    private BlobDirectory getFolderSelection() {
        if(blobListTable.getSelectedRow() >= 0) {
            String name = blobListTable.getValueAt(blobListTable.getSelectedRow(), 1).toString();

            for (BlobItem item : blobItems) {
                if (item instanceof BlobDirectory && item.getName().equals(name)) {
                    return (BlobDirectory) item;
                }
            }
        }
        return null;
    }


    private BlobFile getFileSelection() {

        if(blobListTable.getSelectedRow() >= 0) {
            String name = blobListTable.getValueAt(blobListTable.getSelectedRow(), 1).toString();

            for (BlobItem item : blobItems) {
                if (item instanceof BlobFile && item.getName().equals(name)) {
                    return (BlobFile) item;
                }
            }
        }
        return null;
    }

    private boolean isDirectorySelected() {
        int selectedRow = blobListTable.getSelectedRow();

        if(selectedRow < 0) {
            return false;
        } else {
            Object icon = blobListTable.getValueAt(selectedRow, 0);
            return icon instanceof Icon;
        }
    }

    private void tableSelection() {
        if(isDirectorySelected()) {
            BlobDirectory item = getFolderSelection();

            if (item != null) {
                directoryQueue.addLast(item);
                fillGrid();
            }
        }
    }

    private JPopupMenu createTablePopUp() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem openMenu = new JMenuItem("Open");
        openMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                downloadSelectedFile();
            }
        });

        JMenuItem saveAsMenu = new JMenuItem("Save As");
        saveAsMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveAsSelectedFile();
            }
        });

        JMenuItem copyMenu = new JMenuItem("Copy URL");
        copyMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                copyURLSelectedFile();
            }
        });

        JMenuItem deleteMenu = new JMenuItem("Delete");
        deleteMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteSelectedFile();
            }
        });

        menu.add(openMenu);
        menu.add(saveAsMenu);
        menu.add(copyMenu);
        menu.add(deleteMenu);

        return menu;
    }

    private void deleteSelectedFile() {
        final BlobFile blobItem = getFileSelection();

        if(blobItem != null) {
            if(JOptionPane.showConfirmDialog(mainPanel, "Are you sure you want to delete this blob?", "Delete Blob",JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION) {
                setUIState(true);

                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Deleting blob...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        progressIndicator.setIndeterminate(true);
                        try {
                            AzureSDKManagerImpl.getManager().deleteBlobFile(storageAccount, blobItem);

                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    fillGrid();
                                }
                            });

                        } catch (AzureCmdException ex) {
                            UIHelper.showException("Error deleting blob.", ex, "Error deleting blob", false, true);
                        }
                    }
                });
            }
        }
    }

    private void copyURLSelectedFile() {
        BlobFile fileSelection = getFileSelection();
        if(fileSelection != null) {
            StringSelection selection = new StringSelection(fileSelection.getUri());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

    private void saveAsSelectedFile() {
        BlobFile fileSelection = getFileSelection();

        JFileChooser jFileChooser = new JFileChooser(new File(fileSelection.getName()));
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int saveDialog = jFileChooser.showSaveDialog(this.mainPanel);

        if(saveDialog == JFileChooser.APPROVE_OPTION) {
            downloadSelectedFile(jFileChooser.getSelectedFile());
        }
    }

    private void downloadSelectedFile() {
        String defaultFolder = System.getProperty("user.home") + File.separator + "Downloads";
        BlobFile fileSelection = getFileSelection();
        if(fileSelection != null) {
            downloadSelectedFile(new File(defaultFolder + File.separator + fileSelection.getName()));
        }
    }

    private void downloadSelectedFile(final File targetFile) {
        final BlobFile fileSelection = getFileSelection();

        if(fileSelection != null) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading blob...", true) {

                @Override
                public void run(@NotNull final ProgressIndicator progressIndicator) {


                    try {

                        progressIndicator.setIndeterminate(false);

                        if (!targetFile.exists()) {
                            if(!targetFile.createNewFile()){
                                throw new IOException("File not created");
                            }
                        }

                        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile), 65536) {
                            private long runningCount = 0;

                            @Override
                            public synchronized void write(@NotNull byte[] bytes, int i, int i1) throws IOException {
                                super.write(bytes, i, i1);

                                runningCount += i1;

                                double progress = (double) runningCount / fileSelection.getSize();
                                progressIndicator.setFraction(progress);
                                progressIndicator.setText2(String.format("%s%% downloaded", (int) (progress * 100)));
                            }

                        };
                        try {

                            Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                                @Override
                                public void run() {

                                    try {
                                        AzureSDKManagerImpl.getManager().downloadBlobFileContent(storageAccount, fileSelection, bufferedOutputStream);

                                    } catch (AzureCmdException e) {
                                        Throwable connectionFault = e.getCause().getCause();

                                        progressIndicator.setText("Error downloading Blob");
                                        progressIndicator.setText2((connectionFault instanceof SocketTimeoutException) ? "Connection timed out" : connectionFault.getMessage());
                                    }
                                }
                            });

                            while (!future.isDone()) {
                                progressIndicator.checkCanceled();
                                if (progressIndicator.isCanceled()) {
                                    future.cancel(true);
                                }
                            }

                        } finally {
                            bufferedOutputStream.close();
                        }

                    } catch (IOException e) {
                        UIHelper.showException("Error downloading Blob", e, "Error downloading Blob", false, true);
                    }
                }

            });
        }

    }


    private void uploadFile() {

        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jFileChooser.setDialogTitle("Upload blob");
        if(jFileChooser.showOpenDialog(this.mainPanel) == JFileChooser.APPROVE_OPTION) {

            final File selectedFile = jFileChooser.getSelectedFile();

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading blob...", true) {

                @Override
                public void run(@NotNull final ProgressIndicator progressIndicator) {
                    try {

                        final BlobDirectory blobDirectory = directoryQueue.peekLast();

                        final BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(selectedFile));

                        progressIndicator.setIndeterminate(false);
                        progressIndicator.setText("Uploading blob...");
                        progressIndicator.setText2("0% uploaded");

                        try {

                            CallableSingleArg<Boolean, Long> callable = new CallableSingleArg<Boolean, Long>() {
                                @Override
                                public Boolean call(Long uploadedBytes) throws Exception {
                                    double progress = ((double) uploadedBytes) / selectedFile.length();

                                    progressIndicator.setFraction(progress);
                                    progressIndicator.setText2(String.format("%s%% uploaded", (int) (progress * 100)));
                                    progressIndicator.checkCanceled();

                                    return progressIndicator.isCanceled();
                                }
                            };

                            AzureSDKManagerImpl.getManager().uploadBlobFileContent(
                                    storageAccount,
                                    blobContainer,
                                    selectedFile.getName(),
                                    bufferedInputStream,
                                    callable,
                                    1024 * 1024,
                                    selectedFile.length());

                        } catch (AzureCmdException e) {
                            progressIndicator.checkCanceled();
                            if (!progressIndicator.isCanceled()) {
                                UIHelper.showException("Error uploading", e);
                                /*
                                Throwable connectionFault = e.getCause();
                                if(connectionFault != null) {
                                    connectionFault = connectionFault.getCause();
                                }

                                progressIndicator.setText("Error uploading Blob");
                                String message = connectionFault != null ? connectionFault.getMessage() : null;
                                if (message == null) {
                                    message = "Error type " + connectionFault.getClass().getName();
                                }
                                progressIndicator.setText2((connectionFault instanceof SocketTimeoutException) ? "Connection timed out" : message);
                                */
                            }
                        }  finally {
                            bufferedInputStream.close();
                        }
                    } catch (IOException e) {
                        UIHelper.showException("Error uploading Blob", e, "Error uploading Blob", false, true);
                    }
                }
            });
        }
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return queryButton;
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
