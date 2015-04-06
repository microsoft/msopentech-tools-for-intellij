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

package com.microsoftopentechnologies.intellij.helpers.aadauth;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class ADJarLoader {
    private static final String BASE_URL = "http://msopentechrelease.blob.core.windows.net/msopentechtools/";
    private static FileCache filesCache;
    private static String jarName;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");
        boolean isMac = osName.contains("mac");
        boolean isLinux = osName.contains("linux");
        boolean isx64 = System.getProperty("os.arch").contains("64");

        // this name should finally look something like this:
        //  ad-interactive-auth-linux-x64.jar
        jarName = "ad-interactive-auth-" +
                (isWindows ? "win32-" :
                    isMac ? "osx-" :
                    isLinux ? "linux-" : "") +
                (isx64 ? "x64" : "x86") +
                ".jar";
    }

    public static File load() throws ExecutionException, MalformedURLException {
        if(filesCache == null) {
            filesCache = new FileCache(new FileSource[] {
                new FileSource(jarName, new URL(BASE_URL + jarName))
            });
        }

        return filesCache.getFile(jarName);
    }
}
