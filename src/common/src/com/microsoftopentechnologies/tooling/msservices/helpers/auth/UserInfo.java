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
package com.microsoftopentechnologies.tooling.msservices.helpers.auth;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;

public class UserInfo {
    private final String tenantId;
    private final String uniqueName;

    public UserInfo(@NotNull String tenantId, @NotNull String uniqueName) {
        this.tenantId = tenantId;
        this.uniqueName = uniqueName;
    }

    @NotNull
    public String getTenantId() {
        return this.tenantId;
    }

    @NotNull
    public String getUniqueName() {
        return this.uniqueName;
    }

    @Override
    public boolean equals(Object otherObj) {
        if (otherObj != null && otherObj instanceof UserInfo) {
            UserInfo other = (UserInfo) otherObj;
            return tenantId.equals(other.tenantId) && uniqueName.equals(other.uniqueName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return tenantId.hashCode() * 13 + uniqueName.hashCode();
    }
}