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
package com.microsoftopentechnologies.intellij.runnable;

import com.microsoft.windowsazure.management.storage.models.StorageAccountCreateParameters;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.microsoftopentechnologies.deploy.tasks.AccountCachingExceptionEvent;
import com.microsoftopentechnologies.model.StorageService;
import com.microsoftopentechnologies.deploy.util.PublishData;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class NewStorageAccountWithProgressWindow extends AccountActionRunnable implements Runnable {

	private StorageAccountCreateParameters body;
	private StorageService storageService;

	public NewStorageAccountWithProgressWindow(PublishData data, StorageAccountCreateParameters body) {
		super(data);
        this.body = body;
	}

    void setIndicatorText() {
        progressIndicator.setText(message("crtStrgAcc") + body.getName() + message("takeMinLbl"));
    }

	public StorageService getStorageService() {
		return storageService;
	}

	@Override
	public void doTask() {
        try {
            storageService = WizardCacheManager.createStorageAccount(body);
        } catch (Exception e) {
            AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
            event.setException(e);
            event.setMessage(e.getMessage());
            onRestAPIError(event);
            log(message("createStorageAccountFailedTitle"), e);
        }
    }
}
