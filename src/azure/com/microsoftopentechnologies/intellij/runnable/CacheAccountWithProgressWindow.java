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

import java.io.File;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.microsoftopentechnologies.deploy.util.PublishData;
import com.microsoftopentechnologies.intellij.util.PluginUtil;

public class CacheAccountWithProgressWindow extends AccountActionRunnable implements Runnable {

	private String message;
	private boolean isCompletedSuccesfully;
    private final File publishSettingsFile;
	
	public boolean isCompletedSuccesfully() {
		return isCompletedSuccesfully;
	}
	
	public CacheAccountWithProgressWindow(File publishSettingsFile, PublishData data, String message) {
		super(data);
		this.message = message;
        this.publishSettingsFile = publishSettingsFile;
	}

	@Override
	public void doTask() {
		try {
			WizardCacheManager.cachePublishData(publishSettingsFile, data, this);
            isCompletedSuccesfully = true;
		} catch (Exception e) {
            PluginUtil.displayErrorDialogInAWTAndLog(message, message, e);
            isCompletedSuccesfully = false;
        }
    }
}