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
package com.microsoftopentechnologies.intellij.ui.util;

import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;

import java.util.Arrays;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;

public class JdkSrvConfigListener extends JdkSrvConfig {
    /**
     * Method decides whether to
     * show third party JDK names or not.
     *
     * @param status
     */
    public static String[] getThirdPartyJdkNames(Boolean status, String depJdkName) {
        if (status) {
            try {
                String[] thrdPrtJdkArr = WindowsAzureProjectManager.getThirdPartyJdkNames(cmpntFile, depJdkName);
                // check at least one element is present
                return thrdPrtJdkArr;
            } catch (WindowsAzureInvalidProjectOperationException e) {
                log(e.getMessage());
            }
        }
        return new String[]{};
    }

    public static String[] getServerList() {
        try {
            String[] servList = WindowsAzureProjectManager.getServerTemplateNames(cmpntFile);
            Arrays.sort(servList);
            return servList;
        } catch (WindowsAzureInvalidProjectOperationException e) {
            log(e.getMessage());
        }
        return new String[]{};
    }
}
