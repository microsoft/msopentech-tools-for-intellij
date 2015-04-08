package com.microsoftopentechnologies.intellij.forms;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ViewMessageForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextArea messageTextArea;

    public ViewMessageForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setTitle("View Message");
        setResizable(false);
        setModal(true);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
    }

    private void onOK() {
// add your code here
        dispose();
    }

    public void setMessage(String message) {
        messageTextArea.setText(message);
    }
}
