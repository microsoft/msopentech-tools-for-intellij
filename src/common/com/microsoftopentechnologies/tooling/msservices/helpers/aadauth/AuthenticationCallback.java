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

/**
 * Authentication callback Interface that can be implemented by the developer.
 */
public interface AuthenticationCallback {

    /**
     * Executed on success.
     * 
     * @param result
     *            authentication result for further processing
     */
    void onSuccess(AuthenticationResult result);

    /**
     * Executed on failure.
     * 
     * @param exc
     *            Throwable exception to be handled.
     */
    void onFailure(Throwable exc);
}
