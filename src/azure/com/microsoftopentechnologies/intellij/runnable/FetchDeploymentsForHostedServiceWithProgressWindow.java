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

package com.microsoftopentechnologies.intellij.runnable;

import com.microsoft.windowsazure.management.compute.models.HostedServiceGetDetailedResponse;
import com.microsoftopentechnologies.azurecommons.deploy.tasks.AccountCachingExceptionEvent;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;
import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class FetchDeploymentsForHostedServiceWithProgressWindow extends AccountActionRunnable implements Runnable {

	private String hostedServiceName;
	private HostedServiceGetDetailedResponse hostedService;
	
	public FetchDeploymentsForHostedServiceWithProgressWindow(PublishData data, String hostedServiceName) {
		super(data);
        this.hostedServiceName = hostedServiceName;
	}

    void setIndicatorText() {
        progressIndicator.setText("Fetching Deployments For " + hostedServiceName);
    }

	@Override
	public void doTask() {
		try {
			this.hostedService = WizardCacheManager.getHostedServiceWithDeployments(hostedServiceName);
		} catch (Exception e) {
			AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
			event.setException(e);
			event.setMessage(e.getMessage());
			onRestAPIError(event);
			log(message("fetchingDeploymentsTitle"), e);
		}
    }

	public HostedServiceGetDetailedResponse getHostedServiceDetailed() {
		return hostedService;
	}
}
