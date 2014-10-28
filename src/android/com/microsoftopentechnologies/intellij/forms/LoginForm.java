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

import com.microsoftopentechnologies.intellij.helpers.InputValidator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class LoginForm extends JDialog {
    private JPanel mainPanel;
    private JTextField userNameTextField;
    private JPasswordField userPasswordField;
    private JButton loginButton;
    private JButton cancelButton;
    private Runnable onLoad;

    public LoginForm() {
        final LoginForm form = this;

        form.setContentPane(mainPanel);
        form.setTitle("Login");
        form.setModal(true);
        form.setResizable(false);

        userNameTextField.setInputVerifier(new InputValidator<JTextField>() {

            @Override
            public String validate(JTextField component) {
                String userName = component.getText();

                if(userName.isEmpty())
                    return "User name must not be empty";

                if(!userName.contains("@"))
                    return  "User name must be in the User Principal Name format (e.g.: user@domain)";

                return null;
            }
        });

        userPasswordField.setInputVerifier(new InputValidator<JPasswordField>() {
            @Override
            public String validate(JPasswordField component) {
                char[] password = component.getPassword();

                if(password.length == 0)
                    return "User name must not be empty";

                return null;
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                form.setVisible(false);
                form.dispose();
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                /*
                form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                try {
                    AzureCommandManager.login(userNameTextField.getText(), new String(userPasswordField.getPassword()));
                } catch (AzureCmdException e) {
                    form.setCursor(Cursor.getDefaultCursor());
                    UIHelper.showException("Error logging in", e);
                }


                if (onLoad != null)
                    onLoad.run();

                form.setCursor(Cursor.getDefaultCursor());

                form.setVisible(false);
                form.dispose();
                 */
            }
        });
    }


    public void setOnLoad(Runnable onLoad) {
        this.onLoad = onLoad;
    }

}
