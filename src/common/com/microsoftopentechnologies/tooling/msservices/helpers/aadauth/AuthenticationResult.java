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

package com.microsoftopentechnologies.tooling.msservices.helpers.aadauth;

import java.io.Serializable;

/**
 * Contains the results of one token acquisition operation.
 */
public final class AuthenticationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String accessTokenType;
    private final long expiresOn;
    private final UserInfo userInfo;
    private final String accessToken;
    private final String refreshToken;

    public AuthenticationResult(final String accessTokenType,
            final String accessToken, final String refreshToken,
            final long expiresOn, final UserInfo userInfo) {
        this.accessTokenType = accessTokenType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresOn = expiresOn;
        this.userInfo = userInfo;
    }

    public String getAccessTokenType() {
        return accessTokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresOn() {
        return expiresOn;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public static boolean equals(AuthenticationResult o1, AuthenticationResult o2) {
        if(o1 != null) {
            return o1.equals(o2);
        }
        if(o2 != null) {
            return o2.equals(o1);
        }

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        AuthenticationResult other = (AuthenticationResult)obj;

        // if the access tokens are not the same these are not the same objects
        if (accessToken == null && other.accessToken != null) {
            return false;
        }

        return accessToken.equals(other.accessToken);
    }
}
