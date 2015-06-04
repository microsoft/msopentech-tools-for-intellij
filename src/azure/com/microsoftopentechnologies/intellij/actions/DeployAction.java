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
package com.microsoftopentechnologies.intellij.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventListener;
import com.microsoftopentechnologies.azurecommons.deploy.model.AutoUpldCmpnts;
import com.microsoftopentechnologies.azurecommons.deploy.model.CertificateUpload;
import com.microsoftopentechnologies.azurecommons.deploy.model.CertificateUploadList;
import com.microsoftopentechnologies.azurecommons.deploy.model.RemoteDesktopDescriptor;
import com.microsoftopentechnologies.azurecommons.deploy.wizard.ConfigurationEventArgs;
import com.microsoftopentechnologies.azurecommons.exception.DeploymentException;
import com.microsoftopentechnologies.azurecommons.roleoperations.JdkSrvConfigUtilMethods;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.wacommonutil.CerPfxUtil;
import com.microsoftopentechnologies.azurecommons.wacommonutil.EncUtilHelper;
import com.microsoftopentechnologies.azurecommons.wacommonutil.PreferenceSetUtil;
import com.microsoftopentechnologies.azuremanagementutil.model.StorageService;
import com.microsoftopentechnologies.intellij.deploy.*;
import com.microsoftopentechnologies.intellij.ui.JdkServerPanel;
import com.microsoftopentechnologies.intellij.util.AntHelper;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.interopbridges.tools.windowsazure.*;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateListResponse;
import com.microsoft.windowsazure.management.storage.models.StorageAccountCreateParameters;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.AzureSettings;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.ui.DeployWizardDialog;
import com.microsoftopentechnologies.intellij.ui.PfxPwdDialog;
import com.microsoftopentechnologies.intellij.ui.messages.AzureBundle;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.wacommon.utils.WACommonException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class DeployAction extends AnAction {
    private static final String BASE_PATH = "${basedir}";
    private static final String AUTO = "auto";
    private static final String DASH_AUTO = "-auto";

    /**
     * To store components which got modified before build.
     */
    List<AutoUpldCmpnts> mdfdCmpntList = new ArrayList<AutoUpldCmpnts>();
    /**
     * To store roles of whom cache property
     * got modified before build.
     */
    List<String> roleMdfdCache = new ArrayList<String>();

    public void actionPerformed(AnActionEvent event) {
        Module module = event.getData(LangDataKeys.MODULE);
        DeployWizardDialog deployDialog = new DeployWizardDialog(module);
        final String title = AzureBundle.message("deplWizTitle");
        deployDialog.setTitle(title);
        deployDialog.show();

        if (deployDialog.isOK()) {
            try {
                String modulePath = PluginUtil.getModulePath(module);
                WindowsAzureProjectManager waProjManager = WindowsAzureProjectManager.load(new File(modulePath));

                // Update global properties in package.xml
                updateGlobalPropertiesinPackage(waProjManager);

                // Configure or remove remote access settings
                boolean status = handleRDPSettings(waProjManager, deployDialog, modulePath);
                if (!status) {
                    return;
                }
                // certificate upload configuration
                List<WindowsAzureCertificate> certToUpload = handleCertUpload(waProjManager);
                if (certToUpload != null && certToUpload.size() > 0) {
                    List<CertificateUpload> certUploadList = new ArrayList<CertificateUpload>();
                    for (int i = 0; i < certToUpload.size(); i++) {
                        WindowsAzureCertificate cert = certToUpload.get(i);
                        String name = cert.getName();
                        Boolean invokePfxDlg = true;
                    /*
					 * If Remote access is enabled and
					 * using sample certificate, then don't ask PFX file path
					 * and password. Just assign default values.
					 */
                        if (name.equalsIgnoreCase(message("remoteAccessPasswordEncryption")) && checkRDPUsesSampleCert(waProjManager, modulePath)) {
                            invokePfxDlg = false;
                            String defaultPfxPath = String.format("%s%s", modulePath, message("pfxPath"));
                            CertificateUpload object = new CertificateUpload(name, cert.getFingerPrint(), defaultPfxPath, message("defPfxPwd"));
                            certUploadList.add(object);
                        }
                        if (invokePfxDlg) {
                            // open dialog to accept pfx password
                            PfxPwdDialog inputDlg = new PfxPwdDialog(cert);
                            inputDlg.show();
                            if (inputDlg.isOK()) {
                                CertificateUpload object = new CertificateUpload(name, cert.getFingerPrint(), inputDlg.getPfxPath(), inputDlg.getPwd());
                                certUploadList.add(object);
                            } else {
							/*
							 * Just return to publish wizard.
							 * No need to save any information.
							 */
                                return;
                            }
                        }
                    }
                    deployDialog.fireConfigurationEvent(new ConfigurationEventArgs(this,ConfigurationEventArgs.CERTIFICATES,
                            new CertificateUploadList(certUploadList)));
                }
//            showBusy(true);
                // clear new service array
                deployDialog.getNewServices().clear();

                // set target OS
                String wizTargetOS = deployDialog.getTargetOSName();
                if (!waProjManager.getOSFamily().getName().equalsIgnoreCase(wizTargetOS)) {
                    for (OSFamilyType osType : OSFamilyType.values()) {
                        if (osType.getName().equalsIgnoreCase(wizTargetOS)) {
                            waProjManager.setOSFamily(osType);
                        }
                    }
                }
                // WORKITEM: China Support customizable portal URL in the plugin
                try {
                    String prefSetUrl = PreferenceSetUtil.getSelectedPortalURL(PreferenceSetUtil.getSelectedPreferenceSetName(AzurePlugin.prefFilePath),
                            AzurePlugin.prefFilePath);
				/*
				 * Don't check if URL is empty or null.
				 * As if it is then we remove "portalurl" attribute
				 * from package.xml.
				 */
                    waProjManager.setPortalURL(prefSetUrl);
                } catch (WACommonException e1) {
//                Display.getDefault().syncExec(new Runnable() {
//                    public void run() {
                    PluginUtil.displayErrorDialog(message("error"), message("getPrefUrlErMsg"));
//                    }
//                });
                }
                waProjManager.save();

                waProjManager = WindowsAzureProjectManager.load(new File(modulePath));

                WAAutoStorageConfTask autoStorageConfJob = new WAAutoStorageConfTask(module, waProjManager, event.getDataContext());
                autoStorageConfJob.queue();
            } catch (Exception e) {
                PluginUtil.displayErrorDialogInAWTAndLog(message("buildFail"), message("projLoadErr"), e);
                return;
            }
            return;
        }
    }

    private void updateGlobalPropertiesinPackage(WindowsAzureProjectManager waProjManager) throws WindowsAzureInvalidProjectOperationException {
        String currentSubscriptionID = WizardCacheManager.getCurrentPublishData().getCurrentSubscription().getSubscriptionID();
        waProjManager.setPublishSubscriptionId(currentSubscriptionID);
        waProjManager.setPublishSettingsPath(WizardCacheManager.getPublishSettingsPath(currentSubscriptionID));
        waProjManager.setPublishCloudServiceName(WizardCacheManager.getCurentHostedService().getServiceName());
        waProjManager.setPublishRegion(WizardCacheManager.getCurentHostedService().getProperties().getLocation());
        waProjManager.setPublishStorageAccountName(WizardCacheManager.getCurrentStorageAcount().getServiceName());
        waProjManager.setPublishDeploymentSlot(DeploymentSlot.valueOf(WizardCacheManager.getCurrentDeplyState()));
        waProjManager.setPublishOverwritePreviousDeployment(Boolean.parseBoolean(WizardCacheManager.getUnpublish()));
    }

    private class WindowsAzureBuildProjectTask extends Task.Backgroundable {

        private WindowsAzureProjectManager waProjManager;
        private Module myModule;
        private DataContext dataContext;

        public WindowsAzureBuildProjectTask(@NotNull final Module module, WindowsAzureProjectManager manager, DataContext dataContext) {
            super(module.getProject(), message("buildProj"), true, Backgroundable.DEAF);
            this.waProjManager = manager;
            this.myModule = module;
            this.dataContext = dataContext;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                waProjManager.setPackageType(WindowsAzurePackageType.CLOUD);
                waProjManager.save();
                indicator.setFraction(0.0);
                indicator.setText2(String.format(message("buildingProjTask"), myModule.getName()));
                AntHelper.runAntBuild(dataContext, myModule, AntHelper.createDeployListener(myModule, mdfdCmpntList, roleMdfdCache));
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ToolWindowManager.getInstance(myModule.getProject()).getToolWindow(ToolWindowId.MESSAGES_WINDOW).activate(null);
                    }
                });
                indicator.setFraction(1.0);
            } catch (Exception e) {
                log(message("error"), e);
            }
        }
    }

    private class WAAutoStorageConfTask extends Task.Backgroundable {
        private WindowsAzureProjectManager waProjManager;
        private boolean isError = false;
        private Module myModule;
        private DataContext dataContext;

        public WAAutoStorageConfTask(@NotNull final Module module, WindowsAzureProjectManager manager, DataContext dataContext) {
            super(module.getProject(), message("confStorageAccount"), true, Backgroundable.DEAF);
            this.waProjManager = manager;
            this.myModule = module;
            this.dataContext = dataContext;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                // Create storage account if it does not exists
                createStorageAccountIfNotExists();
				/*
				 * Check components having upload method "AUTO"
				 * and cloudurl set to auto, update them with
				 * appropriate blob url and key
				 * as per storage account selected on wizard.
				 */
                waProjManager = removeAutoCloudUrl(waProjManager);
                waProjManager.save();
            } catch (Exception e) {
                log(message("error"), e);
                isError = true;
            }
        }

        @Override
        public void onSuccess() {
            if (!isError) {
                WindowsAzureProjectManager waProjManager = null;
                try {
                    waProjManager = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(myModule)));
                } catch (WindowsAzureInvalidProjectOperationException e) {
                    PluginUtil.displayErrorDialog(message("error"), message("deployErr"));
                    return;
                }
                WindowsAzureBuildProjectTask buildProjectTask = new WindowsAzureBuildProjectTask(myModule, waProjManager, dataContext);
                buildProjectTask.queue();
            }
        }

        private void createStorageAccountIfNotExists() throws Exception {
            StorageService storageAccount = WizardCacheManager.getCurrentStorageAcount();

            if (storageAccount.getUrl() == null || storageAccount.getUrl().isEmpty()) {
                StorageAccountCreateParameters accountParameters = new StorageAccountCreateParameters();
                accountParameters.setName(storageAccount.getServiceName());
                accountParameters.setLabel(storageAccount.getServiceName());
                accountParameters.setLocation(storageAccount.getStorageAccountProperties().getLocation());
                accountParameters.setDescription(storageAccount.getStorageAccountProperties().getDescription());
                StorageService storageService = WizardCacheManager.createStorageAccount(accountParameters);
            /*
             * Add newly created storage account
			 * in centralized storage account registry.
			 */
                StorageAccount storageAccountPref = new StorageAccount(storageService.getServiceName(),
                        storageService.getPrimaryKey(),
                        storageService.getStorageAccountProperties().getEndpoints().get(0).toString());
                StorageAccountRegistry.addAccount(storageAccountPref);
                AzureSettings.getSafeInstance(myModule.getProject()).saveStorage();
            }
        }
    }

    public static class WindowsAzureDeploymentTask extends Task.Backgroundable {

        private final Module myModule;
        private String deploymentId;
//        private String name;

        public WindowsAzureDeploymentTask(Module selectedModule) {
            super(selectedModule.getProject(), message("deployingToAzure"), true, Backgroundable.DEAF);
            this.myModule = selectedModule;
        }

        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
//            MessageConsole myConsole = Activator.findConsole(Activator.CONSOLE_NAME);

//            myConsole.clearConsole();

//            final MessageConsoleStream out = myConsole.newMessageStream();

//            monitor.beginTask(name, 100);
            System.out.println(message("deployingToAzureMsg"));
            AzurePlugin.removeUnNecessaryListener();
            DeploymentEventListener deployListnr = new DeploymentEventListener() {

                @Override
                public void onDeploymentStep(DeploymentEventArgs args) {
                    indicator.checkCanceled();
                    deploymentId = args.getId();
                    log(args.toString());
                    log("Complete: " + args.getDeployCompleteness());
                    indicator.setFraction(indicator.getFraction() + args.getDeployCompleteness() / 100.0);
                    indicator.setText(message("deployingToAzureMsg"));
                    indicator.setText2(args.getDeployMessage());
                }
            };
            AzurePlugin.addDeploymentEventListener(deployListnr);
            AzurePlugin.depEveList.add(deployListnr);

             doTask();
//            Thread thread = doAsync();

//            while (wait.get() == true) {
//                if (monitor.isCanceled()) {
//                    DeploymentEventArgs canceled = createDeploymentCanceledEventArgs(deploymentId);
//                    Activator.getDefault().fireDeploymentEvent(canceled);
//                    thread.interrupt();
//                    super.setName("");
//                    monitor.done();
//                    super.done(Status.CANCEL_STATUS);
//                    return Status.CANCEL_STATUS;
//                }
//            }
//            monitor.done();
//            super.setName("");
//            super.done(Status.OK_STATUS);
//            return Status.OK_STATUS;
        }

        @Nullable
        @Override
        public NotificationInfo getNotificationInfo() {
            return new NotificationInfo(message("proPageBFCloud"), message("deplCompleted"), "");
        }

        private DeploymentEventArgs createDeploymentCanceledEventArgs(String id) {
            DeploymentEventArgs canceledArgs = new DeploymentEventArgs(this);
            canceledArgs.setDeployMessage(message("deploymentCanceled"));
            canceledArgs.setId(id);
            canceledArgs.setDeployCompleteness(100);
            return canceledArgs;

        }

        private void doTask() {
            try {
                DeploymentManager.getInstance().deploy(myModule);
            } catch (InterruptedException e) {
            } catch (DeploymentException e) {
                log(message("error"), e);
            }
        }

        @Override
        public void onSuccess() {
            log("onSuccess");
            new Notification("Azure", "Deployment complete",
                    "<html>You can see your deployment at <a href=''>link</a></html>",
                    NotificationType.INFORMATION);
        }

        public void onCancel() {
            log("onCancel");
            new Notification("Azure", "Cancelled", "Cancelled", NotificationType.INFORMATION);
        }

    }

    /**
     * If remote desktop is enabled
     * then method checks whether
     * its using sample certificate or not.
     * @param waProjManager
     * @return
     */
    private boolean checkRDPUsesSampleCert(WindowsAzureProjectManager waProjManager, String modulePath) {
        Boolean usesSampleCert = false;
        try {
            if (waProjManager.getRemoteAccessAllRoles()) {
				/*
				 * Check if sample certificate is used or
				 * custom one.
				 */
                String certPath = waProjManager.getRemoteAccessCertificatePath();
                if (certPath.startsWith(BASE_PATH)) {
                    certPath = certPath.substring(certPath.indexOf("}") + 1, certPath.length());
                    certPath = String.format("%s%s", modulePath, certPath);
                }
                String thumbprint = CerPfxUtil.getThumbPrint(certPath);
                if (thumbprint.equals(message("dfltThmbprnt"))) {
                    usesSampleCert = true;
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("error"), message("certUploadEr"), e);
        }
        return usesSampleCert;
    }


    /**
     * Configure or remove remote access settings
     * according to user name provided by user.
     * @param waProjManager
     */
    private boolean handleRDPSettings(WindowsAzureProjectManager waProjManager, DeployWizardDialog deployDialog, String modulePath) {
        try {
            String userName = null;
            String pwd = null;
            Date expDate = null;
            String certPath = "";
            boolean remoteEnabled = false;
            if (deployDialog.getRdpUname().isEmpty()) {
                // disable remote access
                waProjManager.setRemoteAccessAllRoles(false);
            } else {
                remoteEnabled = true;
                userName = deployDialog.getRdpUname();
                pwd = deployDialog.getRdpPwd();
                // already enabled
                if (waProjManager.getRemoteAccessAllRoles()) {
                    certPath = waProjManager.getRemoteAccessCertificatePath();
                    if (certPath.startsWith(BASE_PATH)) {
                        certPath = certPath.substring(certPath.indexOf("}") + 1, certPath.length());
                        certPath = String.format("%s%s", modulePath, certPath);
                    }
                    // if user name has been changed by user
                    if (!userName.equals(waProjManager.getRemoteAccessUsername())) {
                        waProjManager.setRemoteAccessUsername(userName);
                    }
                    // if password has been changed by user
                    if (!pwd.equals(waProjManager.getRemoteAccessEncryptedPassword())) {
                        String encryptedPwd = EncUtilHelper.encryptPassword(pwd, certPath, AzurePlugin.pluginFolder);
                        waProjManager.setRemoteAccessEncryptedPassword(encryptedPwd);
                    }
                    expDate = waProjManager.getRemoteAccessAccountExpiration();
                } else {
                    // enabling for the first time, so use all default entries
                    String defaultCertPath = String.format("%s%s", modulePath, message("cerPath"));
                    File certFile = new File(defaultCertPath);
                    if (certFile.exists()) {
                        String defaultThumbprint = CerPfxUtil.getThumbPrint(defaultCertPath);
                        if (defaultThumbprint.equals(message("dfltThmbprnt"))) {
                            waProjManager.setRemoteAccessAllRoles(true);
                            waProjManager.setRemoteAccessUsername(userName);
                            certPath = defaultCertPath;
                            // save certificate path in package.xml in the format of
                            // ${basedir}\cert\SampleRemoteAccessPublic.cer
                            waProjManager.setRemoteAccessCertificatePath(String.format("%s%s", BASE_PATH, message("cerPath")));
                            // save thumb print
                            try {
                                if (waProjManager.isRemoteAccessTryingToUseSSLCert(defaultThumbprint)) {
                                    PluginUtil.displayErrorDialog(message("remAccSyntaxErr"), message("usedBySSL"));
                                    return false;
                                } else {
                                    waProjManager.setRemoteAccessCertificateFingerprint(defaultThumbprint);
                                }
                            } catch (Exception e) {
                                PluginUtil.displayErrorDialog(message("error"), message("dfltImprted"));
                                return false;
                            }
                            // save password, encrypt always as storing for the first time
                            String encryptedPwd = EncUtilHelper.encryptPassword(pwd, certPath, AzurePlugin.pluginFolder);
                            waProjManager.setRemoteAccessEncryptedPassword(encryptedPwd);
                            // save expiration date
                            GregorianCalendar currentCal = new GregorianCalendar();
                            currentCal.add(Calendar.YEAR, 1);
                            expDate = currentCal.getTime();
                            waProjManager.setRemoteAccessAccountExpiration(expDate);
                        } else {
                            PluginUtil.displayErrorDialog(message("error"), message("dfltErrThumb"));
                            return false;
                        }
                    } else {
                        PluginUtil.displayErrorDialog(message("error"), message("dfltErr"));
                        return false;
                    }
                }
            }
            deployDialog.fireConfigurationEvent(new ConfigurationEventArgs(this, ConfigurationEventArgs.REMOTE_DESKTOP,
                    new RemoteDesktopDescriptor(userName, pwd, expDate, certPath, deployDialog.getConToDplyChkStatus(), remoteEnabled)));
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("error"), message("rdpError"), e);
            return false;
        }
        return true;
    }

    /**
     * Prepares list of certificates which needs to be
     * uploaded to cloud service by comparing
     * certificates present in particular cloud service
     * with the certificates configured in selected project.
     * @param projMngr
     * @return
     */
    private List<WindowsAzureCertificate> handleCertUpload(WindowsAzureProjectManager projMngr) {
        List<WindowsAzureCertificate> certToUpload = new ArrayList<WindowsAzureCertificate>();
        List<ServiceCertificateListResponse.Certificate> cloudCertList = null;
        try {
            HostedServiceListResponse.HostedService service = WizardCacheManager.getCurentHostedService();
            if (service.getUri() != null) {
                cloudCertList = WizardCacheManager.fetchUploadedCertificates();
            }
            List<WindowsAzureRole> roleList = projMngr.getRoles();
            // iterate over roles
            for (int i = 0; i < roleList.size(); i++) {
                WindowsAzureRole role = roleList.get(i);
                Map<String, WindowsAzureCertificate> pmlCertList = role.getCertificates();
                for (Iterator<Map.Entry<String, WindowsAzureCertificate>> iterator = pmlCertList.entrySet().iterator(); iterator.hasNext();) {
                    WindowsAzureCertificate pmlCert = iterator.next().getValue();
					/*
					 * No certificate present on cloud as REST API returned null
					 * Need to upload each certificate
					 */
                    if (cloudCertList == null || cloudCertList.isEmpty()) {
                        if (!isDuplicateThumbprintCert(certToUpload, pmlCert)) {
                            certToUpload.add(pmlCert);
                        }
                    } else {
						/*
						 * Check certificate is present on cloud
						 * or not.
						 */
                        boolean isPresent = false;
                        for (int j = 0; j < cloudCertList.size(); j++) {
                            ServiceCertificateListResponse.Certificate cloudCert = cloudCertList.get(j);
                            if (cloudCert.getThumbprint().equalsIgnoreCase(pmlCert.getFingerPrint())) {
                                isPresent = true;
                                break;
                            }
                        }
                        if (!isPresent && !isDuplicateThumbprintCert(certToUpload, pmlCert)) {
                            certToUpload.add(pmlCert);
                        }
                    }
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogInAWTAndLog(message("error"), message("certUploadEr"), e);
        }
        return certToUpload;
    }

    /**
     * Certificate is not present on cloud
     * but check whether its already
     * been added to certToUpload list.
     * To avoid unnecessary PFX password prompt
     * invocation.
     * @param certToUpload
     * @param pmlCert
     * @return
     */
    private boolean isDuplicateThumbprintCert(List<WindowsAzureCertificate> certToUpload, WindowsAzureCertificate pmlCert) {
        boolean alreadyAdded = false;
        for (int j = 0; j < certToUpload.size(); j++) {
            WindowsAzureCertificate certObj = certToUpload.get(j);
            if (certObj.getFingerPrint().equalsIgnoreCase(pmlCert.getFingerPrint())) {
                alreadyAdded = true;
                break;
            }
        }
        return alreadyAdded;
    }

    /**
     * Check each role and its respective
     * components having upload method "AUTO"
     * and cloudurl set to auto, update them with
     * appropriate blob url and key
     * as per storage account selected on wizard.
     * Also method remembers which components has been updated
     * so that after project build
     * they can be restored to original state.
     * @param projMngr
     * @return
     */
    private WindowsAzureProjectManager removeAutoCloudUrl(WindowsAzureProjectManager projMngr) {
        mdfdCmpntList.clear();
        roleMdfdCache.clear();
        try {
            // get number of roles in one project
            List<WindowsAzureRole> roleList = projMngr.getRoles();
            StorageService curAcc = WizardCacheManager.getCurrentStorageAcount();
            String curKey = curAcc.getPrimaryKey();
            String accUrl = curAcc.getStorageAccountProperties().getEndpoints().get(0).toString();
            for (int i = 0; i < roleList.size(); i++) {
                WindowsAzureRole role = roleList.get(i);
				/*
				 * check for caching storage account name given
				 * and its "auto"
				 * then update cache name and key.
				 */
                String name = role.getCacheStorageAccountName();
                if (name != null && !name.isEmpty() && name.equals(DASH_AUTO)) {
                    roleMdfdCache.add(role.getName());
                    role.setCacheStorageAccountName(curAcc.getServiceName());
                    role.setCacheStorageAccountKey(curKey);
                    role.setCacheStorageAccountUrl(accUrl);
                }
                // get list of components in one role.
                List<WindowsAzureRoleComponent> cmpnntsList = role.getComponents();
                for (int j = 0; j < cmpnntsList.size(); j++) {
                    WindowsAzureRoleComponent component = cmpnntsList.get(j);
                    String cmpntType = component.getType();
                    WARoleComponentCloudUploadMode mode = component.getCloudUploadMode();
					/*
					 * Check component is of JDK or server
					 * and auto upload is enabled.
					 */
                    if (((cmpntType.equals(message("typeJdkDply")) || cmpntType.equals(message("typeSrvDply")))
                            && mode != null && mode.equals(WARoleComponentCloudUploadMode.auto))
                            || (cmpntType.equals(message("typeSrvApp")) && mode != null
                            && mode.equals(WARoleComponentCloudUploadMode.always))) {
                        /*
						 * Check storage account is not specified,
						 * i.e URL is auto
						 */
                        if (component.getCloudDownloadURL().equalsIgnoreCase(AUTO)) {
                            // update cloudurl and cloudkey
							/*
							 * If component is JDK, then check if its
							 * third party JDK.
							 */
                            if (cmpntType.equals(message("typeJdkDply"))) {
                                String jdkName = role.getJDKCloudName();
                                if (jdkName == null || jdkName.isEmpty()) {
                                    component.setCloudDownloadURL(JdkSrvConfigUtilMethods.prepareCloudBlobURL(component.getImportPath(), accUrl));
                                } else {
                                    component.setCloudDownloadURL(JdkSrvConfigUtilMethods.prepareUrlForThirdPartyJdk(jdkName, accUrl, AzurePlugin.cmpntFile));
                                }
                            } else if (cmpntType.equals(message("typeSrvDply"))) {
                                String srvName = role.getServerCloudName();
                                if (srvName == null || srvName.isEmpty()) {
                                    component.setCloudDownloadURL(JdkSrvConfigUtilMethods.prepareCloudBlobURL(component.getImportPath(), accUrl));
                                } else {
                                    component.setCloudDownloadURL(JdkServerPanel.prepareUrlForThirdPartySrv(srvName, accUrl));
                                }
                            } else {
                                component.setCloudDownloadURL(JdkSrvConfigUtilMethods.prepareUrlForApp(component.getDeployName(), accUrl));
                            }
                            component.setCloudKey(curKey);
                            // Save components that are modified
                            AutoUpldCmpnts obj = new AutoUpldCmpnts(role.getName());
							/*
							 * Check list contains
							 * entry with this role name,
							 * if yes then just add index of entry to list
							 * else create new object.
							 */
                            if (mdfdCmpntList.contains(obj)) {
                                int index = mdfdCmpntList.indexOf(obj);
                                AutoUpldCmpnts presentObj = mdfdCmpntList.get(index);
                                if (!presentObj.getCmpntIndices().contains(j)) {
                                    presentObj.getCmpntIndices().add(j);
                                }
                            } else {
                                mdfdCmpntList.add(obj);
                                obj.getCmpntIndices().add(j);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogInAWTAndLog(message("error"), message("autoUploadEr"), e);
        }
        return projMngr;
    }

    /**
     * Method restores components which are updated before build
     * to original state i.e. again updates cloudurl to "auto"
     * and removes cloudkey attribute.
     * @param projMngr
     * @return
     */
    public static WindowsAzureProjectManager addAutoCloudUrl(WindowsAzureProjectManager projMngr, List<AutoUpldCmpnts> mdfdCmpntList) {
        try {
            // get number of roles in one project
            List<WindowsAzureRole> roleList = projMngr.getRoles();
            for (int i = 0; i < roleList.size(); i++) {
                WindowsAzureRole role = roleList.get(i);
                AutoUpldCmpnts obj = new AutoUpldCmpnts(role.getName());
                // check list has entry with this role name
                if (mdfdCmpntList.contains(obj)) {
                    // get list of components
                    List<WindowsAzureRoleComponent> cmpnntsList = role.getComponents();
                    // get indices of components which needs to be updated.
                    int index = mdfdCmpntList.indexOf(obj);
                    AutoUpldCmpnts presentObj = mdfdCmpntList.get(index);
                    List<Integer> indices = presentObj.getCmpntIndices();
                    // iterate over indices and update respective components.
                    for (int j = 0; j < indices.size(); j++) {
                        WindowsAzureRoleComponent cmpnt = cmpnntsList.get(indices.get(j));
                        cmpnt.setCloudDownloadURL(AUTO);
                        cmpnt.setCloudKey("");
                    }
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogInAWTAndLog(message("error"), message("autoUploadEr"), e);
        }
        return projMngr;
    }

    /**
     * Method restores caching properties which are updated before build
     * to original state i.e. again updates storage account name to "auto"
     * and removes key property.
     * @param projMngr
     * @return
     */
    public static WindowsAzureProjectManager addAutoSettingsForCache(WindowsAzureProjectManager projMngr, List<String> roleMdfdCache) {
        try {
            // get number of roles in one project
            List<WindowsAzureRole> roleList = projMngr.getRoles();
            for (int i = 0; i < roleList.size(); i++) {
                WindowsAzureRole role = roleList.get(i);
                if (roleMdfdCache.contains(role.getName())) {
                    role.setCacheStorageAccountName(DASH_AUTO);
                    role.setCacheStorageAccountKey("");
                    role.setCacheStorageAccountUrl("");
                }
            }
        } catch (Exception e) {
            PluginUtil.displayErrorDialogInAWTAndLog(message("error"), message("autoUploadEr"), e);
        }
        return projMngr;
    }


    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        event.getPresentation().setEnabledAndVisible(module != null && AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))
                && PluginUtil.isModuleRoot(selectedFile, module));
    }
}