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

import com.microsoftopentechnologies.intellij.helpers.StringHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ErrorMessageForm extends JDialog {
    public static final String advancedInfoText = "Show advanced info";
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel lblError;
    private JCheckBox showAdvancedInfoCheckBox;
    private JTextArea detailTextArea;
    private JScrollPane detailScroll;

    public ErrorMessageForm(String title) {
        setContentPane(contentPane);
        setModal(true);
        setTitle(title);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        showAdvancedInfoCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setDetailsVisible(showAdvancedInfoCheckBox.isSelected());
            }
        });

        showAdvancedInfoCheckBox.setText(advancedInfoText);
    }

    public void showErrorMessageForm(String errorMessage, String details) {
        lblError.setText("<html><p>" + (errorMessage.length() > 260 ? errorMessage.substring(0, 260) + "..." : errorMessage) + "</p></html>");
        detailTextArea.setText(details);
        showAdvancedInfoCheckBox.setEnabled(!StringHelper.isNullOrWhiteSpace(details));
        this.setResizable(false);
    }

    private void setDetailsVisible(boolean visible) {
        detailScroll.setVisible(visible);

        if (visible) {
            Dimension dimension = new Dimension(detailScroll.getMinimumSize().width, detailScroll.getMinimumSize().height + 200);
            this.detailScroll.setMinimumSize(dimension);
            this.detailScroll.setPreferredSize(dimension);
            this.detailScroll.setMaximumSize(dimension);

            this.setSize(this.getSize().width, this.getSize().height + 220);
        } else {

            Dimension dimension = new Dimension(detailScroll.getMinimumSize().width, detailScroll.getMinimumSize().height - 200);
            this.detailScroll.setMinimumSize(dimension);
            this.detailScroll.setPreferredSize(dimension);
            this.detailScroll.setMaximumSize(dimension);
            this.setSize(this.getSize().width, this.getSize().height - 220);
        }

        detailScroll.repaint();

        JViewport jv = detailScroll.getViewport();
        jv.setViewPosition(new Point(0, 0));
    }
}