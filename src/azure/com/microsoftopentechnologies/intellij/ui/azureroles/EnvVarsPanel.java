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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import static java.util.Map.Entry;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class EnvVarsPanel extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private JPanel contentPane;
    private JPanel tablePanel;
    private TableView<Map.Entry<String, String>> tblEnvVariables;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private Map<String, String> mapEnvVar;

    public EnvVarsPanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole windowsAzureRole) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.waRole = windowsAzureRole;
        try {
            mapEnvVar = windowsAzureRole.getRuntimeEnv();
            EnvVarsTableModel envVarsTableModel = new EnvVarsTableModel(new ArrayList<Entry<String, String>>(mapEnvVar.entrySet()));
            tblEnvVariables.setModelAndUpdateColumns(envVarsTableModel);
            tblEnvVariables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("rolsErr"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
        }
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

    @Nls
    @Override
    public String getDisplayName() {
        return message("cmhLblEnvVars");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            waProjManager.save();
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module) + File.separator + message("resCLPkgXML")).refresh(true, false);
            setModified(false);
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

    private final ColumnInfo<Entry<String, String>, String> NAME = new ColumnInfo<Entry<String, String>, String>(message("evColName")) {
        public String valueOf(Entry<String, String> object) {
            return object.getKey();
        }

        @Override
        public void setValue(Entry<String, String> entry, String modifiedVal) {
            try {
                String modifiedName = modifiedVal.toString();
                boolean isValidName = true;
                for (Iterator<String> iterator = mapEnvVar.keySet().iterator(); iterator.hasNext();) {
                    String key = iterator.next();
                    if (key.equalsIgnoreCase(modifiedName)) {
                        isValidName = false;
                        break;
                    }
                }
                if (modifiedName.isEmpty()) {
                    PluginUtil.displayErrorDialog(message("evNameEmptyTtl"), message("evNameEmptyMsg"));
                } else if (!isValidName && !modifiedName.equalsIgnoreCase(entry.getKey()) || waRole.getLsEnv().contains(modifiedName)) {
                    PluginUtil.displayErrorDialog(message("evInUseTitle"), message("evInUseMsg"));
                } else {
                    String name = modifiedName.trim();
                    name = name.replaceAll("[\\s]+", "_");
                    waRole.renameRuntimeEnv(entry.getKey(), name);
                    setModified(true);
                }
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            }
        }
    };

    private final ColumnInfo<Entry<String, String>, String> VALUE = new ColumnInfo<Entry<String, String>, String>(message("evColValue")) {
        public String valueOf(Entry<String, String> object) {
            return object.getValue();
        }

        @Override
        public void setValue(Entry<String, String> entry, String modifiedVal) {
            try {
                waRole.setRuntimeEnv(entry.getKey(), modifiedVal.toString().trim());
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), e);
            }
        }
    };

    /**
     * Listener for add button, which launches a dialog to add
     * an environment variable.
     */
    protected void addBtnListener() {
        try {
            EnvVarDialog dialog = new EnvVarDialog(mapEnvVar, waRole, null, false);
            dialog.show();
            if (dialog.isOK()) {
                setModified(true);
                String newVarName = dialog.getNewVarName();
                for (Entry<String, String> var : mapEnvVar.entrySet()) {
                    if (var.getKey().equals(newVarName)) {
                        tblEnvVariables.getListTableModel().addRow(var);
                    }
                }
            }
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("evLaunchErrTtl"), message("evErrLaunchMsg1") + message("evErrLaunchMsg2"), ex);
        }
    }

    /**
     * Listener for edit button, which launches a dialog to edit
     * an environment variable.
     */
    @SuppressWarnings("unchecked")
    protected void editBtnListener() {
        try {
            Entry<String, String> mapEntry = tblEnvVariables.getSelectedObject();
            EnvVarDialog dialog = new EnvVarDialog(mapEnvVar, waRole, mapEntry.getKey(), true);
            dialog.show();
            if (dialog.isOK()) {
                setModified(true);
            }
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(message("evLaunchErrTtl"), message("evErrLaunchMsg1") + message("evErrLaunchMsg2"), ex);
        }
    }

    /**
     * Listener for remove button, which removes the
     * environment variable from the role.
     */
    @SuppressWarnings("unchecked")
    protected void removeBtnListener() {
            try {
                Entry<String, String> mapEntry = tblEnvVariables.getSelectedObject();
                // Check environment variable is associated with component
                if (waRole.getIsEnvPreconfigured(mapEntry.getKey())) {
                    PluginUtil.displayErrorDialog(message("jdkDsblErrTtl"), message("envJdkDslErrMsg"));
                } else {
                    int choice = Messages.showYesNoDialog(message("evRemoveMsg"), message("evRemoveTtl"), Messages.getQuestionIcon());
                    if (choice == Messages.YES) {
                        /*
                         * to delete call rename with
                         * newName(second param) as empty
                         */
                        waRole.renameRuntimeEnv(mapEntry.getKey(), "");
                        setModified(true);
                    }
                }
            } catch (Exception ex) {
                PluginUtil.displayErrorDialogAndLog(message("adRolErrTitle"), message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), ex);
            }
    }

    private void createUIComponents() {
        tblEnvVariables = new TableView<Map.Entry<String, String>>();
        tablePanel = ToolbarDecorator.createDecorator(tblEnvVariables, null)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addBtnListener();
                    }
                }).setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton anActionButton) {
                        editBtnListener();
                    }
                }).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeBtnListener();
                    }
                }).setEditActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblEnvVariables.getSelectedObject() != null;
                    }
                }).setRemoveActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblEnvVariables.getSelectedObject() != null;
                    }
                }).disableUpDownActions().createPanel();
        tablePanel.setPreferredSize(new Dimension(-1, 200));
    }

    private class EnvVarsTableModel extends ListTableModel<Entry<String, String>> {
        private EnvVarsTableModel(java.util.List<Entry<String, String>> envVars) {
            super(new ColumnInfo[]{NAME, VALUE}, envVars, 0);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
}
