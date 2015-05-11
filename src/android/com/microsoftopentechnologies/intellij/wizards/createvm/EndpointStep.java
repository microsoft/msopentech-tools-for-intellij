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

package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.vm.Endpoint;
import com.microsoftopentechnologies.tooling.msservices.model.vm.VirtualMachine;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm.VMNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

public class EndpointStep extends WizardStep<CreateVMWizardModel> {
    private final VMServiceModule node;
    private CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JTable endpointsTable;
    private JComboBox portNameComboBox;
    private JButton addButton;
    private Project project;

    public EndpointStep(final CreateVMWizardModel model, Project project, VMServiceModule node) {
        super("Endpoint Settings", null, null);

        this.node = node;
        this.project = project;
        this.model = model;

        endpointsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        model.configStepList(createVmStepsList, 4);

        Endpoint[] defaultEndpoints = {
                new Endpoint("Remote Desktop", "TCP", 3389, 3389),
                new Endpoint("Powershell", "TCP", 5986, 5986),
                new Endpoint("Http", "TCP", 80, 80),
                new Endpoint("SSH", "TCP", 22, 22),
                new Endpoint("FTP", "TCP", 21, 21),
                new Endpoint("SMTP", "TCP", 25, 25),
                new Endpoint("MYSQL", "TCP", 3306, 3306),
                new Endpoint("MSSQL", "TCP", 1433, 1433),
                new Endpoint("DNS", "TCP", 53, 53),
                new Endpoint("POP3", "TCP", 110, 110),
                new Endpoint("POP3S", "TCP", 995, 995),
                new Endpoint("Https", "TCP", 443, 443),
                new Endpoint("SMTPS", "TCP", 587, 587),
                new Endpoint("LDAP", "TCP", 389, 389),
                new Endpoint("IMAP", "TCP", 143, 143),
                new Endpoint("WebDeploy", "TCP", 8172, 8172),
        };

        portNameComboBox.setModel(new DefaultComboBoxModel(defaultEndpoints));

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                EndpointTableModel endpointsTableModel = (EndpointTableModel) endpointsTable.getModel();

                if (portNameComboBox.getSelectedItem() instanceof Endpoint) {
                    Endpoint selectedItem = (Endpoint) portNameComboBox.getSelectedItem();
                    endpointsTableModel.getData().add(new Endpoint(
                            selectedItem.getName(),
                            selectedItem.getProtocol(),
                            selectedItem.getPrivatePort(),
                            selectedItem.getPublicPort()));
                } else {
                    endpointsTableModel.getData().add(new Endpoint(portNameComboBox.getSelectedItem().toString(), "TCP", 0, 0));
                }
                endpointsTableModel.fireTableDataChanged();
            }
        });

        final EndpointTableModel endpointTableModel = new EndpointTableModel();

        endpointsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = endpointsTable.rowAtPoint(evt.getPoint());
                int col = endpointsTable.columnAtPoint(evt.getPoint());
                if (col == 5) {
                    //Determine if click was on the "X" label
                    if (endpointsTable.getWidth() - 30 < evt.getX()) {
                        endpointTableModel.getData().remove(row);
                        endpointTableModel.fireTableDataChanged();
                    }
                }
            }
        });

        endpointTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {

                boolean hasErrors = false;
                for (int i = 0; i < endpointTableModel.getRowCount() && !hasErrors; i++) {
                    String errorFromRow = getErrorFromRow(i, endpointTableModel.getData());
                    if (errorFromRow.length() > 0) {
                        hasErrors = true;
                    }
                }

                model.getCurrentNavigationState().FINISH.setEnabled(!hasErrors);
            }
        });

        endpointsTable.setTableHeader(null);

        endpointsTable.setModel(endpointTableModel);

    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        endpointsTable.getColumnModel().getColumn(0).setPreferredWidth(15);
        endpointsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        endpointsTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        endpointsTable.getColumnModel().getColumn(3).setPreferredWidth(75);
        endpointsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        endpointsTable.getColumnModel().getColumn(5).setPreferredWidth(205);


        EndpointTableModel endpointTableModel = (EndpointTableModel) endpointsTable.getModel();

        if (model.getEndpoints() == null) {
            if (model.getVirtualMachineImage().getOperatingSystemType().equals("Windows")) {
                endpointTableModel.getData().add(new Endpoint("Powershell", "TCP", 5983, 5983));
                endpointTableModel.getData().add(new Endpoint("Remote Desktop", "TCP", 3389, 3389));
            } else {
                endpointTableModel.getData().add(new Endpoint("SSH", "TCP", 22, 22));
            }
        } else {
            endpointTableModel.getData().removeAllElements();
            for (Endpoint ep : model.getEndpoints()) {
                endpointTableModel.getData().add(ep);
            }
        }


        return rootPanel;
    }

    @Override
    public WizardStep onPrevious(CreateVMWizardModel model) {
        Vector<Endpoint> endpointData = ((EndpointTableModel) endpointsTable.getModel()).getData();
        model.setEndpoints(endpointData.toArray(new Endpoint[endpointData.size()]));

        return super.onPrevious(model);
    }

    @Override
    public boolean onFinish() {

        final EndpointTableModel tableModel = (EndpointTableModel) endpointsTable.getModel();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating virtual machine...", false) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    VirtualMachine virtualMachine = new VirtualMachine(
                            model.getName(),
                            model.getCloudService().getName(),
                            model.getCloudService().getProductionDeployment().getName(),
                            model.getAvailabilitySet(),
                            model.getSubnet(),
                            model.getSize().getName(),
                            VirtualMachine.Status.Unknown,
                            model.getSubscription().getId().toString()
                    );

                    virtualMachine.getEndpoints().addAll(tableModel.getData());

                    String certificate = model.getCertificate();
                    byte[] certData = new byte[0];

                    if (!certificate.isEmpty()) {
                        File certFile = new File(certificate);

                        if (certFile.exists()) {
                            FileInputStream certStream = null;

                            try {
                                certStream = new FileInputStream(certFile);
                                certData = new byte[(int) certFile.length()];
                                if (certStream.read(certData) != certData.length) {
                                    throw new Exception("Unable to process certificate: stream longer than informed size.");
                                }
                            } finally {
                                if (certStream != null) {
                                    try {
                                        certStream.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                            }
                        }
                    }

                    AzureSDKManagerImpl.getManager().createVirtualMachine(virtualMachine,
                            model.getVirtualMachineImage(),
                            model.getStorageAccount(),
                            model.getVirtualNetwork() != null ? model.getVirtualNetwork().getName() : "",
                            model.getUserName(),
                            model.getPassword(),
                            certData);

                    virtualMachine = AzureSDKManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);

                    final VirtualMachine vm = virtualMachine;

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                node.addChildNode(new VMNode(node, vm));
                            } catch (AzureCmdException e) {
                                DefaultLoader.getUIHelper().showException("An error occurred while trying to refresh the list of virtual machines",
                                        e,
                                        "Error Refreshing VM List",
                                        false,
                                        true);
                            }
                        }
                    });
                } catch (Exception e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying to create the specified virtual machine",
                            e,
                            "Error Creating Virtual Machine",
                            false,
                            true);
                }
            }
        });

        return super.onFinish();
    }

    private void createUIComponents() {
        endpointsTable = new JBTable() {
            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                switch (col) {
                    case 0:
                        return new ErrorRenderer();
                    case 4:
                        return new ProtocolRenderer();
                    case 5:
                        return new DeleteRenderer();
                    default:
                        return super.getCellRenderer(row, col);
                }
            }

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                if (col == 4) {
                    return new DefaultCellEditor(new ComboBox(new String[]{"TCP", "UDP"}));
                } else {
                    return super.getCellEditor(row, col);
                }
            }
        };
    }

    private static String getErrorFromRow(int row, Vector<Endpoint> list) {
        String errors = "";
        Endpoint endpoint = list.get(row);

        if (endpoint.getName().length() < 3 && endpoint.getName().length() > 15) {
            errors = errors + "The name must between 3 and 15 character long. \n";
        }

        if (!endpoint.getName().matches("^[A-Za-z0-9][A-Za-z0-9-\\s]+[A-Za-z0-9]$")) {
            errors = errors + "The name must start with a letter or number, " +
                    "contain only letters, numbers, and hyphens, " +
                    "and end with a letter or number. \n";
        }


        if (endpoint.getPrivatePort() < 1 || endpoint.getPrivatePort() > 65535) {
            errors = errors + "The private port must between a number between 1 and 65535. \n";
        }

        boolean containsName = false;
        boolean containsPublicPort = false;
        boolean containsPrivatePort = false;

        for (Endpoint ep : list) {
            if (ep != endpoint && ep.getName().equals(endpoint.getName())) {
                containsName = true;
            }

            if (ep != endpoint && ep.getProtocol().equals(endpoint.getProtocol()) && ep.getPrivatePort() == endpoint.getPrivatePort()) {
                containsPrivatePort = true;
            }

            if (ep != endpoint && ep.getProtocol().equals(endpoint.getProtocol()) && ep.getPublicPort() == endpoint.getPublicPort()) {
                containsPublicPort = true;
            }
        }

        if (containsName) {
            errors = errors + "The name must be unique. \n";
        }

        if (containsPrivatePort) {
            errors = errors + "The private port and the protocol conflicts with another in the virtual machine. \n";
        }

        if (containsPublicPort) {
            errors = errors + "The public port and the protocol conflicts with another in the virtual machine. \n";
        }

        return errors;
    }


    private class DeleteRenderer extends JPanel implements TableCellRenderer {

        public DeleteRenderer() {
            setOpaque(false);
            setLayout(new BorderLayout());
        }

        @Override
        public Component getTableCellRendererComponent(final JTable jTable, Object o, boolean b, boolean b1, final int row, int i1) {
            JLabel jLabel = new JLabel();
            jLabel.setForeground(Color.red);
            jLabel.setText("X  ");
            jLabel.setBackground(Color.WHITE);
            jLabel.setHorizontalAlignment(JButton.RIGHT);
            jLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

            this.add(jLabel, BorderLayout.LINE_END);

            return this;
        }
    }

    private class ProtocolRenderer extends ComboBox implements TableCellRenderer {

        public ProtocolRenderer() {
            super(new String[]{"TCP", "UDP"});
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean b, boolean b1, int i, int i1) {
            setSelectedItem(value);

            return this;
        }
    }

    private class ErrorRenderer extends JLabel implements TableCellRenderer {

        public ErrorRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setPreferredSize(new Dimension(getSize().width, 15));
            setForeground(Color.red);
            setHorizontalAlignment(CENTER);
            setFont(getFont().deriveFont(Font.BOLD));

            if (jTable.getModel() != null && jTable.getModel() instanceof EndpointTableModel) {
                EndpointTableModel endpointTableModel = (EndpointTableModel) jTable.getModel();
                String errorList = getErrorFromRow(row, endpointTableModel.getData());
                setToolTipText(errorList);
                setText(errorList.isEmpty() ? "" : "!");
            }

            return this;
        }
    }

    private class EndpointTableModel extends AbstractTableModel {
        private Vector<Endpoint> data = new Vector<Endpoint>();
        private String[] columns = new String[]{"", "Port name", "Public port", "Private port", "Protocol", ""};

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int i) {
            return columns[i];
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return (col > 0 && col < 5);
        }

        @Override
        public Object getValueAt(int row, int column) {
            Endpoint endpoint = data.get(row);
            switch (column) {
                case 1:
                    return endpoint.getName();
                case 2:
                    return endpoint.getPublicPort();
                case 3:
                    return endpoint.getPrivatePort();
                case 4:
                    return endpoint.getProtocol();
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object object, int row, int col) {
            Endpoint endpoint = data.get(row);
            switch (col) {
                case 1:
                    endpoint.setName(object.toString());
                    break;
                case 2:
                    try {
                        int publicPort = Integer.parseInt(object.toString());
                        endpoint.setPublicPort(publicPort);
                    } catch (NumberFormatException ignored) {
                    }

                    break;
                case 3:
                    try {
                        int privatePort = Integer.parseInt(object.toString());
                        endpoint.setPrivatePort(privatePort);
                    } catch (NumberFormatException ignored) {
                    }

                    break;
                case 4:
                    endpoint.setProtocol(object.toString());
                    break;
            }
            fireTableCellUpdated(row, col);
        }

        public Vector<Endpoint> getData() {
            return data;
        }
    }
}