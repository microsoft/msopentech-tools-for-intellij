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
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.interopbridges.tools.windowsazure.*;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.AzureWizardModel;
import com.microsoftopentechnologies.intellij.ui.StorageAccountPanel;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.ui.util.JdkSrvConfig;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.roleoperations.WARCachingUtilMethods;
import com.microsoftopentechnologies.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.storageregistry.StorageRegistryUtilMethods;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;

import java.awt.event.*;
import java.util.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class CachingPanel extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private static final String DASH_AUTO = "-auto";
    /**
     * Default cache memory size.
     */
    private final static int CACH_DFLTVAL = 30;

    private enum BACKUP_OPTIONS {
        Yes, No
    }

    private enum EXPIRATION_TYPE {
        NeverExpires, Absolute, SlidingWindow
    }

    private JPanel contentPane;
    private JCheckBox cacheCheck;
    private JTextPane enblCacheNote;
    private JPanel tablePanel;
    private JTextField txtHostName;
    private JComboBox comboStrgAcc;
    private JSlider cacheScale;
    private JXHyperlink accLink;
    private JTextField txtCache;
    private JLabel hostNameLbl;
    private JLabel scaleLbl;
    private JLabel explNtLbl;
    private JLabel crdntlLbl;
    private TableView<WindowsAzureNamedCache> tblCache;

    private Module module;
    private WindowsAzureProjectManager waProjManager;
    private WindowsAzureRole waRole;
    private Map<String, WindowsAzureNamedCache> mapCache;
    /**
     * Boolean field to track whether
     * cache memory size is set to valid value or not.
     */
    private Boolean isCachPerValid = true;

    public CachingPanel(Module module, WindowsAzureProjectManager waProjManager, WindowsAzureRole waRole) {
        this.module = module;
        this.waProjManager = waProjManager;
        this.waRole = waRole;
        /* Check Cache memory setting is present or not
         * and if enabled show appropriate values on property page */
        int cachePercent = 0;
        try {
            cachePercent = waRole.getCacheMemoryPercent();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("getCachMemErMsg"), e);
        }
        if (cachePercent > 0) {
            setEnableCaching(true);
            cacheCheck.setSelected(true);
            txtCache.setText(String.format("%s%s", cachePercent, "%"));
            cacheScale.setValue(cachePercent);
            txtHostName.setText(String.format("%s%s%s", message("dlgDbgLclHost"), "_", waRole.getName().toLowerCase()));
            try {
                String accKey = waRole.getCacheStorageAccountKey();
                UIUtils.populateStrgNameAsPerKey(accKey, comboStrgAcc);
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("getStrAccErrMsg"), e);
            }

        } else {
            cacheCheck.setSelected(false);
            setEnableCaching(false);
        }
        try {
            mapCache = waRole.getNamedCaches();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachGetErMsg"), e);
            mapCache = new HashMap<String, WindowsAzureNamedCache>();
        }
        init();
    }

    private void init() {
        Messages.configureMessagePaneUi(enblCacheNote, message("enblCachNtLbl"));
        cacheCheck.addItemListener(createCacheCheckListener());
        cacheScale.setPaintTicks(true);
        cacheScale.setMajorTickSpacing(10);
        cacheScale.addChangeListener(createCacheScaleListener());
        txtCache.addFocusListener(createTxtCacheListener());
        accLink.setAction(createAccLinkAction());

        CacheTableModel myModel = new CacheTableModel(new ArrayList<WindowsAzureNamedCache>(mapCache.values()));
        tblCache.setModelAndUpdateColumns(myModel);
        tblCache.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCache.setRowHeight(ComboBoxTableCellEditor.INSTANCE.getComponent().getPreferredSize().height);
    }

    private ChangeListener createCacheScaleListener() {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                int value = source.getValue();
                if (!source.getValueIsAdjusting()) { //done adjusting
                    txtCache.setText(String.format("%s%s", value, "%"));
                    setCachPerMem(value);
                } else { //value is adjusting; just set the text
                    txtCache.setText(String.valueOf(value));
                }
            }
        };
    }

    private FocusListener createTxtCacheListener() {
        /**
         * Adjusts scales's position, according to value
         * entered in the synchronized cache text box.
         */
        return new FocusListener() {
            private String oldTxt = "";

            @Override
            public void focusGained(FocusEvent e) {
                oldTxt = txtCache.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!txtCache.getText().equals(oldTxt)) {
                    int cacheVal = 0;
                    Boolean isNumber = true;
                    try {
						/*
						 * As '%' is allowed in user's input,
						 * check if '%' is present already
						 * then ignore '%' and take only numeric value
						 */
                        if (txtCache.getText().endsWith("%")) {
                            cacheVal = Integer.parseInt(txtCache.getText().trim().substring(0, txtCache.getText().length() - 1));
                        } else {
                            cacheVal = Integer.parseInt(txtCache.getText().trim());
                            txtCache.setText(String.format("%s%s", cacheVal, "%"));
                        }
                    } catch (NumberFormatException ex) {
						/*
						 * User has given alphabet
						 * or special character as input
						 * for cache memory size.
						 */
                        isNumber = false;
                    }
					/*
					 * Check cache memory size input
					 * is numeric and has value > 0
					 */
                    if (isNumber && cacheVal >= 10 && cacheVal <= 100) {
                        setCachPerMem(cacheVal);
                        cacheScale.setValue(cacheVal);
                    } else {
						/*
						 * User has given zero
						 * or negative number as input.
						 */
                        isCachPerValid = false;
                    }
                }
            }
        };
    }

    private ItemListener createCacheCheckListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setModified(true);
                if (cacheCheck.isSelected()) {
                    if (message("txtExtraSmallVM").equals(waRole.getVMSize())) {
                        PluginUtil.displayErrorDialog(message("cacheConfTitle"), message("cacheConfErrMsg"));
                        cacheCheck.setSelected(false);
                        return;
                    }
                    setEnableCaching(true);
					/* Set cache memory percent
					 * to default value 30
					 */
                    setCachPerMem(CACH_DFLTVAL);
                    cacheScale.setValue(CACH_DFLTVAL);
                    txtCache.setText(String.format("%s%s", CACH_DFLTVAL, "%"));
                    txtHostName.setText(String.format("%s%s%s", message("dlgDbgLclHost"), "_", waRole.getName().toLowerCase()));
                    setName(DASH_AUTO);
                } else {
                    setEnableCaching(false);
					/* Set cache memory percent to 0
					 *  to disable cache.
					 *  Also set storage account name
					 *  and key to empty.
					 */
                    setCachPerMem(0);
                    setName("");
                    setKey("");
                    setBlobUrl("");
                }
                tblCache.getListTableModel().setItems(new ArrayList<WindowsAzureNamedCache>(mapCache.values()));
            }
        };
    }

    private Action createAccLinkAction() {
        return new AbstractAction("Accounts...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DefaultDialogWrapper storageAccountDialog = new DefaultDialogWrapper(module.getProject(), new StorageAccountPanel(module.getProject()));
                storageAccountDialog.show();
                /*
                 * Update data in every case.
		         * No need to check which button (OK/Cancel)
		         * has been pressed as change is permanent
		         * even though user presses cancel
		         * according to functionality.
		        */
                /*
                 * store old value which was selected
		         * previously so that we can populate
		         * the same later.
		         */
                String cmbName = (String) comboStrgAcc.getSelectedItem();
                String accPgName = storageAccountDialog.getSelectedValue();
                String finalNameToSet;
                /*
			     * If row selected on preference page.
			     * set combo box to it always.
			     * Else keep combo box's previous value
			     * as it is.
			     */
                if (!(JdkSrvConfig.NONE_TXT.equals(accPgName) || JdkSrvConfig.AUTO_TXT.equals(accPgName))) {
                    finalNameToSet = accPgName;
                } else {
                    finalNameToSet = cmbName;
                }
                // update storage account combo box
                JdkSrvConfig.populateStrgAccComboBox(finalNameToSet, comboStrgAcc, null, JdkSrvConfig.AUTO_TXT.equals(finalNameToSet));
            }
        };
    }

    /**
     * Method sets cache name and key
     * as per storage account combo box value.
     */
    private void setCacheNameKey() {
        String key = AzureWizardModel.getAccessKey(comboStrgAcc);
        String url = JdkSrvConfig.getBlobEndpointUrl(comboStrgAcc);
        if (key.isEmpty()) {
            // auto is selected
            setName(DASH_AUTO);
        } else {
            String name = StorageAccountRegistry.getStrgList().get(StorageRegistryUtilMethods.getStrgAccIndexAsPerKey(key)).getStrgName();
            setName(name);
        }
        setKey(key);
        setBlobUrl(url);
    }

    /**
     * Method sets azure role's
     * cache storage account key.
     *
     * @param key
     */
    private void setKey(String key) {
        try {
            waRole.setCacheStorageAccountKey(key);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("setAccKyErrMsg"), e);
        }
    }

    /**
     * Method sets azure role's
     * cache storage account name.
     *
     * @param name
     */
    private void setName(String name) {
        try {
            waRole.setCacheStorageAccountName(name);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("setAccNmErrMsg"), e);
        }
    }

    /**
     * Method sets azure role's
     * cache storage account blob endpoint url.
     *
     * @param url
     */
    private void setBlobUrl(String url) {
        try {
            waRole.setCacheStorageAccountUrl(url);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("setAccUrlErrMsg"), e);
        }
    }

    /**
     * Method enables or disables
     * UI components on caching page.
     *
     * @param status
     */
    private void setEnableCaching(boolean status) {
        enblCacheNote.setEnabled(status);
        scaleLbl.setEnabled(status);
        cacheScale.setEnabled(status);
        txtCache.setEnabled(status);
        tblCache.setEnabled(status);
        explNtLbl.setEnabled(status);
        txtHostName.setEnabled(status);
        hostNameLbl.setEnabled(status);
        comboStrgAcc.setEnabled(status);
        crdntlLbl.setEnabled(status);
        if (status) {
            JdkSrvConfig.populateStrgAccComboBox("", comboStrgAcc, null, true);
        } else {
            cacheScale.setValue(cacheScale.getMinimum());
            txtCache.setText("");
            comboStrgAcc.removeAll();
            txtHostName.setText("");
        }
    }

    private void setCachPerMem(int cachVal) {
        try {
            waRole.setCacheMemoryPercent(cachVal);
            isCachPerValid = true;
//            setErrorMessage(null);
        } catch (Exception e) {
			/*
			 * User has given input
			 * cachVal < 0 or cachVal > 100
			 */
            isCachPerValid = false;
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
        return message("cmhLblCach");
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
        boolean okToProceed = false;
        // Check caching is enabled
        if (cacheCheck.isSelected()) {
			/* Check cache memory size
			 * is set to valid value or not
			 */
            if (isCachPerValid) {
                okToProceed = true;
            } else {
                PluginUtil.displayErrorDialog(message("cachPerErrTtl"), message("cachPerErrMsg"));
            }
        } else {
            okToProceed = true;
        }
        if (okToProceed) {
            try {
                waProjManager.save();
                setModified(false);
                LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
            } catch (WindowsAzureInvalidProjectOperationException e) {
                AzurePlugin.log(message("adRolErrMsgBox1") + message("adRolErrMsgBox2", e));
                throw new ConfigurationException(message("adRolErrMsgBox1") + message("adRolErrMsgBox2"), message("adRolErrTitle"));
            }
        }
    }

    @Override
    public void reset() {
        setModified(false);
    }

    @Override
    public void disposeUIResources() {
    }

    /**
     * Listener method for add button which opens a dialog
     * to add new cache entry in cache table.
     */
    protected void addBtnListener() {
        CacheDialog dialog = new CacheDialog(waRole, mapCache, null, false);
        dialog.show();
        if (dialog.isOK()) {
            setModified(true);
            String name = dialog.getCacheName();
            tblCache.getListTableModel().addRow(mapCache.get(name));
        }
    }

    /**
     * Listener method for edit button which opens a dialog
     * to edit cache entry.
     */
    protected void editBtnListener() {
        WindowsAzureNamedCache cachEntry = tblCache.getSelectedObject();
        CacheDialog dialog = new CacheDialog(waRole, mapCache, cachEntry.getName(), true);
        dialog.show();
        if (dialog.isOK()) {
            ((CacheTableModel) tblCache.getModel()).fireTableDataChanged();
            setModified(true);
        }
    }

    /**
     * Listener method for remove button which
     * deletes the selected cache entry.
     */
    protected void removeBtnListener() {
        try {
            int choice = Messages.showYesNoDialog(message("cachRmvMsg"), message("cachRmvTtl"), Messages.getQuestionIcon());
            if (choice == Messages.YES) {
                WindowsAzureNamedCache cachToDel = tblCache.getSelectedObject();
                cachToDel.delete();
                tblCache.getListTableModel().setItems(new ArrayList<WindowsAzureNamedCache>(mapCache.values()));
                setModified(true);
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachDelErMsg"), e);
        }
    }

    private void createUIComponents() {
        tblCache = new TableView<WindowsAzureNamedCache>();
        tablePanel = ToolbarDecorator.createDecorator(tblCache, null)
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
                        return tblCache.getSelectedObject() != null;
                    }
                }).setRemoveActionUpdater(new AnActionButtonUpdater() {
                    @Override
                    public boolean isEnabled(AnActionEvent e) {
                        return tblCache.getSelectedObject() != null;
                    }
                }).disableUpDownActions().createPanel();
    }

    private final ColumnInfo<WindowsAzureNamedCache, String> NAME = new ColumnInfo<WindowsAzureNamedCache, String>(message("colChName")) {
        public String valueOf(WindowsAzureNamedCache object) {
            return object.getName();
        }

        @Override
        public void setValue(WindowsAzureNamedCache cache, String modifiedVal) {
            try {
                WARCachingUtilMethods.modifyCacheName(cache, modifiedVal, mapCache);
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("cachNameErrTtl"), e.getMessage());
            }
        }
    };

    private final ColumnInfo<WindowsAzureNamedCache, BACKUP_OPTIONS> HIGH_AVAILABILITY =
            new ColumnInfo<WindowsAzureNamedCache, BACKUP_OPTIONS>(message("colBkps")) {
        public BACKUP_OPTIONS valueOf(WindowsAzureNamedCache object) {
            if (object.getBackups()) {
                return BACKUP_OPTIONS.Yes;
            } else {
                return BACKUP_OPTIONS.No;
            }
        }

        @Override
        public TableCellEditor getEditor(final WindowsAzureNamedCache cache) {
            return ComboBoxTableCellEditor.INSTANCE;
        }

        @Override
        public void setValue(WindowsAzureNamedCache cache, BACKUP_OPTIONS modifiedVal) {
            try {
                if (modifiedVal.toString().equals(message("cachBckYes"))) {
				/*
				 * If user selects backup option
				 * then check virtual machine instances > 2
				 * otherwise give warning
				 */
                    int vmCnt = 0;
                    try {
                        vmCnt = Integer.parseInt(waRole.getInstances());
                    } catch (Exception e) {
                        PluginUtil.displayErrorDialogAndLog(message("genErrTitle"), message("vmInstGetErMsg"), e);
                    }
                    if (vmCnt < 2) {
					/*
					 * If virtual machine instances < 2
					 * then set back up to false.
					 */
                        cache.setBackups(false);
                        Messages.showWarningDialog(message("backWarnMsg"), message("backWarnTtl"));
                    } else {
                        cache.setBackups(true);
                    }
                } else if (modifiedVal.toString().equals(message("cachBckNo"))) {
                    cache.setBackups(false);
                }
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachSetErrMsg"), e);
            }
        }
    };

    private final ColumnInfo<WindowsAzureNamedCache, EXPIRATION_TYPE> EXPIRATION = new ColumnInfo<WindowsAzureNamedCache, EXPIRATION_TYPE>(message("colExp")) {
        public EXPIRATION_TYPE valueOf(WindowsAzureNamedCache cache) {
            if (cache.getExpirationPolicy().equals(WindowsAzureCacheExpirationPolicy.NEVER_EXPIRES)) {
                return EXPIRATION_TYPE.NeverExpires;
            } else if (cache.getExpirationPolicy().equals(WindowsAzureCacheExpirationPolicy.ABSOLUTE)) {
                return EXPIRATION_TYPE.Absolute;
            } else {
                return EXPIRATION_TYPE.SlidingWindow;
            }
        }

        @Override
        public TableCellEditor getEditor(final WindowsAzureNamedCache cache) {
            return ComboBoxTableCellEditor.INSTANCE;
        }

        @Override
        public void setValue(WindowsAzureNamedCache cache, EXPIRATION_TYPE modifiedVal) {
            try {
                WARCachingUtilMethods.modifyExpirationPol(cache, String.valueOf(modifiedVal.ordinal()));
                setModified(true);
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("cachNameErrTtl"), e.getMessage());
            }
        }
    };

    private final ColumnInfo<WindowsAzureNamedCache, String> MINUTES_TO_LIVE = new ColumnInfo<WindowsAzureNamedCache, String>(message("colMinToLive")) {
        public String valueOf(WindowsAzureNamedCache cache) {
            /*
			 * If expiration policy is NEVER_EXPIRES
			 * then show N/A for minutes to Live column
			 */
            if (cache.getExpirationPolicy().equals(WindowsAzureCacheExpirationPolicy.NEVER_EXPIRES)) {
                return message("dlgDbgNA");
            } else {
                return Integer.toString(cache.getMinutesToLive());
            }
        }

        @Override
        public void setValue(WindowsAzureNamedCache cache, String modifiedVal) {
            try {
                Boolean isVallidMtl = WARCachingUtilMethods.validateMtl(modifiedVal);
                if (isVallidMtl) {
                    cache.setMinutesToLive(Integer.parseInt(modifiedVal));
                    setModified(true);
                } else {
                    PluginUtil.displayErrorDialog(message("cachMtlErrTtl"), message("cachMtlErrMsg"));
                }
            } catch (Exception ex) {
                PluginUtil.displayErrorDialog(message("cachMtlErrTtl"), message("cachMtlErrMsg"));
            }
        }
    };

    private final ColumnInfo<WindowsAzureNamedCache, String> PORT = new ColumnInfo<WindowsAzureNamedCache, String>(message("colPort")) {
        public String valueOf(WindowsAzureNamedCache cache) {
            try {
                return cache.getEndpoint().getPrivatePort();
            } catch (WindowsAzureInvalidProjectOperationException e) {
                PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachGetErMsg"), e);
                return "";
            }
        }

        @Override
        public void setValue(WindowsAzureNamedCache cache, String modifiedVal) {
            try {
                WARCachingUtilMethods.modifyPort(cache, modifiedVal, waRole);
            } catch (Exception ex) {
                PluginUtil.displayErrorDialog(message("cachPortErrTtl"), ex.getMessage());
            }
        }
    };

    private class CacheTableModel extends ListTableModel<WindowsAzureNamedCache> {
        private CacheTableModel(java.util.List<WindowsAzureNamedCache> namedCaches) {
            super(new ColumnInfo[]{NAME, HIGH_AVAILABILITY, EXPIRATION, MINUTES_TO_LIVE, PORT}, namedCaches);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
}
