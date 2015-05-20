/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.tooling.msservices.helpers;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExternalStorageHelper {
    private static final String EXTERNAL_STORAGE_LIST = "EXTERNAL_STORAGE_LIST";

    public static List<ClientStorageAccount> getList() {
        List<ClientStorageAccount> list = new ArrayList<ClientStorageAccount>();

        String[] storageArray = DefaultLoader.getIdeHelper().getProperties(EXTERNAL_STORAGE_LIST);
        if (storageArray != null) {

            for (String json : storageArray) {
                ClientStorageAccount clientStorageAccount = new Gson().fromJson(json, ClientStorageAccount.class);
                list.add(clientStorageAccount);
            }

        }

        return list;
    }

    public static void add(ClientStorageAccount clientStorageAccount) {
        String json = new Gson().toJson(clientStorageAccount);

        String[] values = DefaultLoader.getIdeHelper().getProperties(EXTERNAL_STORAGE_LIST);

        ArrayList<String> list = new ArrayList<String>();
        if (values != null) {
            list.addAll(Arrays.asList(values));
        }

        list.add(json);

        DefaultLoader.getIdeHelper().setProperties(EXTERNAL_STORAGE_LIST, list.toArray(new String[list.size()]));
    }

    public static void detach(ClientStorageAccount clientStorageAccount) {
        String[] storageArray = DefaultLoader.getIdeHelper().getProperties(EXTERNAL_STORAGE_LIST);

        if (storageArray != null) {
            ArrayList<String> storageList = Lists.newArrayList(storageArray);

            for (String json : storageArray) {
                ClientStorageAccount csa = new Gson().fromJson(json, ClientStorageAccount.class);

                if (csa.getName().equals(clientStorageAccount.getName())) {
                    storageList.remove(json);
                }
            }

            if (storageList.size() == 0) {
                DefaultLoader.getIdeHelper().unsetProperty(EXTERNAL_STORAGE_LIST);
            } else {
                DefaultLoader.getIdeHelper().setProperties(EXTERNAL_STORAGE_LIST,
                        storageList.toArray(new String[storageList.size()]));
            }
        }
    }
}