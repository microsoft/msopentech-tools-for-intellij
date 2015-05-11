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

import com.microsoftopentechnologies.tooling.msservices.helpers.StringHelper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Map;

/**
 * Contains information of a single user.
 */
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String userId;
    private final String givenName;
    private final String familyName;
    private final String identityProvider;
    private final String upn;
    private final String uniqueName;

    private UserInfo(final String userId,
                     final String givenName,
                     final String familyName,
                     final String identityProvider,
                     final String upn,
                     final String uniqueName) {
        this.userId = userId;
        this.givenName = givenName;
        this.familyName = familyName;
        this.identityProvider = identityProvider;
        this.upn = upn;
        this.uniqueName = uniqueName;
    }

    public static UserInfo parse(String idtoken) throws ParseException {
        if(StringHelper.isNullOrWhiteSpace(idtoken)) {
            return null;
        }

        JWT jwt = JWTParser.parse(idtoken);
        ReadOnlyJWTClaimsSet claims = jwt.getJWTClaimsSet();
        Map<String, Object> customClaims = claims.getCustomClaims();

        return new UserInfo(
                (String)customClaims.get(IdTokenClaim.ObjectId),
                (String)customClaims.get(IdTokenClaim.GivenName),
                (String)customClaims.get(IdTokenClaim.FamilyName),
                (String)customClaims.get(IdTokenClaim.IdentityProvider),
                (String)customClaims.get(IdTokenClaim.UPN),
                (String)customClaims.get(IdTokenClaim.UniqueName));
    }

    /**
     * Get user id
     * 
     * @return String value
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get given name
     * 
     * @return String value
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * Get family name
     * 
     * @return String value
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Get identity provider
     * 
     * @return String value
     */
    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getUpn() {
        return upn;
    }

    public String getUniqueName() {
        return uniqueName;
    }
}
