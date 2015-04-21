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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.Queue;
import com.microsoftopentechnologies.intellij.model.storage.QueueMessage;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class QueueMessageForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea messageTextArea;
    private JComboBox unitComboBox;
    private JTextField expireTimeTextField;
    private StorageAccount storageAccount;
    private Queue queue;
    private Project project;
    private Runnable onAddedMessage;

    public QueueMessageForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setResizable(false);
        setPreferredSize(new Dimension(412, 370));

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

        ((AbstractDocument) expireTimeTextField.getDocument()).setDocumentFilter(new DocumentFilter() {
            Pattern pat = compile("\\d+");


            @Override
            public void replace(FilterBypass filterBypass, int i, int i1, String s, AttributeSet attributeSet) throws BadLocationException {
                if (pat.matcher(s).matches()) {
                    super.replace(filterBypass, i, i1, s, attributeSet);
                }
            }
        });

    }

    private void onOK() {

        int expireUnitFactor = 1;
        int maxSeconds = 60 * 60 * 24 * 7;

        switch(unitComboBox.getSelectedIndex()){
            case 0: //Days
                expireUnitFactor = 60 * 60 * 24;
                break;
            case 1: //Hours
                expireUnitFactor = 60 * 60;
                break;
            case 2: //Minutes
                expireUnitFactor = 60;
                break;
        }

        final int expireSeconds = expireUnitFactor * Integer.parseInt(expireTimeTextField.getText());
        final String message = messageTextArea.getText();

        if(expireSeconds > maxSeconds) {
            JOptionPane.showMessageDialog(this,
                    "The specified message time span exceeds the maximum allowed by the storage client.",
                    "Service Explorer",
                    JOptionPane.INFORMATION_MESSAGE);

            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project,  "Adding queue message", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    QueueMessage queueMessage = new QueueMessage(
                            "",
                            queue.getName(),
                            message,
                            new GregorianCalendar(),
                            new GregorianCalendar(),
                            0,
                            storageAccount.getSubscriptionId());


                    AzureSDKManagerImpl.getManager().createQueueMessage(storageAccount, queueMessage, expireSeconds);

                    if(onAddedMessage != null) {
                        ApplicationManager.getApplication().invokeLater(onAddedMessage);
                    }
                } catch (AzureCmdException e) {
                    UIHelper.showException("Error adding queue message", e, "Service Explorer", false, true);
                }
            }
        });

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setOnAddedMessage(Runnable onAddedMessage) {
        this.onAddedMessage = onAddedMessage;
    }
}
