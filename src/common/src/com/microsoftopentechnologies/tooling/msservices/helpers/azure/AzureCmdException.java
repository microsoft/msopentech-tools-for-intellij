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
package com.microsoftopentechnologies.tooling.msservices.helpers.azure;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AzureCmdException extends Exception {
    private String mErrorLog;

    public AzureCmdException(String message) {
        super(message);

        mErrorLog = "";
    }

    public AzureCmdException(String message, String errorLog) {
        super(message);

        mErrorLog = errorLog;
    }

    public AzureCmdException(String message, Throwable throwable) {
        super(message, throwable);

        if (throwable instanceof AzureCmdException) {
            mErrorLog = ((AzureCmdException) throwable).getErrorLog();
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);

            throwable.printStackTrace(writer);
            writer.flush();

            mErrorLog = sw.toString();
        }
    }

    public String getErrorLog() {
        return mErrorLog;
    }
}