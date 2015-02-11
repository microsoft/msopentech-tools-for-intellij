/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.interopbridges.tools.windowsazure.*;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.intellij.ui.AzureAbstractPanel;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;

import java.awt.*;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class RoleEndpointsPanel extends BaseConfigurable implements AzureAbstractPanel, SearchableConfigurable, Configurable.NoScroll {
    /**
     * End point range's minimum value.
     */
    private final static int RANGE_MIN = 1;
    /**
     * End point range's maximum value.
     */
    private final static int RANGE_MAX = 65535;

    private JPanel contentPane;
    private TableView<WindowsAzureEndpoint> tblEndpoints;
    private JPanel tablePanel;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private final WindowsAzureRole windowsAzureRole;
    private List<WindowsAzureEndpoint> listEndPoints;

    private ListTableModel<WindowsAzureEndpoint> myModel;


    public RoleEndpointsPanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole windowsAzureRole) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.windowsAzureRole = windowsAzureRole;
        try {
            //Get endpoints to be displayed in endpoints table
            listEndPoints = windowsAzureRole.getEndpoints();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
        }
        init();
    }

    private void init() {
        initializeModel();
        tblEndpoints.setRowHeight(ComboBoxTableCellEditor.INSTANCE.getComponent().getPreferredSize().height);
    }

    private void initializeModel() {
        myModel = new EndpointsTableModel(listEndPoints);
        tblEndpoints.setModelAndUpdateColumns(myModel);
        tblEndpoints.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void createUIComponents() {
        tblEndpoints = new TableView<WindowsAzureEndpoint>();
        tablePanel = ToolbarDecorator.createDecorator(tblEndpoints, null)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addEndpoint();
                    }
                }).setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        editEndpoint();
                    }
                }).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeEndpoint();
                    }
                }).setEditActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblEndpoints.getSelectedObject() != null;
                    }
                }).setRemoveActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblEndpoints.getSelectedObject() != null;
                    }
                }).disableUpDownActions().createPanel();
        tablePanel.setPreferredSize(new Dimension(-1, 200));
    }

    private void addEndpoint() {
        EndpointDialog endpointDialog = new EndpointDialog(windowsAzureRole, null);
        endpointDialog.show();
        if (endpointDialog.isOK()) {
            ((EndpointsTableModel) tblEndpoints.getModel()).fireTableDataChanged();
            setModified(true);
        }
    }

    /**
     * Listener method for edit button which opens a dialog
     * to edit an endpoint.
     */
    private void editEndpoint() {
        WindowsAzureEndpoint waEndpoint = tblEndpoints.getSelectedObject();
        /*
    	 * Check end point selected for modification
    	 * is associated with caching then give error
    	 * and does not allow to edit.
    	 */
        try {
            if (waEndpoint.isCachingEndPoint()) {
                PluginUtil.displayErrorDialog(message("cachDsblErTtl"), message("endPtEdtErMsg"));
            } else if (waEndpoint.isStickySessionEndpoint() && waEndpoint.isSSLEndpoint()) {
                PluginUtil.displayErrorDialog(message("sslSesDsbl"), message("sslSesAffMsg"));
            } else if (waEndpoint.isStickySessionEndpoint()) {
                PluginUtil.displayErrorDialog(message("sesAffDsblErTl"), message("sesAffMsg"));
            } else if (waEndpoint.isSSLEndpoint()) {
                PluginUtil.displayErrorDialog(message("sslDsblErTl"), message("sslMsg"));
            } else if (waEndpoint.isSSLRedirectEndPoint()) {
                PluginUtil.displayErrorDialog(message("sslDsblErTl"), message("sslMsg"));
            } else {
                EndpointDialog endpointDialog = new EndpointDialog(windowsAzureRole, waEndpoint);
                endpointDialog.show();
                if (endpointDialog.isOK()) {
                    ((EndpointsTableModel) tblEndpoints.getModel()).fireTableDataChanged();
                    setModified(true);
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), message("chEndPtErMsg"));
        }
    }

    /**
     * Listener method for remove button which
     * deletes the selected endpoint.
     */
    private void removeEndpoint() {
        WindowsAzureEndpoint waEndpoint = tblEndpoints.getSelectedObject();
        try {
            WindowsAzureEndpoint debugEndpt = windowsAzureRole.getDebuggingEndpoint();
            String dbgEndptName = "";
            if (debugEndpt != null) {
                dbgEndptName = debugEndpt.getName();
            }
            // delete the selected endpoint
                /*
                 * Check end point selected for removal
        		 * is associated with Caching then give error
        		 * and does not allow to remove.
        		 */
            if (waEndpoint.isCachingEndPoint()) {
                PluginUtil.displayErrorDialog(message("cachDsblErTtl"), message("endPtRmvErMsg"));
            }
                /*
        		 * Check end point selected for removal
        		 * is associated with Debugging.
        		 */
            else if (waEndpoint.getName().equalsIgnoreCase(dbgEndptName)) {
                StringBuilder msg = new StringBuilder(message("dlgEPDel"));
                msg.append(message("dlgEPDel1"));
                msg.append(message("dlgEPDel2"));
                int choice = Messages.showYesNoDialog(msg.toString(), message("dlgDelEndPt1"), Messages.getQuestionIcon());
                if (choice == Messages.YES) {
                    waEndpoint.delete();
                    setModified(true);
                    windowsAzureRole.setDebuggingEndpoint(null);
                }
            }
                /*
                 * Endpoint associated with both SSL
                 * and Session affinity
                 */
            else if (waEndpoint.isStickySessionEndpoint() && waEndpoint.isSSLEndpoint()) {
                int choice = Messages.showOkCancelDialog(message("bothDelMsg"), message("dlgDelEndPt1"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    setModified(true);
                    if (waEndpoint.getEndPointType().equals(WindowsAzureEndpointType.Input)) {
                        windowsAzureRole.setSessionAffinityInputEndpoint(null);
                        windowsAzureRole.setSslOffloading(null, null);
                        waEndpoint.delete();
                    } else {
                        windowsAzureRole.setSessionAffinityInputEndpoint(null);
                        windowsAzureRole.setSslOffloading(null, null);
                    }
                }
            }
                /*
        		 * Check end point selected for removal
        		 * is associated with Load balancing
        		 * i.e (HTTP session affinity).
        		 */
            else if (waEndpoint.isStickySessionEndpoint()) {
                int choice = Messages.showOkCancelDialog(message("ssnAffDelMsg"), message("dlgDelEndPt1"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    setModified(true);
                    if (waEndpoint.getEndPointType().equals(WindowsAzureEndpointType.Input)) {
                        windowsAzureRole.setSessionAffinityInputEndpoint(null);
                        waEndpoint.delete();
                    } else {
                        windowsAzureRole.setSessionAffinityInputEndpoint(null);
                    }
                }
            }
                /*
                 * Endpoint associated with SSL
                 */
            else if (waEndpoint.isSSLEndpoint()) {
                int choice = Messages.showOkCancelDialog(message("sslDelMsg"), message("dlgDelEndPt1"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    setModified(true);
                    if (waEndpoint.getEndPointType().equals(WindowsAzureEndpointType.Input)) {
                        windowsAzureRole.setSslOffloading(null, null);
                        waEndpoint.delete();
                    } else {
                        windowsAzureRole.setSslOffloading(null, null);
                    }
                }
            }
                /*
                 * Endpoint associated with SSL redirection.
                 */
            else if (waEndpoint.isSSLRedirectEndPoint()) {
                int choice = Messages.showOkCancelDialog(message("sslRedirectDelMsg"), message("dlgDelEndPt1"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    windowsAzureRole.deleteSslOffloadingRedirectionEndpoint();
                    setModified(true);
                }
            }
                /*
                 * Normal end point.
                 */
            else {
                int choice = Messages.showOkCancelDialog(message("dlgDelEndPt2"), message("dlgDelEndPt1"), Messages.getQuestionIcon());
                if (choice == Messages.OK) {
                    setModified(true);
                    waEndpoint.delete();
                }
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"), e);
        }
    }

    @Override
    public JComponent getPanel() {
        return contentPane;
    }

    @Override
    public String getDisplayName() {
        return message("cmhLblEndPts");
    }

    @Override
    public boolean doOKAction() {
        try {
            apply();
        } catch (ConfigurationException e) {
            PluginUtil.displayErrorDialogAndLog(e.getTitle(), e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "windows_azure_endpoint_page";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            if (isModified()) {
                waProjManager.save();
                setModified(false);
            }
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            throw new ConfigurationException(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"));
        }
    }

    @Override
    public void reset() {
        setModified(false);
    }

    @Override
    public void disposeUIResources() {
    }

    @NotNull
    @Override
    public String getId() {
        return getDisplayName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    private void setEndpointType(WindowsAzureEndpoint endpoint, WindowsAzureEndpointType type, String prvPort, String pubPort)
            throws WindowsAzureInvalidProjectOperationException {
        if (windowsAzureRole.isValidEndpoint(endpoint.getName(), type, prvPort, pubPort)) {
            endpoint.setPrivatePort(prvPort);
            setModified(true);
            endpoint.setEndPointType(type);
        } else {
            Messages.showInfoMessage(message("changeErr"), message("dlgTypeTitle"));
        }
    }


    private final ColumnInfo<WindowsAzureEndpoint, String> NAME = new ColumnInfo<WindowsAzureEndpoint, String>(message("dlgColName")) {
        public String valueOf(WindowsAzureEndpoint object) {
            return object.getName();
        }

        /**
         * Handles the modification of endpoint name.
         *
         * @param endpoint : the endpoint being modified.
         * @param endptName : new value for endpoint name.
         */
        @Override
        public void setValue(WindowsAzureEndpoint endpoint, String endptName) {
            // Validate endpoint name
        	/*
        	 * Check endpoint name contain
        	 * alphanumeric and underscore characters only.
        	 * Starts with alphabet.
        	 */
            if (WAEclipseHelperMethods.isAlphaNumericUnderscore(endptName)) {
                try {
                    boolean isValid = windowsAzureRole.isAvailableEndpointName(endptName, endpoint.getEndPointType());
                        /*
                		 * Check already used endpoint name is given.
                		 */
                    if (isValid || endptName.equalsIgnoreCase(endpoint.getName())) {
                        endpoint.setName(endptName);
                        setModified(true);
                    } else {
                        PluginUtil.displayErrorDialog(message("dlgInvdEdPtName1"), message("dlgInvdEdPtName2"));
                    }
                } catch (WindowsAzureInvalidProjectOperationException e) {
                    PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
                }
            } else {
                PluginUtil.displayErrorDialog(message("dlgInvdEdPtName1"), message("enPtAlphNuMsg"));
            }
        }
    };

    private final ColumnInfo<WindowsAzureEndpoint, WindowsAzureEndpointType> TYPE = new ColumnInfo<WindowsAzureEndpoint, WindowsAzureEndpointType>(message("dlgColType")) {
        public WindowsAzureEndpointType valueOf(WindowsAzureEndpoint object) {
            try {
                return object.getEndPointType();
            } catch (WindowsAzureInvalidProjectOperationException e) {
                //display error message if any exception occurs while
                //reading role data
                PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
                return null;
            }
        }

        @Override
        public TableCellEditor getEditor(final WindowsAzureEndpoint endpoint) {
            return ComboBoxTableCellEditor.INSTANCE;
        }

        /**
         * Handles the modification of endpoint type.
         *
         * @param endpoint : the endpoint being modified.
         * @param type     : new value for endpoint type.
         */
        @Override
        public void setValue(WindowsAzureEndpoint endpoint, WindowsAzureEndpointType type) {
            try {
                WindowsAzureEndpointType oldType = endpoint.getEndPointType();
                String endpointName = endpoint.getName();
                String prvPort = endpoint.getPrivatePort();
                String pubPort = endpoint.getPort();

                if (WindowsAzureEndpointType.Input == type) {
                    // User changed type to Input
                    /*
        		     * While changing type from Internal
        		     * if private port is in the form of range,
        		     * then assign minimum of that range
        		     * as a private port because private port
        		     * range is not valid for other types of endpoints.
        		     */
                    if (oldType.equals(WindowsAzureEndpointType.Internal)) {
                        if (prvPort.contains("-")) {
                            String[] portRange = prvPort.split("-");
                            prvPort = portRange[0];
                        }
                        pubPort = prvPort;
                    } else if (oldType.equals(WindowsAzureEndpointType.InstanceInput) && pubPort.contains("-")) {
                        String[] portRange = pubPort.split("-");
                        pubPort = portRange[0];
                    }
                    setEndpointType(endpoint, WindowsAzureEndpointType.Input, prvPort, pubPort);
                } else if (WindowsAzureEndpointType.Internal == type) {
                    // User changed type to Internal
                    WindowsAzureEndpoint debugEndpt = windowsAzureRole.getDebuggingEndpoint();
                    String dbgEndptName = "";
                    if (debugEndpt != null) {
                        dbgEndptName = debugEndpt.getName();
                    }
                    Boolean disableDebug = false;
                    if (endpointName.equalsIgnoreCase(dbgEndptName)) {
                        int choice = Messages.showYesNoDialog(String.format("%s%s%s", message("dlgEPDel"), message("dlgEPChangeType"),
                                message("dlgEPDel2")), message("dlgTypeTitle"), Messages.getQuestionIcon());
                        if (choice == Messages.YES) {
                            disableDebug = true;
                        }
                    }
                    setEndpointType(endpoint, WindowsAzureEndpointType.Internal, prvPort, "");
                    if (disableDebug) {
                        windowsAzureRole.setDebuggingEndpoint(null);
                    }
                } else if (WindowsAzureEndpointType.InstanceInput == type) {
                    // User changed type to InstanceInput
                    Boolean changeType = true;
                    if (oldType.equals(WindowsAzureEndpointType.Internal)) {
                        if (prvPort.contains("-")) {
                            String[] portRange = prvPort.split("-");
                            prvPort = portRange[0];
                        }
                        pubPort = prvPort;
                    } else if (oldType.equals(WindowsAzureEndpointType.Input) && prvPort == null) {
                        if (windowsAzureRole.isValidEndpoint(endpointName, WindowsAzureEndpointType.InstanceInput, pubPort, pubPort)) {
                            prvPort = pubPort;
                        } else {
                            changeType = false;
                            PluginUtil.displayWarningDialog(message("dlgTypeTitle"), String.format(message("inpInstTypeMsg"), pubPort));
                        }
                    }
                    if (changeType) {
                        setEndpointType(endpoint, WindowsAzureEndpointType.InstanceInput, prvPort, pubPort);
                    }
                }
            } catch (WindowsAzureInvalidProjectOperationException ex) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            }
        }
    };

    private final ColumnInfo<WindowsAzureEndpoint, String> PUBLIC_PORT = new ColumnInfo<WindowsAzureEndpoint, String>(message("dlgColPubPort")) {
        public String valueOf(WindowsAzureEndpoint object) {
            return object.getPort();
        }

        /**
         * Handles the modification of endpoint's public port.
         *
         * @param endpoint : the endpoint being modified.
         * @param port : new value for endpoint's public port.
         */
        @Override
        public void setValue(WindowsAzureEndpoint endpoint, String port) {
            try {
         	    /*
        	     * Check only one '-' is present,
        	     * while specifying range for Instance end point.
        	     * If end point is Internal or Input then,
        	     * it will not satisfy if condition
        	     * and dash count will be zero.
        	     */
                int dashCnt = 0;
                if (endpoint.getEndPointType().equals(WindowsAzureEndpointType.InstanceInput)) {
                    dashCnt = StringUtils.countMatches(port.trim(), "-");
                    if (dashCnt > 1) {
                        PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dashErrMsg"));
                    }
                }
                if (dashCnt <= 1) {
                    // Check for valid range 1 to 65535
                    Boolean isPortValid = true;
                    try {
        			/*
        			 * If public port contains '-'
        			 * then split string and
        			 * get two integer values out of it.
        			 * else directly check for value.
        			 */
                        if (dashCnt == 1) {
                            String[] range = port.split("-");
                            int rngStart = Integer.parseInt(range[0]);
                            int rngEnd = Integer.parseInt(range[1]);
                            if (!(rngStart >= RANGE_MIN && rngStart <= RANGE_MAX && rngEnd >= RANGE_MIN && rngEnd <= RANGE_MAX)) {
                                isPortValid = false;
                            }
                        } else {
                            int portNum = Integer.parseInt(port.toString());
                            if (!(portNum >= RANGE_MIN && portNum <= RANGE_MAX)) {
                                isPortValid = false;
                            }
                        }
                    } catch (NumberFormatException e) {
                        isPortValid = false;
                    }
                    if (isPortValid) {
                        // Validate port
                        boolean isValid = windowsAzureRole.isValidEndpoint(endpoint.getName(), endpoint.getEndPointType(), endpoint.getPrivatePort(), port);
                        if (isValid) {
                            endpoint.setPort(port);
                            setModified(true);
                        } else {
                            PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dlgPortInUse"));
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("rngErrMsg"));
                    }
                }
            } catch (WindowsAzureInvalidProjectOperationException ex) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            }
        }
    };

    private final ColumnInfo<WindowsAzureEndpoint, String> PRIVATE_PORT = new ColumnInfo<WindowsAzureEndpoint, String>(message("dlgColPrivatePort")) {
        public String valueOf(WindowsAzureEndpoint object) {
            return object.getPrivatePort();
        }

        /**
         * Handles the modification of endpoint's private port.
         *
         * @param endpoint : the endpoint being modified.
         * @param port : new value for endpoint's private port.
         */
        @Override
        public void setValue(WindowsAzureEndpoint endpoint, String port) {
            try {
        	    /*
        	     * Check only one '-' is present,
        	     * while specifying range for Internal end point.
        	     * If end point is Instance or Input then,
        	     * it will not satisfy if condition
        	     * and dash count will be zero.
        	     */
                int dashCnt = 0;
                if (endpoint.getEndPointType().equals(WindowsAzureEndpointType.Internal)) {
                    dashCnt = StringUtils.countMatches(port.toString().trim(), "-");
                    if (dashCnt > 1) {
                        PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dashErrMsg"));
                    }
                }
                if (dashCnt <= 1) {
                    // Check for valid range 1 to 65535
                    Boolean isPortValid = true;
                    try {
        			/*
        			 * If public port contains '-'
        			 * then split string and
        			 * get two integer values out of it.
        			 * else directly check for value.
        			 */
                        if (dashCnt == 1) {
                            String[] range = port.split("-");
                            int rngStart = Integer.parseInt(range[0]);
                            int rngEnd = Integer.parseInt(range[1]);
                            if (!(rngStart >= RANGE_MIN && rngStart <= RANGE_MAX && rngEnd >= RANGE_MIN && rngEnd <= RANGE_MAX)) {
                                isPortValid = false;
                            }
                        } else {
                            // no dash
                            if (!((port.isEmpty() || port.equalsIgnoreCase("*")) && (endpoint.getEndPointType().equals(WindowsAzureEndpointType.Internal)
                                    || endpoint.getEndPointType().equals(WindowsAzureEndpointType.Input)))) {
                                int portNum = Integer.parseInt(port);
                                if (!(portNum >= RANGE_MIN && portNum <= RANGE_MAX)) {
                                    isPortValid = false;
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        isPortValid = false;
                    }
                    if (isPortValid) {
                        // Validate port
                        if (port.isEmpty() || port.equalsIgnoreCase("*")) {
                            port = null;
                        }
                        boolean isValid = windowsAzureRole.isValidEndpoint(endpoint.getName(), endpoint.getEndPointType(), port, endpoint.getPort());
                        if (isValid) {
                            boolean canChange = true;
                            boolean isDebugEnabled = false;
                            boolean isSuspended = false;
                            WindowsAzureEndpoint endPt = windowsAzureRole.getDebuggingEndpoint();
        				    /*
        				     * check if the endpoint is associated with debug,
        				     * if yes then set isDebugEnabled to true and
        				     * store the suspended mode value. Disable debug endpoint
        				     * and then enable it with the modified endpoint.
        				     */
                            if (endPt != null && endpoint.getName().equalsIgnoreCase(endPt.getName())) {
                                if (port == null) {
                                    PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dbgPort"));
                                    canChange = false;
                                } else {
                                    isSuspended = windowsAzureRole.getStartSuspended();
                                    windowsAzureRole.setDebuggingEndpoint(null);
                                    setModified(true);
                                    isDebugEnabled = true;
                                }
                            }
                            if (canChange) {
                                endpoint.setPrivatePort(port);
                                setModified(true);
                                if (isDebugEnabled) {
                                    windowsAzureRole.setDebuggingEndpoint(endpoint);
                                    windowsAzureRole.setStartSuspended(isSuspended);
                                }
                            }
                        } else {
                            PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("dlgPortInUse"));
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("dlgInvldPort"), message("rngErrMsg"));
                    }
                }
            } catch (WindowsAzureInvalidProjectOperationException ex) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            }
        }
    };

    private class EndpointsTableModel extends ListTableModel<WindowsAzureEndpoint> {

        private EndpointsTableModel(List<WindowsAzureEndpoint> listEndPoints) {
            super(new ColumnInfo[]{NAME, TYPE, PUBLIC_PORT, PRIVATE_PORT}, listEndPoints, 0);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
}
