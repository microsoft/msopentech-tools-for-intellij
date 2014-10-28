/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoftopentechnologies.intellij.helpers.o365;

import com.microsoft.services.odata.interfaces.Credentials;
import com.microsoft.services.odata.interfaces.Request;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;

public class ADCredentials implements Credentials {
    private AuthenticationResult token;

    public ADCredentials(AuthenticationResult token) {
        if(token == null) {
            throw new IllegalArgumentException("token is null");
        }

        this.token = token;
    }

    @Override
    public void prepareRequest(Request request) {
        if(request == null) {
            throw new IllegalArgumentException("request");
        }

        // add an "Authorization" header to this request with the access token
        if(token != null) {
            request.addHeader("Authorization", "Bearer " + token.getAccessToken());
        }
    }
}
