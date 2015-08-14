/**
 * Copyright 2015 Microsoft Open Technologies Inc.
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
package com.microsoftopentechnologies.intellij.ui.libraries;

public class AzureLibrary {
    public static AzureLibrary ACS_FILTER = new AzureLibrary("Azure Access Control Services Filter (by MS Open Tech)",
            "ACSAuthFilter.jar", new String[] {"ACSAuthFilter.jar"});
    public static AzureLibrary QPID_CLIENT = new AzureLibrary("Package for Apache Qpid Client Libraries for JMS (by MS Open Tech)",
            "com.microsoftopentechnologies.qpid", new String[]{});
    public static AzureLibrary AZURE_LIBRARIES = new AzureLibrary("Package for Microsoft Azure Libraries for Java (by MS Open Tech)",
            "com.microsoftopentechnologies.windowsazure.tools.sdk",
            new String[]{
                    "azure-core-0.7.0.jar",
                    "azure-management-0.7.0.jar",
                    "azure-management-compute-0.7.0.jar",
                    "azure-management-network-0.7.0.jar",
                    "azure-management-storage-0.7.0.jar",
                    "azure-storage-3.0.0.jar",
                    "commons-codec-1.6.jar",
                    "commons-codec-1.7.jar",
                    "commons-lang3-3.4.jar",
                    "commons-logging-1.1.3.jar",
                    "guava-18.0.jar",
                    "httpclient-4.3.5.jar",
                    "httpcore-4.3.2.jar",
                    "jackson-core-2.6.0.jar",
                    "jackson-core-asl-1.9.2.jar",
                    "jackson-jaxrs-1.9.2.jar",
                    "jackson-mapper-asl-1.9.2.jar",
                    "jackson-xc-1.9.2.jar",
                    "javax.inject-1.jar",
                    "jcip-annotations-1.0.jar",
                    "jersey-client-1.13.jar",
                    "jersey-core-1.13.jar",
                    "jersey-json-1.13.jar",
                    "jettison-1.1.jar",
                    "mail-1.4.5.jar"
            });
    public static AzureLibrary[] LIBRARIES = new AzureLibrary[]{ACS_FILTER, QPID_CLIENT, AZURE_LIBRARIES};

    private String name;
    private String location;
    private String[] files;

    public AzureLibrary(String name, String location, String[] files) {
        this.name = name;
        this.location = location;
        this.files = files;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String[] getFiles() {
        return files;
    }
}
