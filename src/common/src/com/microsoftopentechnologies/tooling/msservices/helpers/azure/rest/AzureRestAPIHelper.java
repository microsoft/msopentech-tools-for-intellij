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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;

public class AzureRestAPIHelper {
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static boolean existsMobileService(String name) {
        try {
            URL myUrl = new URL("https://" + name + ".azure-mobile.net");
            HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();

            int responseCode = conn.getResponseCode();
            return (responseCode >= 200 && responseCode < 300);
        } catch (Exception e) {
            return false;
        }
    }
}