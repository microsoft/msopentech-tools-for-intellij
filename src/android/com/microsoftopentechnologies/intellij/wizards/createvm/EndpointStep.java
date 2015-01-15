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

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;

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
import java.util.Vector;

public class EndpointStep extends WizardStep<CreateVMWizardModel> {
    private CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JTable endpointsTable;
    private JComboBox portNameComboBox;
    private JButton addButton;

    public EndpointStep(final CreateVMWizardModel model) {
        super("Endpoint Settings", null);

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

                Endpoint endpoint = (portNameComboBox.getSelectedItem() instanceof Endpoint)
                        ? (Endpoint) portNameComboBox.getSelectedItem()
                        : new Endpoint(portNameComboBox.getSelectedItem().toString(), "TCP", 0, 0);

                endpointsTableModel.getData().add(endpoint);
                endpointsTableModel.fireTableDataChanged();
            }
        });

        final EndpointTableModel endpointTableModel = new EndpointTableModel();
        endpointTableModel.getData().add(new Endpoint("Powershell", "TCP", 5983, 5983));
        endpointTableModel.getData().add(new Endpoint("Remote Desktop", "TCP", 3389, 3389));

        endpointsTable.setModel(endpointTableModel);

        endpointsTable.getColumnModel().getColumn(0).setPreferredWidth(15);
        endpointsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        endpointsTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        endpointsTable.getColumnModel().getColumn(3).setPreferredWidth(75);
        endpointsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        endpointsTable.getColumnModel().getColumn(5).setPreferredWidth(205);

        endpointsTable.setTableHeader(null);


        endpointsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = endpointsTable.rowAtPoint(evt.getPoint());
                int col = endpointsTable.columnAtPoint(evt.getPoint());
                if (col == 5) {
                    //Determine if click was on the "X" label
                    if(endpointsTable.getWidth() - 30 < evt.getX()) {
                        endpointTableModel.getData().remove(row);
                        endpointTableModel.fireTableDataChanged();
                    }
                }
            }
        });

        endpointTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {
                model.getCurrentNavigationState().FINISH.setEnabled(endpointTableModel.getRowCount() > 0);
            }
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();
        return rootPanel;
    }

    @Override
    public boolean onFinish() {

        EndpointTableModel tableModel = (EndpointTableModel) endpointsTable.getModel();
        model.setEndpoints(tableModel.getData().toArray(new Endpoint[]{}));

        return super.onFinish();
    }

    private void createUIComponents() {
        endpointsTable = new JBTable() {
            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                switch(col){
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
                if(col == 4) {
                    return new DefaultCellEditor(new ComboBox(new String[] { "TCP", "UDP" }));
                } else {
                    return super.getCellEditor(row, col);
                }
            }
        };
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
            super(new String[] { "TCP", "UDP" });
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean b, boolean b1, int i, int i1) {
            setSelectedItem(value);

            return this;
        }
    }

    private class ErrorRenderer extends JLabel implements TableCellRenderer  {

        public ErrorRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setPreferredSize(new Dimension(getSize().height, 15));
            setForeground(Color.red);
            setHorizontalAlignment(CENTER);
            setFont(getFont().deriveFont(Font.BOLD));

            if(jTable.getModel() != null && jTable.getModel() instanceof EndpointTableModel) {
                EndpointTableModel endpointTableModel = (EndpointTableModel) jTable.getModel();
                String errorList = getErrorFromRow(row, endpointTableModel.getData());
                setToolTipText(errorList);
                setText(errorList.isEmpty() ? "" : "!");
            }

            return this;
        }

        public String getErrorFromRow(int row, Vector<Endpoint> list) {
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

            if (endpoint.getPublicPort() > 1 || endpoint.getPublicPort() < 65535) {
                errors = errors + "The public port must between a number between 1 and 65535. \n";
            }

            if (endpoint.getPrivatePort() > 1 || endpoint.getPrivatePort() < 65535) {
                errors = errors + "The private port must between a number between 1 and 65535. \n";
            }

            boolean containsName = false;
            for (Endpoint ep : list) {
                if(ep.getName().equals(endpoint.getName())) {
                    containsName = true;
                }
            }

            if(!containsName) {
                errors = errors + "The name must be unique. \n";
            }

            return errors;
        }

    }

    private class EndpointTableModel extends AbstractTableModel {
        private Vector<Endpoint> data = new Vector<Endpoint>();
        private String[] columns = new String[]{ "", "Port name", "Public port", "Private port", "Protocol", "" };

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
        public Object getValueAt(int row, int column){
            Endpoint endpoint = data.get(row);
            switch(column) {
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
            switch(col) {
                case 1:
                    endpoint.setName(object.toString());
                    break;
                case 2:
                    try {
                        int publicPort = Integer.parseInt(object.toString());
                        endpoint.setPublicPort(publicPort);
                    } catch (NumberFormatException ex) {}
                    break;
                case 3:
                    try{
                        int privatePort = Integer.parseInt(object.toString());
                        endpoint.setPrivatePort(privatePort);
                    } catch (NumberFormatException ex) {}
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
