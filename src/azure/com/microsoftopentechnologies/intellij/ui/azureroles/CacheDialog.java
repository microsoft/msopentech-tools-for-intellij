package com.microsoftopentechnologies.intellij.ui.azureroles;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TitlePanel;
import com.interopbridges.tools.windowsazure.WindowsAzureCacheExpirationPolicy;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureNamedCache;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.azurecommons.exception.AzureCommonsException;
import com.microsoftopentechnologies.azurecommons.roleoperations.CacheDialogUtilMethods;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class CacheDialog extends DialogWrapper {
    /**
     * Cache expiration policies.
     */
    private static String[] arrType = {message("expPolNvrExp"), message("expPolAbs"), message("expPolSlWn")};

    private JPanel contentPane;
    private JTextField txtCacheName;
    private JTextField txtPortNum;
    private JComboBox comboExpPolicy;
    private JTextField txtMinLive;
    private JCheckBox backupCheck;

    private WindowsAzureRole waRole;
    private Map<String, WindowsAzureNamedCache> cacheMap;
    private boolean isEdit;
    private String cacheName;

    public CacheDialog(WindowsAzureRole waRole, Map<String, WindowsAzureNamedCache> cacheMap, String key, boolean isEdit) {
        super(true);
        this.waRole = waRole;
        this.cacheMap = cacheMap;
        this.cacheName = key;
        this.isEdit = isEdit;
        setTitle(message("cacheTtl"));
        if (isEdit) {
            populateData();
        }
        init();
    }

    protected void init() {
        comboExpPolicy.setModel(new DefaultComboBoxModel(arrType));
        comboExpPolicy.setSelectedItem(arrType[1]);
        comboExpPolicy.addItemListener(createComboExpPolicyListener());
        backupCheck.addItemListener(createBackupCheckListener());
        super.init();
    }

    /**
     * Populates the cache name and value text fields with the corresponding
     * attributes of named cache resource selected for editing.
     */
    private void populateData() {
        try {
            WindowsAzureNamedCache cache = cacheMap.get(cacheName);
            txtCacheName.setText(cache.getName());
            /*
			 * Disable cache name text box if default cache
			 * is selected for editing
			 * as renaming default cache is not allowed.
			 */
            if (cache.getName().equalsIgnoreCase(message("dfltCachName"))) {
                txtCacheName.setEnabled(false);
            }
            txtPortNum.setText(cache.getEndpoint().getPrivatePort());
            comboExpPolicy.setSelectedItem(CacheDialogUtilMethods.getExpPolStr(cache));
			/*
			 * Check if expiration policy is NEVER_EXPIRES
			 * then disable minutes to live text box
			 * and set value to N/A
			 */
            if (cache.getExpirationPolicy().equals(WindowsAzureCacheExpirationPolicy.NEVER_EXPIRES)) {
                txtMinLive.setText(message("dlgDbgNA"));
                txtMinLive.setEnabled(false);
            } else {
                txtMinLive.setText(Integer.toString(cache.getMinutesToLive()));
            }
            backupCheck.setSelected(cache.getBackups());
        } catch (WindowsAzureInvalidProjectOperationException e) {
            PluginUtil.displayErrorDialogAndLog(message("cachErrTtl"), message("cachGetErMsg"), e);
        }
    }

    private ItemListener createComboExpPolicyListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                /*
				 * Check if expiration policy is NEVER_EXPIRES
				 * then disable minutes to live text box
				 * and set value to N/A
				 * else enable text box
				 * and set value to default i.e 10
				 */
                if (comboExpPolicy.getSelectedItem().equals(message("expPolNvrExp"))) {
                    txtMinLive.setText(message("dlgDbgNA"));
                    txtMinLive.setEnabled(false);
                } else {
                    txtMinLive.setEnabled(true);
                    txtMinLive.setText("");
                }
            }
        };
    }

    private ItemListener createBackupCheckListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                /*
				 * If user selects backup option
				 * then check virtual machine instances > 2
				 * otherwise give warning
				 */
                if (backupCheck.isSelected()) {
                    int vmCnt = 0;
                    try {
                        vmCnt = Integer.parseInt(waRole.getInstances());
                    } catch (Exception ex) {
                        PluginUtil.displayErrorDialogAndLog(message("genErrTitle"), message("vmInstGetErMsg"), ex);
                    }
                    if (vmCnt < 2) {
						/*
						 * If virtual machine instances < 2
						 * then make back up check-box unchecked.
						 */
                        backupCheck.setSelected(false);
                        Messages.showWarningDialog(message("backWarnMsg"), message("backWarnTtl"));
                    }
                }
            }
        };
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("cachTxt"), message("cachMsg"));
    }

    public String getCacheName() {
        return cacheName;
    }

    protected void doOKAction() {
        boolean okToProceed;
        String cachNameTxt = txtCacheName.getText().trim();
        String port = txtPortNum.getText().trim();
        String mtl = txtMinLive.getText().trim();
        try {
            // Check values for all fields are specified
            if (!txtCacheName.getText().equals("") && !txtPortNum.getText().equals("") && !txtMinLive.getText().equals("")) {
				/*
				 * Check if expiration policy is NEVER_EXPIRES
				 * then only validate cache name and port number
				 * else all three fields
				 * name, port, minutes to live.
				 */
                if (comboExpPolicy.getSelectedItem().equals(message("expPolNvrExp"))) {
                    okToProceed = isValidName(cachNameTxt) && validatePort(port);
                } else {
                    okToProceed = isValidName(cachNameTxt) && validatePort(port) && validateMtl(mtl);
                }
            } else {
                okToProceed = false;
                PluginUtil.displayErrorDialog(message("cachErrTtl"), message("cachSpcfyAll"));
            }
            if (okToProceed) {
                // Edit case
                if (isEdit) {
                    WindowsAzureNamedCache namedCache = cacheMap.get(cacheName);
				    /*
				     * If cache name is edited
				     * then set name to newer value.
				     */
                    if (!namedCache.getName().equals(cachNameTxt)) {
                        namedCache.setName(cachNameTxt);
                        cacheName = cachNameTxt;
                    }
                    namedCache.getEndpoint().setPrivatePort(port);
                    setCacheAttributes(namedCache);
                } else {
                    cacheName = cachNameTxt;
                    WindowsAzureNamedCache namedCache = waRole.addNamedCache(cachNameTxt, Integer.parseInt(port));
                    setCacheAttributes(namedCache);
                }
                super.doOKAction();
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("cachErrTtl"), message("cachSetErrMsg"));
        }
    }

    /**
     * Validates the name of cache.
     *
     * @param name
     * @return Boolean
     */
    private Boolean isValidName(String name) {
        boolean retVal = true;
        try {
            retVal = CacheDialogUtilMethods.isValidName(name, cacheMap, isEdit, cacheName);
        } catch (AzureCommonsException e) {
            retVal = false;
            PluginUtil.displayErrorDialog(message("genErrTitle"), e.getMessage());
        }
        return retVal;
    }

    /**
     * Validates the Minutes to live attribute of named cache.
     * Value must be numeric and should be at least 0
     *
     * @param minToLive
     * @return Boolean
     */
    private Boolean validateMtl(String minToLive) {
        Boolean isVallidMtl = CacheDialogUtilMethods.validateMtl(minToLive);
        if (!isVallidMtl) {
            PluginUtil.displayErrorDialog(message("cachMtlErrTtl"), message("cachMtlErrMsg"));
        }
        return isVallidMtl;
    }

    /**
     * Validates the port number of named cache.
     * Positive integer between 1 to 65535 is allowed.
     *
     * @param port
     * @return Boolean
     */
    private Boolean validatePort(String port) {
        Boolean isValidPortRng = false;
        try {
            isValidPortRng = CacheDialogUtilMethods.validatePort(port, cacheMap, cacheName, isEdit, txtCacheName.getText(), waRole);
        } catch (AzureCommonsException e) {
            PluginUtil.displayErrorDialog(message("genErrTitle"), e.getMessage());
        }
        return isValidPortRng;
    }

    /**
     * Function sets values of named cache attributes
     * like backup option, expiration policy and minutes to live.
     *
     * @param namedCache
     */
    private void setCacheAttributes(WindowsAzureNamedCache namedCache) {
        String expPolCmbTxt = (String) comboExpPolicy.getSelectedItem();
		/*
		 * Mapping of expiration policies shown on UI
		 * to actual values stored in project manager object
		 */
        WindowsAzureCacheExpirationPolicy expPol = CacheDialogUtilMethods.getExpPolObject(expPolCmbTxt);
        try {
            CacheDialogUtilMethods.setCacheAttributes(namedCache, expPolCmbTxt, backupCheck.isSelected(), expPol, txtMinLive.getText().trim());
        } catch (Exception e) {
            PluginUtil.displayErrorDialog(message("cachErrTtl"), message("cachSetErrMsg"));
        }
    }

    @Override
    protected String getHelpId() {
        return "windows_azure_cache_dialog";
    }
}
