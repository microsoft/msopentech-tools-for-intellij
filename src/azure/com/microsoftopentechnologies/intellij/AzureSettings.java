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
package com.microsoftopentechnologies.intellij;

import com.microsoftopentechnologies.azurecommons.deploy.tasks.LoadingAccoutListener;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.intellij.wizards.WizardCacheManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.xmlbeans.impl.util.Base64;

import java.io.*;
import java.util.*;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

@State(
        name = "AzureSettings",
        storages = {
                @Storage(id = "AzureSettings", file = "$PROJECT_FILE$"),
                @Storage(file = "$PROJECT_CONFIG_DIR$/azureSettings.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public class AzureSettings implements PersistentStateComponent<AzureSettings.State> {

    private State myState = new State();

    private boolean subscriptionLoaded;

    public static AzureSettings getSafeInstance(Project project) {
        AzureSettings settings = ServiceManager.getService(project, AzureSettings.class);
        return settings != null ? settings : new AzureSettings();
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public void loadStorage() {
        try {
            if (myState.storageAccount != null) {
                byte[] data = Base64.decode(myState.storageAccount.getBytes());
                ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                ObjectInput input = new ObjectInputStream(buffer);
                try {
                    StorageAccount[] storageAccs = (StorageAccount[]) input.readObject();
                    for (StorageAccount str : storageAccs) {
                        if (!StorageAccountRegistry.getStrgList().contains(str)) {
                            StorageAccountRegistry.getStrgList().add(str);
                        }
                    }
                } finally {
                    input.close();
                }
            }
        } catch (Exception e) {
            log(message("err"), e);
        }
    }

    public void loadPublishDatas(LoadingAccoutListener listener) {
        try {
            if (myState.publishProfile != null) {
                byte[] data = Base64.decode(myState.publishProfile.getBytes());
                ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                ObjectInput input = new ObjectInputStream(buffer);
                try {
                    PublishData[] publishDatas = (PublishData[]) input.readObject();
                    listener.setNumberOfAccounts(publishDatas.length);
                    for (PublishData pd : publishDatas) {
                        try {
                            WizardCacheManager.cachePublishData(null, pd, listener);
                        } catch (RestAPIException e) {
                            log(message("error"), e);
                        }
                    }
                } finally {
                    input.close();
                }
            }
        } catch (IOException e) {
            log(message("error"),e);
        } catch (ClassNotFoundException e) {
            log(message("error"),e);
        }
    }

    public void saveStorage() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(buffer);
            List<StorageAccount> data = StorageAccountRegistry.getStrgList();
			/*
			 * Sort list according to storage account name.
			 */
            Collections.sort(data);
            StorageAccount[] dataArray = new StorageAccount[data.size()];
            int i = 0;
            for (StorageAccount pd1 : data) {
                dataArray[i] = pd1;
                i++;
            }
            try {
                output.writeObject(dataArray);
            } finally {
                output.close();
            }
            myState.storageAccount = new String(Base64.encode(buffer.toByteArray()));
        } catch (IOException e) {
            log(message("err"), e);
        }
    }

    public void savePublishDatas() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(buffer);
            Collection<PublishData> data = WizardCacheManager.getPublishDatas();
            PublishData[] dataArray = new PublishData[data.size()];
            int i = 0;
            for (PublishData pd1 : data) {
                dataArray[i] = new PublishData();
                dataArray[i++].setPublishProfile(pd1.getPublishProfile());
            }
            try {
                output.writeObject(dataArray);
            } finally {
                output.close();
            }
            myState.publishProfile = new String(Base64.encode(buffer.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSubscriptionLoaded() {
        return subscriptionLoaded;
    }

    public void setSubscriptionLoaded(boolean subscriptionLoaded) {
        this.subscriptionLoaded = subscriptionLoaded;
    }

    public static class State {
        public String storageAccount;
        public String publishProfile;
        public Map<String, String> deployCache = new HashMap<String, String>();
    }
}
