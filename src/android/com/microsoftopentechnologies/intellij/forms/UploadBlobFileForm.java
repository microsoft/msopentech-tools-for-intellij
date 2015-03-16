package com.microsoftopentechnologies.intellij.forms;

import com.microsoftopentechnologies.intellij.helpers.LinkListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class UploadBlobFileForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel blobFolderLink;
    private JTextField nameTextField;
    private JButton browseButton;
    private JTextField folderTextField;

    private String folder;
    private File selectedFile;
    private Runnable uploadSelected;

    private static String linkBlob = "http://go.microsoft.com/fwlink/?LinkID=512749";

    public UploadBlobFileForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setPreferredSize(new Dimension( 600, 250));
        setResizable(false);
        setTitle("Upload blob file");

        blobFolderLink.addMouseListener(new LinkListener(linkBlob));
        nameTextField.setEditable(false);

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

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jFileChooser.setDialogTitle("Upload blob");
                if (jFileChooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {

                    selectedFile = jFileChooser.getSelectedFile();
                    nameTextField.setText(selectedFile.getAbsolutePath());

                    validateForm();
                }
            }
        });

        folderTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                folder = folderTextField.getText();
                validateForm();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                folder = folderTextField.getText();
                validateForm();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                folder = folderTextField.getText();
                validateForm();
            }
        });
    }

    private void validateForm() {
        buttonOK.setEnabled(selectedFile != null);
    }

    private void onOK() {
        try {
            folder = new URI(null, null, folder, null).getPath();
        } catch (URISyntaxException ignore) {}

        uploadSelected.run();

        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public String getFolder() {
        return folder;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setUploadSelected(Runnable uploadSelected) {
        this.uploadSelected = uploadSelected;
    }
}
