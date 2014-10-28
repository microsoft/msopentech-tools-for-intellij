/**
 * Copyright 2014 Microsoft Open Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.deploy;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.OperationStatus;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.storage.models.StorageAccountCreateParameters;

import com.microsoftopentechnologies.deploy.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.deploy.deploy.DeploymentManagerUtilMethods;
import com.microsoftopentechnologies.deploy.model.*;
import com.microsoftopentechnologies.exception.DeploymentException;
import com.microsoftopentechnologies.exception.RestAPIException;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.AzureSettings;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import com.microsoftopentechnologies.model.InstanceStatus;
import com.microsoftopentechnologies.model.Notifier;
import com.microsoftopentechnologies.model.StorageService;
import com.microsoftopentechnologies.rest.WindowsAzureRestUtils;
import com.microsoftopentechnologies.rest.WindowsAzureServiceManagement;
import com.microsoftopentechnologies.rest.WindowsAzureStorageServices;
import com.microsoftopentechnologies.storageregistry.StorageAccount;
import com.microsoftopentechnologies.storageregistry.StorageAccountRegistry;

import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse.HostedService;
//import com.gigaspaces.azure.views.WindowsAzureActivityLogView;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoftopentechnologies.wacommon.utils.WACommonException;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public final class DeploymentManager {

	private final HashMap<String, DeployDescriptor> deployments = new HashMap<String, DeployDescriptor>();

	private static final DeploymentManager DEFAULT_MANAGER = new DeploymentManager();

	public static DeploymentManager getInstance() {
		return DEFAULT_MANAGER;
	}

	private DeploymentManager() {

	}

	public void addDeployment(String name, DeployDescriptor deployment) {
		deployments.put(name, deployment);
	}

	public void removeDeployment(String name) {
		deployments.remove(name);
	}

	public HashMap<String, DeployDescriptor> getDeployments() {
		return deployments;
	}

	public void deploy(Module selectedModule) throws InterruptedException, DeploymentException {

		DeployDescriptor deploymentDesc = WizardCacheManager.collectConfiguration();

		String deployState = deploymentDesc.getDeployState();
		try {

			int conditionalProgress = 20;

			HostedService hostedService = deploymentDesc.getHostedService();
			addDeployment(hostedService.getServiceName(),deploymentDesc);

			StorageService storageAccount = deploymentDesc.getStorageAccount();


			WindowsAzureServiceManagement service = WizardCacheManager.createServiceManagementHelper();

			openWindowsAzureActivityLogView(deploymentDesc);

			if (deploymentDesc.getDeployMode() == WindowsAzurePackageType.LOCAL) {
				deployToLocalEmulator(selectedModule, deploymentDesc);
				notifyProgress(deploymentDesc.getDeploymentId(), null, 100, OperationStatus.Succeeded, message("deplCompleted"));
				return;
			}

			// need to improve this check (maybe hostedSerivce.isExisting())?
			if (hostedService.getUri() == null || hostedService.getUri().toString().isEmpty()) { // the hosted service was not yet created.
				notifyProgress(deploymentDesc.getDeploymentId(), null, 5, OperationStatus.InProgress, String.format("%s - %s", message("createHostedService"), hostedService.getServiceName()));
				createHostedService(hostedService.getServiceName(), hostedService.getServiceName(),
						hostedService.getProperties().getLocation(), hostedService.getProperties().getDescription());
				conditionalProgress -= 5;
			}

			// same goes here
			if (storageAccount.getUrl() == null || storageAccount.getUrl().isEmpty()) { // the storage account was not yet created
				notifyProgress(deploymentDesc.getDeploymentId(), null, 10, OperationStatus.InProgress,
                        String.format("%s - %s", message("createStorageAccount"), storageAccount.getServiceName()));
				createStorageAccount(storageAccount.getServiceName(), storageAccount.getServiceName(),
						storageAccount.getStorageAccountProperties().getLocation(), storageAccount.getStorageAccountProperties().getDescription());
				conditionalProgress -= 10;
			}

			checkContainerExistance();

			// upload certificates
			if (deploymentDesc.getCertList() != null) {
			List<CertificateUpload> certList = deploymentDesc.getCertList().getList();
			if (certList != null && certList.size() > 0) {
				for (int i = 0; i < certList.size(); i++) {
					CertificateUpload cert = certList.get(i);
					DeploymentManagerUtilMethods.uploadCertificateIfNeededGeneric(service, deploymentDesc, cert.getPfxPath(), cert.getPfxPwd());
					notifyProgress(deploymentDesc.getDeploymentId(), null, 0, OperationStatus.InProgress,
							String.format("%s%s", message("deplUploadCert"), cert.getName()));
				}
			}
			}

			if (deploymentDesc.getRemoteDesktopDescriptor().isEnabled()) {

				notifyProgress(deploymentDesc.getDeploymentId(), null, conditionalProgress, OperationStatus.InProgress, message("deplConfigRdp"));

				DeploymentManagerUtilMethods.configureRemoteDesktop(deploymentDesc, WizardCacheManager.getCurrentDeployConfigFile(),
                        String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.PLUGIN_ID));
			}
			else {
				notifyProgress(deploymentDesc.getDeploymentId(), null, conditionalProgress, OperationStatus.InProgress, message("deplConfigRdp"));
			}

			Notifier notifier = new NotifierImp();

			String targetCspckgName = createCspckTargetName(deploymentDesc);

			notifyProgress(deploymentDesc.getDeploymentId(), null, 20, OperationStatus.InProgress, message("uploadingServicePackage"));

			DeploymentManagerUtilMethods.uploadPackageService(
                    WizardCacheManager.createStorageServiceHelper(),
                    deploymentDesc.getCspkgFile(),
                    targetCspckgName,
                    message("eclipseDeployContainer").toLowerCase(),
                    deploymentDesc, notifier);

			notifyProgress(deploymentDesc.getDeploymentId(), null, 20, OperationStatus.InProgress, message("creatingDeployment"));

			String storageAccountURL = deploymentDesc.getStorageAccount().
                    getStorageAccountProperties().getEndpoints().get(0).toString();

			String cspkgUrl = String.format("%s%s/%s", storageAccountURL,
					message("eclipseDeployContainer").toLowerCase(), targetCspckgName);
			/*
			 * To make deployment name unique attach time stamp
			 * to the deployment name.
			 */
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String deploymentName = String.format("%s%s%s",
					hostedService.getServiceName(),
					deployState,
					dateFormat.format(new Date()));
			String requestId = DeploymentManagerUtilMethods.createDeployment(deploymentDesc, service, cspkgUrl, deploymentName);
			OperationStatus status = waitForStatus(deploymentDesc.getConfiguration(), service, requestId);

			DeploymentManagerUtilMethods.deletePackage(WizardCacheManager.createStorageServiceHelper(),
                    message("eclipseDeployContainer").toLowerCase(),
                    targetCspckgName, notifier);
			notifyProgress(deploymentDesc.getDeploymentId(),
					null, 0, OperationStatus.InProgress, message("deletePackage"));

			notifyProgress(deploymentDesc.getDeploymentId(), null, 20, OperationStatus.InProgress, message("waitingForDeployment"));

			DeploymentGetResponse deployment = waitForDeployment(
					deploymentDesc.getConfiguration(),
					hostedService.getServiceName(),
					service,
					deploymentName);

			boolean displayHttpsLink = deploymentDesc.getDisplayHttpsLink();
			
			final String url = displayHttpsLink ? deployment.getUri().toString().replaceAll("http://", "https://") : deployment.getUri().toString();
			notifyProgress(deploymentDesc.getDeploymentId(),
                    displayHttpsLink ? deployment.getUri().toString().replaceAll("http://", "https://") : deployment.getUri().toString(),
                    20, status,
                    deployment.getStatus().toString());
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    Messages.showInfoMessage("<html>Deployment can be accessed at:<br><a href=\"" + url + "\">" + url + "</a></html>", "Deployment Completed");
                }
            });
			if (deploymentDesc.isStartRdpOnDeploy()) {
                String pluginFolder = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.PLUGIN_ID);
				WindowsAzureRestUtils.getInstance().launchRDP(deployment, deploymentDesc.getRemoteDesktopDescriptor().getUserName(), pluginFolder);
			}
		}
		catch (Throwable t) {
			String msg = (t != null ? t.getMessage() : "");
			if (!msg.startsWith(OperationStatus.Failed.toString())) {
				msg = OperationStatus.Failed.toString() + " : " + msg;
			}
			notifyProgress(deploymentDesc.getDeploymentId(), null, 100,
                    OperationStatus.Failed, msg,
					deploymentDesc.getDeploymentId(),
					deployState);
			if (t instanceof DeploymentException) {
				throw (DeploymentException)t;
			}
			throw new DeploymentException(msg, t);
		}
	}

	private void createStorageAccount(final String storageServiceName, final String label, final String location, final String description)
            throws Exception {

        StorageAccountCreateParameters accountParameters = new StorageAccountCreateParameters();
        accountParameters.setName(storageServiceName);
        accountParameters.setLabel(label);
        accountParameters.setLocation(location);
        accountParameters.setDescription(description);

		StorageService storageService = WizardCacheManager.createStorageAccount(accountParameters);
		/*
		 * Add newly created storage account
		 * in centralized storage account registry.
		 */
		StorageAccount storageAccount = new StorageAccount(storageService.getServiceName(),
				storageService.getPrimaryKey(),
				storageService.
                        getStorageAccountProperties().getEndpoints().get(0).toString());
		StorageAccountRegistry.addAccount(storageAccount);
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).saveStorage();
	}

	private void createHostedService(final String hostedServiceName, final String label, final String location, final String description)
            throws Exception {
        HostedServiceCreateParameters createHostedService = new HostedServiceCreateParameters();
        createHostedService.setServiceName(hostedServiceName);
        createHostedService.setLabel(label);
        createHostedService.setLocation(location);
        createHostedService.setDescription(description);

		WizardCacheManager.createHostedService(createHostedService);
	}

	private void checkContainerExistance() throws Exception {
		WindowsAzureStorageServices storageServices = WizardCacheManager.createStorageServiceHelper();
        storageServices.createContainer(message("eclipseDeployContainer").toLowerCase());
	}

	private DeploymentGetResponse waitForDeployment(Configuration configuration,
                                                    String serviceName, WindowsAzureServiceManagement service, String deploymentName)
            throws Exception {
		DeploymentGetResponse deployment = null;
		String status = null;
		do {
			Thread.sleep(5000);
			deployment = service.getDeployment(configuration, serviceName, deploymentName);

			for (RoleInstance instance : deployment.getRoleInstances()) {
				status = instance.getInstanceStatus();
				if (InstanceStatus.ReadyRole.getInstanceStatus().equals(status)
						|| InstanceStatus.CyclingRole.getInstanceStatus().equals(status)
						|| InstanceStatus.FailedStartingVM.getInstanceStatus().equals(status)
						|| InstanceStatus.UnresponsiveRole.getInstanceStatus().equals(status)) {
					break;
				}
			}
		} while (status != null && !(InstanceStatus.ReadyRole.getInstanceStatus().equals(status)
				|| InstanceStatus.CyclingRole.getInstanceStatus().equals(status)
				|| InstanceStatus.FailedStartingVM.getInstanceStatus().equals(status)
				|| InstanceStatus.UnresponsiveRole.getInstanceStatus().equals(status)));

		if (!InstanceStatus.ReadyRole.getInstanceStatus().equals(status)) {
			throw new DeploymentException(status);
		}
		return deployment;
	}

	private OperationStatus waitForStatus(Configuration configuration, WindowsAzureServiceManagement service, String requestId)
            throws Exception {
		OperationStatusResponse op;
		OperationStatus status = null;
		do {
			op = service.getOperationStatus(configuration, requestId);
			status = op.getStatus();

			log(message("deplId") + op.getId());
			log(message("deplStatus") + op.getStatus());
			log(message("deplHttpStatus") + op.getHttpStatusCode());
			if (op.getError() != null) {
                log(message("deplErrorMessage") + op.getError().getMessage());
                throw new RestAPIException(op.getError().getMessage());
			}

			Thread.sleep(5000);

		} while (status == OperationStatus.InProgress);

		return status;
	}

	private String createCspckTargetName(DeployDescriptor deploymentDesc) {
		String cspkgName = String.format(message("cspkgName"), deploymentDesc.getHostedService().getServiceName(), deploymentDesc.getDeployState());
		return cspkgName;
	}

	private void deployToLocalEmulator(Module selectedModule, DeployDescriptor deploymentDesc) throws DeploymentException {
		WindowsAzureProjectManager waProjManager;
		try {
			waProjManager = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(selectedModule)));
			waProjManager.deployToEmulator();
		} catch (WindowsAzureInvalidProjectOperationException e) {
			throw new DeploymentException(e);
		}
	}

	public void notifyUploadProgress() {

	}

	public void notifyProgress(String deploymentId,
			String deploymentURL,
			int progress,
			OperationStatus inprogress, String message, Object... args) {

		DeploymentEventArgs arg = new DeploymentEventArgs(this);
		arg.setId(deploymentId);
		arg.setDeploymentURL(deploymentURL);
		arg.setDeployMessage(String.format(message, args));
		arg.setDeployCompleteness(progress);
		arg.setStartTime(new Date());
		arg.setStatus(inprogress);
		AzurePlugin.fireDeploymentEvent(arg);
	}

	private void openWindowsAzureActivityLogView(
			final DeployDescriptor descriptor) {

//		Display.getDefault().syncExec(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					WindowsAzureActivityLogView waView = (WindowsAzureActivityLogView) PlatformUI
//							.getWorkbench().getActiveWorkbenchWindow()
//							.getActivePage().showView(message("activityView"));
//
//					String desc = String.format(message("deplDesc"), descriptor.getHostedService().getServiceName(), descriptor.getDeployState());
//
//					waView.addDeployment(descriptor.getDeploymentId(), desc,
//							descriptor.getStartTime());
//
//				} catch (PartInitException e) {
//					log(message("deplCantOpenView"), e);
//				}
//
//			}
//		});
	}

	public void undeploy(final String serviceName,
			final String deplymentName,
			final String deploymentState)
					throws WACommonException,
					RestAPIException,
					InterruptedException {
//		Display.getDefault().syncExec(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					WindowsAzureActivityLogView waView = (WindowsAzureActivityLogView) PlatformUI
//							.getWorkbench().getActiveWorkbenchWindow()
//							.getActivePage()
//							.showView(message("activityView"));
//
//					String desc = String.format(message("undeployMsg"), serviceName,deploymentState);
//
//					waView.addDeployment(deplymentName, desc, new Date());
//
//				} catch (Exception e) {
//					log(message("deplCantOpenView"), e);
//				}
//			}
//		});

		Configuration configuration = WizardCacheManager.getCurrentPublishData().getCurrentConfiguration();
		int[] progressArr = new int[]{50, 50};
		unPublish(configuration, serviceName, deplymentName, progressArr);
	}

	/**
	 * Unpublish deployment without notifying user.
	 * @param configuration
	 * @param serviceName
	 * @param deplymentName
	 */
    public void unPublish(
            Configuration configuration,
            String serviceName,
            String deplymentName,
            int[] progressArr) {
        String requestId = null;

        int retryCount = 0;
        boolean successfull = false;
        while (!successfull) {
            try {
                retryCount++;
                WindowsAzureServiceManagement service = WizardCacheManager.createServiceManagementHelper();
                //          Commenting suspend deployment call since it is giving issues in china cloud.
                //			notifyProgress(deplymentName, null, progressArr[0], OperationStatus.InProgress,
                //					Messages.stoppingMsg, serviceName);
                //			requestId = service.updateDeploymentStatus(configuration,
                //					serviceName,
                //					deplymentName,
                //                    UpdatedDeploymentStatus.Suspended
                //            );
                //			waitForStatus(configuration, service, requestId);
                notifyProgress(deplymentName, null, progressArr[0], OperationStatus.InProgress, message("undeployProgressMsg"), deplymentName);
                requestId = service.deleteDeployment(configuration, serviceName, deplymentName);
                waitForStatus(configuration, service, requestId);
                notifyProgress(deplymentName, null, progressArr[1], OperationStatus.Succeeded, message("undeployCompletedMsg"), serviceName);
                successfull = true;
            } catch (Exception e) {
                // Retry 5 times
                if (retryCount > AzurePlugin.REST_SERVICE_MAX_RETRY_COUNT) {
                    log(message("deplError"), e);
                    notifyProgress(deplymentName, null, 100, OperationStatus.Failed, e.getMessage(), serviceName);
                }
                notifyProgress(deplymentName, null, -progressArr[0], OperationStatus.InProgress, message("undeployProgressMsg"), deplymentName);
            }
        }
    }
}
