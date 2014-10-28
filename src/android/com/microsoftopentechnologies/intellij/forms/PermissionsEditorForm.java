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

import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckBoxListListener;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoftopentechnologies.intellij.model.Office365Permission;
import com.microsoftopentechnologies.intellij.model.Office365PermissionList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PermissionsEditorForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel panelPermissions;
    private CheckBoxList<Office365Permission> listPermissions;
    private Office365PermissionList permissions;
    private DialogResult dialogResult;

    public enum DialogResult {
        OK,
        CANCEL
    }

    public PermissionsEditorForm(String title, Office365PermissionList office365Permissions) {
        this.permissions = new Office365PermissionList(office365Permissions.size());
        for(Office365Permission p : office365Permissions) {
            this.permissions.add(p.clone());
        }

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Edit Permissions : " + title);
        setModal(true);
        setPreferredSize(new Dimension(420, 280));

        // populate list box
        listPermissions = new CheckBoxList<Office365Permission>();
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        constraints.setColumn(0);
        constraints.setFill(GridConstraints.FILL_BOTH);
        panelPermissions.add(listPermissions, constraints);
        for(Office365Permission permission : this.permissions) {
            listPermissions.addItem(permission, permission.getDescription(), permission.isEnabled());
        }

        // this updates the datamodel when the checkbox is clicked in the listbox
        listPermissions.setCheckBoxListListener(new CheckBoxListListener() {
            @Override
            public void checkBoxSelectionChanged(int i, boolean b) {
                permissions.get(i).setEnabled(b);
            }
        });

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

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        dialogResult = DialogResult.OK;
        dispose();
    }

    private void onCancel() {
        dialogResult = DialogResult.CANCEL;
        dispose();
    }

    public DialogResult getDialogResult() {
        return dialogResult;
    }

    public Office365PermissionList getPermissions() {
        return permissions;
    }
}
