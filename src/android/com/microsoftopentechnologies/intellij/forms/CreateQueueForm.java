package com.microsoftopentechnologies.intellij.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.LinkListener;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Calendar;

public class CreateQueueForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel namingGuidelinesLink;
    private JTextField nameTextField;
    private Runnable onCreate;
    private StorageAccount storageAccount;
    private Project project;

    public CreateQueueForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setResizable(false);
        setPreferredSize(new Dimension(412, 170));

        setTitle("Create queue");
        namingGuidelinesLink.addMouseListener(new LinkListener("http://go.microsoft.com/fwlink/?LinkId=255557"));

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

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                changedName();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                changedName();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                changedName();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void changedName() {
        buttonOK.setEnabled(nameTextField.getText().length() > 0);
    }

    private void onOK() {
        final String name = nameTextField.getText();
        if (name.length() < 3 || name.length() > 63 || !name.matches("^[a-z0-9](?!.*--)[a-z0-9-]+[a-z0-9]$")) {
            JOptionPane.showMessageDialog(this, "Queue names must start with a letter or number, and can contain only letters, numbers, and the dash (-) character.\n" +
                    "Every dash (-) character must be immediately preceded and followed by a letter or number; consecutive dashes are not permitted in container names.\n" +
                    "All letters in a container name must be lowercase.\n" +
                    "Queue names must be from 3 through 63 characters long.", "Service Explorer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating blob container...", false) {

            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);


                    for (Queue queue : AzureSDKManagerImpl.getManager().getQueues(storageAccount)) {

                        if (queue.getName().equals(name)) {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(null, "A queue with the specified name already exists.", "Service Explorer", JOptionPane.ERROR_MESSAGE);
                                }
                            });

                            return;

                        }
                    }


                    Queue queue = new Queue(name, "", 0, storageAccount.getSubscriptionId());
                    AzureSDKManagerImpl.getManager().createQueue(storageAccount, queue);

                    if (onCreate != null) {
                        ApplicationManager.getApplication().invokeLater(onCreate);
                    }
                } catch(AzureCmdException e) {
                    UIHelper.showException("Error creating queue", e, "Service explorer", false, true);
                }
            }
        });

        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
