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

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.model.Job;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class JobForm extends JDialog {
    private JPanel mainPanel;
    private JButton saveButton;
    private JButton cancelButton;
    private JTextField jobNameTextField;
    private JRadioButton scheduledRadioButton;
    private JComboBox intervalUnitComboBox;
    private JFormattedTextField intervalFormattedTextField;
    private JRadioButton onDemandRadioButton;
    private JCheckBox enabledCheckBox;
    private JLabel onDemandLabel;
    private JLabel everyLabel;
    private UUID id;
    private String serviceName;
    private UUID subscriptionId;
    private Project project;

    public void setAfterSave(Runnable afterSave) {
        this.afterSave = afterSave;
    }

    private Runnable afterSave;

    public JobForm() {

        final JobForm form = this;

        this.setContentPane(mainPanel);
        this.setModal(true);
        saveButton.setEnabled(false);

        ButtonGroup group = new ButtonGroup();
        group.add(onDemandRadioButton);
        group.add(scheduledRadioButton);

        jobNameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                super.keyTyped(keyEvent);

                saveButton.setEnabled(!jobNameTextField.getText().isEmpty());
            }
        });

        ActionListener radioActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setEnabledOptions();
            }
        };

        scheduledRadioButton.addActionListener(radioActionListener);
        onDemandRadioButton.addActionListener(radioActionListener);

        intervalUnitComboBox.setModel(new DefaultComboBoxModel(Job.getUnits()));

        scheduledRadioButton.setSelected(true);
        intervalFormattedTextField.setText("15");
        setEnabledOptions();

        intervalFormattedTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent jComponent) {
                if(jComponent instanceof JFormattedTextField) {
                    JFormattedTextField field = (JFormattedTextField) jComponent;

                    try {
                        Integer.parseInt(field.getText());
                    } catch(NumberFormatException e) {
                        return false;
                    }

                    return true;
                }

                return false;
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    String jobName = jobNameTextField.getText();
                    int interval = onDemandRadioButton.isSelected() ? 0 : Integer.parseInt(intervalFormattedTextField.getText());
                    String unit = onDemandRadioButton.isSelected() ? "none" : Job.getUnits()[intervalUnitComboBox.getSelectedIndex()];

                    SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                    String now = ISO8601DATEFORMAT.format(new Date());

                    if(id == null)
                        AzureRestAPIManager.getManager().createJob(subscriptionId, serviceName, jobName, interval, unit, now);
                    else {
                        AzureRestAPIManager.getManager().updateJob(subscriptionId, serviceName, jobName, interval, unit, now, enabledCheckBox.isSelected());
                    }

                    if(afterSave != null)
                        afterSave.run();

                    form.setCursor(Cursor.getDefaultCursor());

                    form.setVisible(false);
                    form.dispose();

                } catch (Throwable ex) {
                    form.setCursor(Cursor.getDefaultCursor());
                    UIHelper.showException("Error trying to save job", ex);
                }

            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                form.setVisible(false);
                form.dispose();
            }
        });
    }


    public void setJob(Job job) {
        jobNameTextField.setEnabled(false);
        enabledCheckBox.setEnabled(true);
        saveButton.setEnabled(true);

        id = job.getId();
        jobNameTextField.setText(job.getName());
        enabledCheckBox.setSelected(job.isEnabled());
        if(job.getIntervalUnit() == null) {
            onDemandRadioButton.setSelected(true);
        } else {
            scheduledRadioButton.setSelected(true);
            intervalFormattedTextField.setText(String.valueOf(job.getIntervalPeriod()));

            int index = 0;
            String[] units = Job.getUnits();
            for(int i = 0; i < units.length; i++)
                if(job.getIntervalUnit() == units[i])
                    index = i;

            intervalUnitComboBox.setSelectedIndex(index);
        }

    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    private void setEnabledOptions() {
        intervalFormattedTextField.setEnabled(scheduledRadioButton.isSelected());
        intervalUnitComboBox.setEnabled(scheduledRadioButton.isSelected());
        everyLabel.setEnabled(scheduledRadioButton.isSelected());
        onDemandLabel.setEnabled(onDemandRadioButton.isSelected());
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Job getEditingJob() {
        Job job = new Job();

        job.setId(id);
        job.setName(jobNameTextField.getText());
        job.setEnabled(enabledCheckBox.isSelected());
        if(scheduledRadioButton.isSelected()) {
            job.setIntervalUnit(Job.getUnits()[intervalUnitComboBox.getSelectedIndex()]);
            job.setIntervalPeriod(Integer.parseInt(intervalFormattedTextField.getText()));
        }

        return job;
    }
}
