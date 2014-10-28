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
package com.microsoftopentechnologies.intellij.util;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.microsoftopentechnologies.intellij.AzurePlugin;

import java.io.*;
import java.net.URL;
import java.util.Scanner;

import static com.microsoftopentechnologies.intellij.AzurePlugin.log;
import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class WAHelper {
    /**
     * @return resource filename in plugin's directory
     */
    public static String getTemplateFile(String fileName) {
        return String.format("%s%s%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.PLUGIN_ID, File.separator, fileName);
    }

    /**
     * This API compares if two files content is identical. It ignores extra
     * spaces and new lines while comparing
     *
     * @param sourceFile
     * @param destFile
     * @return
     * @throws Exception
     */
    public static boolean isFilesIdentical(URL sourceFile, File destFile)
            throws Exception {
        try {
            Scanner sourceFileScanner = new Scanner(sourceFile.openStream());
            Scanner destFileScanner = new Scanner(destFile);

            while (sourceFileScanner.hasNext()) {
				/*
				 * If source file is having next token then destination file
				 * also should have next token, else they are not identical.
				 */
                if (!destFileScanner.hasNext()) {
                    destFileScanner.close();
                    sourceFileScanner.close();
                    return false;
                }
                if (!sourceFileScanner.next().equals(destFileScanner.next())) {
                    sourceFileScanner.close();
                    destFileScanner.close();
                    return false;
                }
            }
			/*
			 * Handling the case where source file is empty and destination file
			 * is having text
			 */
            if (destFileScanner.hasNext()) {
                destFileScanner.close();
                sourceFileScanner.close();
                return false;
            } else {
                destFileScanner.close();
                sourceFileScanner.close();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } /*finally {
            sourceFile.close();
        }*/
    }

    /**
     * Copy file from source to destination.
     *
     * @param source
     * @param destination
     * @throws Exception
     */
    public static void copyFile(String source, String destination)
            throws Exception {
        try {
            File f1 = new File(source);
            File f2 = new File(destination);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * Returns path of deploy folder.
     *
     * @param waProjMngr
     * @param module
     * @return
     */
    public static String getDeployFolderPath(WindowsAzureProjectManager waProjMngr, Module module) {
        String dplyFolderPath = "";
        try {
            String dplyFldrName = waProjMngr.getPackageDir();
            String modulePath = PluginUtil.getModulePath(module);

            if (dplyFldrName.startsWith(".")) {
                dplyFldrName = dplyFldrName.substring(1);
            }
            dplyFolderPath = String.format("%s%s", modulePath, dplyFldrName);
        } catch (Exception e) {
            AzurePlugin.log(e.getMessage(), e);
        }
        return dplyFolderPath;
    }

    public static WindowsAzureRole prepareRoleToAdd(WindowsAzureProjectManager waProjManager) {
        WindowsAzureRole windowsAzureRole = null;
        try {
            StringBuffer strBfr = new StringBuffer(message("dlgWorkerRole1"));
            int roleNo = 2;
            while (!waProjManager.isAvailableRoleName(strBfr.toString())) {
                strBfr.delete(10, strBfr.length());
                strBfr.append(roleNo++);
            }
            String strKitLoc = WAHelper.getTemplateFile(message("pWizStarterKit"));
            windowsAzureRole = waProjManager.addRole(strBfr.toString(), strKitLoc);
            windowsAzureRole.setInstances(message("rolsNoOfInst"));
            windowsAzureRole.setVMSize(message("rolsVMSmall"));
        } catch (Exception e) {
            log(e.getMessage());
        }
        return windowsAzureRole;
    }
}
