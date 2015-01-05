/**
 * Copyright 2013 Persistent Systems Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.util;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azuremanagementutil.model.StorageService;
import com.microsoftopentechnologies.azuremanagementutil.model.StorageServices;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import com.microsoftopentechnologies.intellij.runnable.AccountActionRunnable;
import com.microsoftopentechnologies.intellij.runnable.CacheAccountWithProgressBar;
import com.microsoftopentechnologies.intellij.runnable.LoadAccountWithProgressBar;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.AzureSettings;
import com.microsoftopentechnologies.intellij.ui.util.UIUtils;

import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import org.jetbrains.annotations.NotNull;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

/**
 * Class has common methods which
 * handles publish settings file and extract data.
 * Methods get called whenever user clicks
 * "Import from publish settings file..." button
 * on publish wizard or preference page dialog.
 */
public class MethodUtils {
	/**
	 * Method checks file selected by user is valid
	 * and call method which extracts data from it.
	 */
	public static void handleFile(String fileName, Project project) {
		if (fileName != null && !fileName.isEmpty()) {
			File file = new File(fileName);
			PublishData publishDataToCache = handlePublishSettings(file, project);
			if (publishDataToCache == null) {
				return;
			}
			WizardCacheManager.setCurrentPublishData(publishDataToCache);
			// Make centralized storage registry.
			prepareListFromPublishData(project);
		}
	}

	/**
	 * Method extracts data from publish settings file.
	 */
	public static PublishData handlePublishSettings(File file, Project project) {
		PublishData data = UIUtils.createPublishDataObj(file);
		/*
		 * If data is equal to null,
		 * then publish settings file already exists.
		 * So don't load information again.
		 */
		if (data != null) {
            AccountActionRunnable settings = new CacheAccountWithProgressBar(file, data, message("loadingCred"));
            ProgressManager.getInstance().runProcessWithProgressSynchronously(settings, "Loading Account Settings...", true, project);
            AzureSettings.getSafeInstance(project).savePublishDatas();
		}
		return data;
	}

	/**
	 * Method prepares storage account list.
	 * Adds data from publish settings file.
	 */
	public static void prepareListFromPublishData(Project project) {
		List<StorageAccount> strgList = StorageAccountRegistry.getStrgList();
		Collection<PublishData> publishDatas = WizardCacheManager.getPublishDatas();
		for (PublishData pd : publishDatas) {
			for (Subscription sub : pd.getPublishProfile().getSubscriptions()) {
				/*
				 * Get collection of storage services in each subscription.
				 */
				StorageServices services = pd.getStoragesPerSubscription().get(sub.getId());
				// iterate over collection of services.
				for (StorageService strgService : services) {
					StorageAccount strEle = new StorageAccount(
									strgService.getServiceName(),
									strgService.getPrimaryKey(),
									strgService.getStorageAccountProperties().
									getEndpoints().get(0).toString());
					/*
					 * Check if storage account is already present
					 * in centralized repository,
					 * if present then do not add.
					 * if not present then check
					 * access key is valid or not.
					 * If not then update with correct one in registry. 
					 */
					if (strgList.contains(strEle)) {
						int index = strgList.indexOf(strEle);
						StorageAccount account = strgList.get(index);
						String newKey = strEle.getStrgKey();
						if (!account.getStrgKey().equals(newKey)) {
							account.setStrgKey(newKey);
						}
					} else {
						strgList.add(strEle);
					}
				}
			}
		}
        AzureSettings.getSafeInstance(project).saveStorage();
	}

    /**
     * When we start new session,
     * reload the subscription and storage account
     * registry information just for once.
     */
    public static void loadSubInfoFirstTime(final Project project) {
        Task.Backgroundable task = new Task.Backgroundable(project, "Loading Account Settings...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                new LoadAccountWithProgressBar(project).run();
            }
        };
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
        prepareListFromPublishData(project);
    }
}
