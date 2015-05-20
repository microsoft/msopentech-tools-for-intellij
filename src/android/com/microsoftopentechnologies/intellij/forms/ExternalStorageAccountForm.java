package com.microsoftopentechnologies.intellij.forms;

import com.microsoftopentechnologies.intellij.helpers.LinkListener;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class ExternalStorageAccountForm extends JDialog {
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel privacyLink;
    private JTextPane connectionStringTextPane;
    private JTextField accountNameTextField;
    private JTextField accountKeyTextField;
    private JCheckBox rememberAccountKeyCheckBox;
    private JRadioButton useHTTPSRecommendedRadioButton;
    private JRadioButton useHTTPRadioButton;
    private JRadioButton specifyCustomEndpointsRadioButton;
    private JTextField blobURLTextField;
    private JTextField tableURLTextField;
    private JTextField queueURLTextField;
    private JPanel customEndpointsPanel;

    private static final String PRIVACY_LINK = "http://go.microsoft.com/fwlink/?LinkID=286720";
    private Runnable onFinish;

    public ExternalStorageAccountForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setPreferredSize(new Dimension(550, 650));
        setResizable(false);

        privacyLink.addMouseListener(new LinkListener(PRIVACY_LINK));

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

        ActionListener connectionClick = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateConnectionString();
                customEndpointsPanel.setVisible(specifyCustomEndpointsRadioButton.isSelected());
            }
        };

        useHTTPRadioButton.addActionListener(connectionClick);
        useHTTPSRecommendedRadioButton.addActionListener(connectionClick);
        specifyCustomEndpointsRadioButton.addActionListener(connectionClick);

        FocusListener focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                updateConnectionString();
            }
        };

        accountNameTextField.addFocusListener(focusListener);
        accountKeyTextField.addFocusListener(focusListener);
        rememberAccountKeyCheckBox.addFocusListener(focusListener);
        blobURLTextField.addFocusListener(focusListener);
        tableURLTextField.addFocusListener(focusListener);
        queueURLTextField.addFocusListener(focusListener);
    }

    private void updateConnectionString() {
        ArrayList<String> connStr = new ArrayList<String>();

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            connStr.add("BlobEndpoint=" + blobURLTextField.getText());
            connStr.add("QueueEndpoint=" + queueURLTextField.getText());
            connStr.add("TableEndpoint=" + tableURLTextField.getText());
        } else {
            connStr.add("DefaultEndpointsProtocol=" + (useHTTPRadioButton.isSelected() ? HTTP : HTTPS));
        }

        connStr.add("AccountName=" + accountNameTextField.getText());
        connStr.add("AccountKey=" + accountKeyTextField.getText());

        String connectionString = StringUtils.join(connStr, ";");
        connectionStringTextPane.setText(connectionString);
    }

    private void onOK() {
        String errors = "";

        if (accountNameTextField.getText().isEmpty()) {
            errors = errors + " - Missing account name.\n";
        }

        if (accountKeyTextField.getText().isEmpty()) {
            errors = errors + " - Missing account key.\n";
        }

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            if (blobURLTextField.getText().isEmpty()
                    || queueURLTextField.getText().isEmpty()
                    || tableURLTextField.getText().isEmpty()) {
                errors = errors + " - The connection string requires Blob, Table, and Queue endpoints.\n";
            }
        }

        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    errors, "Service Explorer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (onFinish != null) {
            onFinish.run();
        }

        dispose();
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        accountNameTextField.setText(storageAccount.getName());
        accountKeyTextField.setText(storageAccount.getPrimaryKey());
        specifyCustomEndpointsRadioButton.setSelected(storageAccount.isUseCustomEndpoints());

        if (storageAccount.isUseCustomEndpoints()) {
            blobURLTextField.setText(storageAccount.getBlobsUri());
            tableURLTextField.setText(storageAccount.getTablesUri());
            queueURLTextField.setText(storageAccount.getQueuesUri());
        } else {
            useHTTPRadioButton.setSelected(storageAccount.getProtocol().equals(HTTP));
            useHTTPSRecommendedRadioButton.setSelected(storageAccount.getProtocol().equals(HTTPS));
        }
        rememberAccountKeyCheckBox.setSelected(!storageAccount.getPrimaryKey().isEmpty());
        accountKeyTextField.setEnabled(false);

        updateConnectionString();
    }

    public ClientStorageAccount getStorageAccount() {
        ClientStorageAccount clientStorageAccount = new ClientStorageAccount(accountNameTextField.getText());

        if (rememberAccountKeyCheckBox.isSelected()) {
            clientStorageAccount.setPrimaryKey(accountKeyTextField.getText());
        }

        if (specifyCustomEndpointsRadioButton.isSelected()) {
            clientStorageAccount.setBlobsUri(blobURLTextField.getText());
            clientStorageAccount.setQueuesUri(queueURLTextField.getText());
            clientStorageAccount.setTablesUri(tableURLTextField.getText());
        } else {
            clientStorageAccount.setProtocol(useHTTPRadioButton.isSelected() ? HTTP : HTTPS);
        }

        return clientStorageAccount;
    }

    private void onCancel() {
        dispose();
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public String getPrimaryKey() {
        return accountKeyTextField.getText();
    }
}