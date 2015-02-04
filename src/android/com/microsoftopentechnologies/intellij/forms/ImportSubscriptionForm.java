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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.microsoftopentechnologies.intellij.helpers.LinkListener;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ImportSubscriptionForm extends JDialog {
    private JPanel mainPanel;
    private JButton browseButton;
    private JButton cancelButton;
    private JTextField txtFile;
    private JButton importButton;
    private JLabel lblDownload;
    private JLabel lblPolicy;

    public ImportSubscriptionForm() {
        lblPolicy.addMouseListener(new LinkListener("http://msdn.microsoft.com/en-us/vstudio/dn425032.aspx"));
        lblDownload.addMouseListener(new LinkListener("http://go.microsoft.com/fwlink/?LinkID=301775"));

        this.setResizable(false);
        this.setModal(true);
        this.setTitle("Import Microsoft Azure Subscriptions");
        this.setContentPane(mainPanel);

        final ImportSubscriptionForm form = this;

        mainPanel.getRootPane().setDefaultButton(null);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        try {
                            return file.isDirectory() || (file.getExtension() != null && file.getExtension().equals("publishsettings"));
                        } catch (Throwable t) {
                            return super.isFileVisible(file, showHiddenFiles);
                        }
                    }

                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return (file.getExtension() != null && file.getExtension().equals("publishsettings"));
                    }
                };
                fileChooserDescriptor.setTitle("Choose Subscriptions File");

                FileChooser.chooseFile(fileChooserDescriptor, null, null, new Consumer<VirtualFile>() {
                    @Override
                    public void consume(VirtualFile virtualFile) {
                        if (virtualFile != null) {
                            txtFile.setText(virtualFile.getPath());
                            importButton.setEnabled(true);
                            mainPanel.getRootPane().setDefaultButton(importButton);
                        }
                    }
                });
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                form.setVisible(false);
                form.dispose();
            }
        });

        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (new File(txtFile.getText()).exists()) {
                    try {
                        form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        AzureRestAPIManager.getManager().loadSubscriptionFile(txtFile.getText());
                        if (form.onSubscriptionLoaded != null)
                            form.onSubscriptionLoaded.run();

                        form.setCursor(Cursor.getDefaultCursor());

                        form.setVisible(false);
                        form.dispose();
                    } catch (Throwable e) {
                        form.setCursor(Cursor.getDefaultCursor());
                        UIHelper.showException("Error: " + e.getMessage(), e);
                    }
                } else {
                    UIHelper.showException("The specified Subscriptions File does not exist.",
                            null,
                            "Invalid Subscriptions File Path",
                            false);
                }
            }
        });

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                importButton.setEnabled(txtFile.getText() != null && !txtFile.getText().trim().isEmpty());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                importButton.setEnabled(txtFile.getText() != null && !txtFile.getText().trim().isEmpty());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                importButton.setEnabled(txtFile.getText() != null && !txtFile.getText().trim().isEmpty());
            }
        };

        txtFile.getDocument().addDocumentListener(documentListener);

        importButton.setEnabled(false);
    }

    private Runnable onSubscriptionLoaded;

    public void setOnSubscriptionLoaded(Runnable onSubscriptionLoaded) {
        this.onSubscriptionLoaded = onSubscriptionLoaded;
    }
}