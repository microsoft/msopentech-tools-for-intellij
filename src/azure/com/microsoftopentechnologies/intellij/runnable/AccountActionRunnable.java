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

import java.util.concurrent.atomic.AtomicBoolean;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.microsoftopentechnologies.deploy.tasks.AccountCachingExceptionEvent;
import com.microsoftopentechnologies.deploy.tasks.LoadingAccoutListener;
import com.microsoftopentechnologies.deploy.util.PublishData;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public abstract class AccountActionRunnable implements Runnable, LoadingAccoutListener {

	protected PublishData data;

   ProgressIndicator progressIndicator;
	
	protected final AtomicBoolean wait = new AtomicBoolean(true);
	protected final AtomicBoolean error = new AtomicBoolean(false);
	
	private int numberOfAccounts = 1;
	protected Exception exception;
	protected String errorMessage;

	public abstract void doTask();

	public AccountActionRunnable(PublishData data) {
		this.data = data;
	}
	
	public void setNumberOfAccounts(int num) {
		this.numberOfAccounts = num;
	}

    @Override
    public void run() {
        this.progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        setIndicatorText();

        doTask();
        if (error.get()) {
            progressIndicator.cancel();
            PluginUtil.displayErrorDialogInAWTAndLog(message("error"), errorMessage, exception);
		}
    }

    void setIndicatorText() {
        progressIndicator.setText("Loading Account Settings...");
        progressIndicator.setText2("Subscriptions");
    }

	@Override
	public synchronized void onLoadedSubscriptions() {
		setWorked(1.0 / (4 * numberOfAccounts));
        progressIndicator.setText2("Storage Services, Cloud Services and Locations");
	}

	@Override
	public void onLoadedStorageServices() {
        setWorked(1.0 / (4 * numberOfAccounts));
	}

	@Override
	public void onLoadedHostedServices() {
        setWorked(1.0 / (4 * numberOfAccounts));
	}

	@Override
	public void onLoadedLocations() {
        setWorked(1.0 / (4 * numberOfAccounts));
	}
	
	@Override
	public void onRestAPIError(AccountCachingExceptionEvent e) {
		wait.set(false);
		error.set(true);
		exception = e.getException();
		errorMessage = e.getMessage();
	}
	
	private synchronized void setWorked(double work) {
        progressIndicator.setFraction(progressIndicator.getFraction() + work);
	}
}
