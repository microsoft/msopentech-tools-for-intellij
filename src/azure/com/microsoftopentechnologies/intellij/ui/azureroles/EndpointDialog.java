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

package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.interopbridges.tools.windowsazure.WindowsAzureEndpoint;
import com.interopbridges.tools.windowsazure.WindowsAzureEndpointType;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.roleoperations.WAEndpointDialogUtilMethods;
import com.microsoftopentechnologies.util.WAEclipseHelperMethods;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class EndpointDialog extends DialogWrapper {
    private static final String AUTO = "(auto)";

    private JPanel contentPane;
    private JTextField txtPublicPort;
    private JTextField txtPublicPortRangeEnd;
    private JTextField txtPrivatePort;
    private JTextField txtPrivatePortRangeEnd;
    private JComboBox comboType;
    private JTextField txtName;
    private JLabel lblPrivatePort;
    private JLabel lblPrivatePortRangeSeparator;
    private JLabel lblPublicPort;
    private JLabel lblPublicPortRangeSeparator;
    private JLabel lblName;
    private WindowsAzureRole waRole;
    private WindowsAzureEndpoint waEndpt;

    public EndpointDialog(WindowsAzureRole waRole, WindowsAzureEndpoint waEndpt) {
        super(true);
        this.waRole = waRole;
        this.waEndpt = waEndpt;
        init();
    }

    @Override
    protected void init() {
        setTitle(waEndpt == null ? message("adRolEndPtTitle") : message("endptEditTitle"));
        comboType.setModel(new DefaultComboBoxModel(WindowsAzureEndpointType.values()));
        comboType.addItemListener(createComboTypeListener());
        lblName.setPreferredSize(lblPrivatePort.getPreferredSize());
        // Edit Endpoint scenario
        if (waEndpt != null) {
            txtName.setText(waEndpt.getName());
            // type
            WindowsAzureEndpointType type = null;
            try {
                type = waEndpt.getEndPointType();
                comboType.setSelectedItem(type);
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialog(message("dlgDbgEndPtErrTtl"), message("endPtTypeErr"));
            }
            // private port
            String prvPort = waEndpt.getPrivatePort();
            if (prvPort == null && !type.equals(WindowsAzureEndpointType.InstanceInput)) {
                txtPrivatePort.setText(AUTO);
            } else {
                String[] prvPortRange = prvPort.split("-");
                txtPrivatePort.setText(prvPortRange[0]);
                if (prvPortRange.length > 1) {
                    txtPrivatePortRangeEnd.setText(prvPortRange[1]);
                }
            }
            // Public port
            String[] portRange = waEndpt.getPort().split("-");
            txtPublicPort.setText(portRange[0]);
            if (portRange.length > 1) {
                txtPublicPortRangeEnd.setText(portRange[1]);
            }
        } else {
        	/*
        	 * Add Endpoint scenario.
        	 * Endpoint type is Internal for the first time.
        	 */
            txtPrivatePort.setText(AUTO);
        }
        enableControlsDependingOnEnpointType((WindowsAzureEndpointType) comboType.getSelectedItem());
        super.init();
    }

    private ItemListener createComboTypeListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                WindowsAzureEndpointType endpointType = (WindowsAzureEndpointType) comboType.getSelectedItem();
                String portTxt = txtPrivatePort.getText();
                enableControlsDependingOnEnpointType(endpointType);
        		/*
        		 * auto not allowed for InstanceInput endpoint,
        		 * hence clear it.
        		 */
                if (endpointType == WindowsAzureEndpointType.InstanceInput && portTxt.equalsIgnoreCase(AUTO)) {
                    txtPrivatePort.setText("");
                } else if (endpointType == WindowsAzureEndpointType.Input && (portTxt.isEmpty() || portTxt.equalsIgnoreCase("*"))) {
                    txtPrivatePort.setText(AUTO);
                } else if (endpointType == WindowsAzureEndpointType.Internal && (portTxt.isEmpty() || portTxt.equalsIgnoreCase("*"))
                        && txtPrivatePortRangeEnd.getText().isEmpty()) {
                    txtPrivatePort.setText(AUTO);
                }
            }
        };
    }

    /**
     * Enabled/disables controls depending on endpoint type.
     */
    private void enableControlsDependingOnEnpointType(WindowsAzureEndpointType endpointType) {
        if (endpointType == WindowsAzureEndpointType.Internal) {
            // Internal port selected
            // Handling for private port
            lblPrivatePort.setText(message("adRolPrvPortRng"));
            lblPrivatePortRangeSeparator.setEnabled(true);
            txtPrivatePortRangeEnd.setEnabled(true);
            // Handling for public port
            lblPublicPort.setEnabled(false);
            lblPublicPort.setText(message("adRolPubPortRange"));
            txtPublicPort.setEnabled(false);
            txtPublicPort.setText("");
            txtPublicPortRangeEnd.setText("");
            lblPublicPortRangeSeparator.setEnabled(false);
            txtPublicPortRangeEnd.setEnabled(false);
        } else if (endpointType == WindowsAzureEndpointType.Input) {
            // Input port selected
            // Handling for private port
            lblPrivatePort.setText(message("adRolPrivatePort"));
            lblPrivatePortRangeSeparator.setEnabled(false);
            txtPrivatePortRangeEnd.setEnabled(false);
            txtPrivatePortRangeEnd.setText("");
            // Handling for public port
            lblPublicPort.setEnabled(true);
            lblPublicPort.setText(message("adRolPubPort"));
            txtPublicPort.setEnabled(true);
            lblPublicPortRangeSeparator.setEnabled(false);
            txtPublicPortRangeEnd.setEnabled(false);
            txtPublicPortRangeEnd.setText("");
        }  else {
            // Instance input point selected
            // Handling for private port
            lblPrivatePort.setText(message("adRolPrivatePort"));
            lblPrivatePortRangeSeparator.setEnabled(false);
            txtPrivatePortRangeEnd.setEnabled(false);
            txtPrivatePortRangeEnd.setText("");
            // Handling for public port
            lblPublicPort.setEnabled(true);
            lblPublicPort.setText(message("adRolPubPortRange"));
            txtPublicPort.setEnabled(true);
            lblPublicPortRangeSeparator.setEnabled(true);
            txtPublicPortRangeEnd.setEnabled(true);
        }
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public void doOKAction() {
        try {
            if (waEndpt != null) {
                //Edit an endpoint scenario
                if (!editEndpt()) {
                    return;
                }
            } else {
                //Add an endpoint scenario
                // validate name
                WindowsAzureEndpointType endPtType = (WindowsAzureEndpointType) comboType.getSelectedItem();
                String endPtName = txtName.getText().trim();
            	/*
            	 * Check endpoint name contain
            	 * alphanumeric and underscore characters only.
            	 * Starts with alphabet.
            	 */
                if (WAEclipseHelperMethods.isAlphaNumericUnderscore(endPtName)) {
                    boolean isValidName = waRole.isAvailableEndpointName(endPtName, endPtType);
            		/*
            		 * Check already used endpoint name is given.
            		 */
                    if (isValidName) {
                        if (endPtType.equals(WindowsAzureEndpointType.InstanceInput) || endPtType.equals(WindowsAzureEndpointType.Internal)) {
                            if(WAEndpointDialogUtilMethods.isDashPresent(endPtType, txtPrivatePort.getText(), txtPrivatePortRangeEnd.getText(),
                                    txtPublicPort.getText(), txtPublicPortRangeEnd.getText())) {
                                PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("portRangeErrMsg"));
                                return;
                            }
                        }
                        // Check for valid range 1 to 65535
                        if (WAEndpointDialogUtilMethods.isValidPortRange(endPtType, txtPrivatePort.getText(), txtPrivatePortRangeEnd.getText(),
                                txtPublicPort.getText(), txtPublicPortRangeEnd.getText())) {
                            // Combine port range
                            String publicPort = WAEndpointDialogUtilMethods.combinePublicPortRange(txtPublicPort.getText(), txtPublicPortRangeEnd.getText(),
                                    endPtType.toString());
                            String privatePort = WAEndpointDialogUtilMethods.combinePrivatePortRange(txtPrivatePort.getText(), txtPrivatePortRangeEnd.getText(),
                                    endPtType.toString());
                            if (privatePort.equalsIgnoreCase(AUTO)) {
                                privatePort = null;
                            }
                            // Validate and commit endpoint addition
                            if (waRole.isValidEndpoint(endPtName, endPtType, privatePort, publicPort)) {
                                waRole.addEndpoint(endPtName, endPtType, privatePort, publicPort);
                            } else {
                                PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dlgPortInUse"));
                                return;
                            }
                        } else {
                            PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("rngErrMsg"));
                            return;
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("dlgInvdEdPtName1"), message("dlgInvdEdPtName2"));
                        return;
                    }
                } else {
                    PluginUtil.displayErrorDialog(message("dlgInvdEdPtName1"), message("enPtAlphNuMsg"));
                    return;
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException ex) {
            PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            return;
        }
        super.doOKAction();
    }

    /**
     * This method edits an endpoint.
     * For editing it also validates endpoint name and ports.
     *
     * @return retVal : false if any error occurs.
     * @throws WindowsAzureInvalidProjectOperationException
     */
    private boolean editEndpt() throws WindowsAzureInvalidProjectOperationException {
        boolean retVal = true;
        String oldEndptName = waEndpt.getName();
        String newEndptName = txtName.getText().trim();
        //validate endpoint name
        /*
         * Ignore if end point name is not edited.
         */
        if (!oldEndptName.equalsIgnoreCase(newEndptName)) {
        	/*
        	 * Check endpoint name contain
        	 * alphanumeric and underscore characters only.
        	 * Starts with alphabet.
        	 */
            if (WAEclipseHelperMethods.isAlphaNumericUnderscore(newEndptName)) {
        		/*
        		 * Check already used endpoint name is given.
        		 */
                boolean isValid = waRole.isAvailableEndpointName(newEndptName, (WindowsAzureEndpointType) comboType.getSelectedItem());
                if (!isValid) {
                    //if name is not valid
                    PluginUtil.displayErrorDialog(message("dlgInvdEdPtName1"), message("dlgInvdEdPtName2"));
                    retVal = false;
                }
            } else {
                PluginUtil.displayErrorDialog(message("dlgInvdEdPtName1"), message("enPtAlphNuMsg"));
                retVal = false;
            }
        }
        if (retVal) {
            retVal = validatePorts(oldEndptName);
        }
        return retVal;
    }

    /**
     * Disables the debugging if debug endpoint's type is changed to 'Internal',
     * and if private port is modified then assigns the new debugging port
     * by setting the modified endpoint as a debugging endpoint.
     *
     * @param oldType : old type of the endpoint.
     * @return retVal : false if any error occurs.
     * @throws WindowsAzureInvalidProjectOperationException
     */
    private boolean handleChangeForDebugEndpt(WindowsAzureEndpointType oldType, String privatePort)
            throws WindowsAzureInvalidProjectOperationException {
        boolean retVal = true;
        if (oldType.equals(WindowsAzureEndpointType.Input) && comboType.getSelectedItem().equals(WindowsAzureEndpointType.Internal.toString())) {
            int choice = Messages.showYesNoDialog(String.format("%s%s%s", message("dlgEPDel"), message("dlgEPChangeType"), message("dlgEPDel2")),
                    message("dlgTypeTitle"), Messages.getQuestionIcon());
            if (choice == Messages.YES) {
                waEndpt.setEndPointType((WindowsAzureEndpointType) comboType.getSelectedItem());
                waRole.setDebuggingEndpoint(null);
            } else {
                retVal = false;
            }
        } else if (privatePort == null) {
            PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dbgPort"));
            retVal = false;
        } else if (!waEndpt.getPrivatePort().equalsIgnoreCase(privatePort)) {
            boolean isSuspended = waRole.getStartSuspended();
            waRole.setDebuggingEndpoint(null);
            waEndpt.setPrivatePort(privatePort);
            waRole.setDebuggingEndpoint(waEndpt);
            waRole.setStartSuspended(isSuspended);
        }
        return retVal;
    }

    /**
     * Validates public and private ports.
     * And also makes changes corresponding to the debug endpoint.
     *
     * @param oldEndptName : old name of the endpoint.
     * @return retVal : false if any error occurs.
     * @throws WindowsAzureInvalidProjectOperationException
     */
    private boolean validatePorts(String oldEndptName) throws WindowsAzureInvalidProjectOperationException {
        boolean retVal = true;
        boolean isDash = false;
        WindowsAzureEndpointType oldType = waEndpt.getEndPointType();
        WindowsAzureEndpoint debugEndpt = waRole.getDebuggingEndpoint();
        WindowsAzureEndpoint stickyEndpt = waRole.getSessionAffinityInputEndpoint();
        WindowsAzureEndpoint stickyIntEndpt = waRole.getSessionAffinityInternalEndpoint();
        String stcEndptName = "";
        String dbgEndptName = "";
        String stcIntEndptName = "";
        if (debugEndpt != null) {
            //get the debugging endpoint name
            dbgEndptName = debugEndpt.getName();
        }
        if (stickyEndpt != null) {
            stcEndptName = stickyEndpt.getName();
            stcIntEndptName = stickyIntEndpt.getName();
        }

        WindowsAzureEndpointType newType = (WindowsAzureEndpointType) comboType.getSelectedItem();
        if (newType.equals(WindowsAzureEndpointType.InstanceInput) || newType.equals(WindowsAzureEndpointType.Internal)) {
            isDash = WAEndpointDialogUtilMethods.isDashPresent(newType, txtPrivatePort.getText(), txtPrivatePortRangeEnd.getText(),
                    txtPublicPort.getText(), txtPublicPortRangeEnd.getText());
        }
        if (isDash) {
            PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("portRangeErrMsg"));
            retVal = false;
        } else {
            // Check for valid range 1 to 65535
            if (WAEndpointDialogUtilMethods.isValidPortRange(newType, txtPrivatePort.getText(), txtPrivatePortRangeEnd.getText(),
                    txtPublicPort.getText(), txtPublicPortRangeEnd.getText())) {
                //validate ports
                String publicPort = WAEndpointDialogUtilMethods.combinePublicPortRange(txtPublicPort.getText(), txtPublicPortRangeEnd.getText(),
                        comboType.getSelectedItem().toString());
                String privatePort = WAEndpointDialogUtilMethods.combinePrivatePortRange(txtPrivatePort.getText(), txtPrivatePortRangeEnd.getText(),
                        comboType.getSelectedItem().toString());
                if (privatePort.equalsIgnoreCase(AUTO)) {
                    privatePort = null;
                }

                boolean isValidendpoint = waRole.isValidEndpoint(oldEndptName, newType, privatePort, publicPort);
                if (isValidendpoint) {
                    if (oldEndptName.equalsIgnoreCase(dbgEndptName)) {
                        retVal = handleChangeForDebugEndpt(oldType, privatePort);
                    }
                    /**
                     * Disables the session affinity
                     * if endpoint's type is changed to 'Internal'.
                     */
                    if (oldEndptName.equalsIgnoreCase(stcEndptName) || oldEndptName.equalsIgnoreCase(stcIntEndptName)) {
                        retVal = false;
                    }
                    if (retVal) {
                        //set the new values in the endpoint object.
                        waEndpt.setEndPointType((WindowsAzureEndpointType) comboType.getSelectedItem());
                        waEndpt.setName(txtName.getText());
        				/*
        				 * Type is Input or Instance then
        				 * set public port as well as private port.
        				 */
                        if (comboType.getSelectedItem() == WindowsAzureEndpointType.Input
                                || comboType.getSelectedItem() == WindowsAzureEndpointType.InstanceInput) {
                            waEndpt.setPort(publicPort);
                        }
        				/*
        				 * Type is Internal then
        				 * set private port only.
        				 */
                        waEndpt.setPrivatePort(privatePort);
                    }
                } else {
                    PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dlgPortInUse"));
                    retVal = false;
                }
            } else {
                PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("rngErrMsg"));
                retVal = false;
            }
        }
        return retVal;
    }
}
