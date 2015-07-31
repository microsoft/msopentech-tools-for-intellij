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
import com.microsoftopentechnologies.intellij.forms.QueueMessageForm;
import com.microsoftopentechnologies.intellij.forms.ViewMessageForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Queue;
import com.microsoftopentechnologies.tooling.msservices.model.storage.QueueMessage;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.List;

public class QueueFileEditor implements FileEditor {
    private Project project;
    private ClientStorageAccount storageAccount;
    private Queue queue;
    private JPanel mainPanel;
    private JButton dequeueMessageButton;
    private JButton refreshButton;
    private JButton addMessageButton;
    private JButton clearQueueButton;
    private JTable queueTable;
    private List<QueueMessage> queueMessages;

    private EventWaitHandle subscriptionsChanged;
    private boolean registeredSubscriptionsChanged;
    private final Object subscriptionsChangedSync = new Object();

    public QueueFileEditor() {
        queueTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };

        model.addColumn("Id");
        model.addColumn("Message Text Preview");
        model.addColumn("Size");
        model.addColumn("Insertion Time (UTC)");
        model.addColumn("Expiration Time (UTC)");
        model.addColumn("Dequeue count");

        queueTable.setModel(model);
        queueTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        queueTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        queueTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        queueTable.getColumnModel().getColumn(3).setPreferredWidth(40);
        queueTable.getColumnModel().getColumn(4).setPreferredWidth(40);
        queueTable.getColumnModel().getColumn(5).setPreferredWidth(20);

        JTableHeader tableHeader = queueTable.getTableHeader();
        Dimension headerSize = tableHeader.getPreferredSize();
        headerSize.setSize(headerSize.getWidth(), 18);
        tableHeader.setPreferredSize(headerSize);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        queueTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getComponent() instanceof JTable) {
                    int r = queueTable.rowAtPoint(me.getPoint());

                    if (r >= 0 && r < queueTable.getRowCount()) {
                        queueTable.setRowSelectionInterval(r, r);
                    } else {
                        queueTable.clearSelection();
                    }

                    int rowIndex = queueTable.getSelectedRow();

                    if (rowIndex < 0) {
                        return;
                    }

                    if (me.getClickCount() == 2) {
                        viewMessageText();
                    }

                    if (me.getButton() == 3) {
                        QueueMessage message = getSelectedQueueMessage();

                        if (message != null) {
                            JPopupMenu popup = createTablePopUp(r == 0);
                            popup.show(me.getComponent(), me.getX(), me.getY());
                        }
                    }
                }
            }
        });

        queueTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    viewMessageText();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fillGrid();
            }
        });

        dequeueMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dequeueFirstMessage();
            }
        });

        addMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                QueueMessageForm queueMessageForm = new QueueMessageForm();
                queueMessageForm.setProject(project);
                queueMessageForm.setQueue(queue);
                queueMessageForm.setStorageAccount(storageAccount);

                queueMessageForm.setOnAddedMessage(new Runnable() {
                    @Override
                    public void run() {
                        fillGrid();
                    }
                });

                UIHelperImpl.packAndCenterJDialog(queueMessageForm);

                queueMessageForm.setVisible(true);
            }
        });


        clearQueueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int optionDialog = JOptionPane.showOptionDialog(null,
                        "Are you sure you want to clear the queue \"" + queue.getName() + "\"?",
                        "Service explorer",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Yes", "No"},
                        null);

                if (optionDialog == JOptionPane.YES_OPTION) {
                    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Clearing queue messages", false) {
                        @Override
                        public void run(@NotNull ProgressIndicator progressIndicator) {
                            try {

                                StorageClientSDKManagerImpl.getManager().clearQueue(storageAccount, queue);

                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        fillGrid();
                                    }
                                });
                            } catch (AzureCmdException e) {
                                DefaultLoader.getUIHelper().showException("Error clearing queue messages", e, "Service Explorer", false, true);
                            }
                        }
                    });
                }
            }
        });

        try {
            registerSubscriptionsChanged();
        } catch (AzureCmdException ignored) {
        }
    }

    public void fillGrid() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading queue messages", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    queueMessages = StorageClientSDKManagerImpl.getManager().getQueueMessages(storageAccount, queue);

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DefaultTableModel model = (DefaultTableModel) queueTable.getModel();

                            while (model.getRowCount() > 0) {
                                model.removeRow(0);
                            }

                            for (QueueMessage queueMessage : queueMessages) {
                                String[] values = {
                                        queueMessage.getId(),
                                        queueMessage.getContent(),
                                        UIHelperImpl.readableFileSize(queueMessage.getContent().length()),
                                        new SimpleDateFormat().format(queueMessage.getInsertionTime().getTime()),
                                        new SimpleDateFormat().format(queueMessage.getExpirationTime().getTime()),
                                        String.valueOf(queueMessage.getDequeueCount()),
                                };

                                model.addRow(values);
                            }

                            clearQueueButton.setEnabled(queueMessages.size() != 0);
                            dequeueMessageButton.setEnabled(queueMessages.size() != 0);
                        }
                    });

                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error getting queue messages", e, "Service Explorer", false, true);
                }
            }
        });
    }

    private JPopupMenu createTablePopUp(boolean isFirstRow) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem openMenu = new JMenuItem("Open");
        openMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewMessageText();
            }
        });

        JMenuItem dequeueMenu = new JMenuItem("Dequeue");
        dequeueMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dequeueFirstMessage();
            }
        });
        dequeueMenu.setEnabled(isFirstRow);

        menu.add(openMenu);
        menu.add(dequeueMenu);

        return menu;
    }

    private void dequeueFirstMessage() {
        if (JOptionPane.showConfirmDialog(mainPanel,
                "Are you sure you want to dequeue the first message in the queue?",
                "Service Explorer",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Dequeuing message", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        StorageClientSDKManagerImpl.getManager().dequeueFirstQueueMessage(storageAccount, queue);

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fillGrid();
                            }
                        });
                    } catch (AzureCmdException e) {
                        DefaultLoader.getUIHelper().showException("Error dequeuing messages", e, "Service Explorer", false, true);
                    }
                }
            });
        }
    }

    private QueueMessage getSelectedQueueMessage() {
        return (queueMessages != null && queueMessages.size() > 0)
                ? queueMessages.get(queueTable.getSelectedRow()) : null;
    }

    private void viewMessageText() {
        ViewMessageForm viewMessageForm = new ViewMessageForm();
        viewMessageForm.setMessage(queueMessages.get(queueTable.getSelectedRow()).getContent());

        UIHelperImpl.packAndCenterJDialog(viewMessageForm);
        viewMessageForm.setVisible(true);
    }

    public void setProject(Project project) {
        this.project = project;
    }


    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
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
        return queue.getName();
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
                                    Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(project, storageAccount, queue);

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