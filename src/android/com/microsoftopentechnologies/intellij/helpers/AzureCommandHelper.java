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

package com.microsoftopentechnologies.intellij.helpers;

import com.intellij.openapi.util.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

@Deprecated
public class AzureCommandHelper {

    static AzureCommandHelper mInstance;
    public static String AzureMisssingError = "Windows Azure Command Line Interface not found.";
    public static String OldVersion = "Windows Azure Command Line Interface version is older that the minimum supported (0.8.2)";

    public static synchronized AzureCommandHelper getInstance() throws AzureCmdException  {
        if(null == mInstance){
            mInstance = new AzureCommandHelper();
        }
        return mInstance;
    }

    private String mOsVersion;
    private String mAzureBinPath;


    protected AzureCommandHelper() throws AzureCmdException {
        try {
            mOsVersion = System.getProperty("os.name");
            String osName = mOsVersion.split(" ")[0];
            if(osName.equals("Windows"))
            {
                if(mOsVersion.split(" ")[1].equals("XP")) //if is Windows XP
                {
                    String path = System.getenv("PATH");

                    if(path == null || path.isEmpty())
                        throw new AzureCmdException(AzureMisssingError, "");

                    String[] paths = path.split(";");
                    for(int i = 0; i != paths.length; i++)
                    {
                        if(paths[i].indexOf("Windows Azure") > 0 && paths[i].indexOf("CLI") > 0)
                            mAzureBinPath = paths[i].replace("wbin","bin");
                    }

                }
                else //if windows server 2003 or higher
                {
                    String cmd = "where azure";
                    Process p = Runtime.getRuntime().exec(cmd);


                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));


                    String path = reader.readLine();

                    if(path == null || path.isEmpty())
                        throw new AzureCmdException(AzureMisssingError, "");

                    //if windows server 2003 or higher
                    path = path.replace("wbin","bin");
                    mAzureBinPath = path.replace("\\azure.cmd","");
                }
            }
            else
            {
                String cmd = "which azure";
                Process p = Runtime.getRuntime().exec(cmd);


                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        p.getInputStream()));

                mAzureBinPath = reader.readLine();

                if(mAzureBinPath == null || mAzureBinPath.isEmpty()) {
                    String defaultLocation = "/usr/local/bin/azure";
                    if(new File(defaultLocation).exists())
                        mAzureBinPath = defaultLocation;
                    else
                        throw new AzureCmdException(AzureMisssingError, "");
                }

                mAzureBinPath = mAzureBinPath.substring(0, mAzureBinPath.lastIndexOf("azure"));
            }
        } catch (IOException ex) {
            throw new AzureCmdException("Error accessing Windows Azure command line.", ex.getMessage());
        }
    }

    public String consoleExec(String[] args) throws AzureCmdException {

        ArrayList<String> cmdList = new ArrayList<String>(args.length + 1);
        String[] env = null;

        if(mOsVersion.split(" ")[0].equals("Windows")){
            cmdList.add(mAzureBinPath + "\\node");
            cmdList.add(mAzureBinPath + "\\azure.js");

            ArrayList<String> tempenvlist = new ArrayList<String>();
            for(String envval : System.getenv().keySet())
                tempenvlist.add(String.format("%s=%s", envval, System.getenv().get(envval)));

            tempenvlist.add("PRECOMPILE_STREAMLINE_FILES=1");
            env = new String[tempenvlist.size()];
            tempenvlist.toArray(env);
        } else {
            cmdList.add(mAzureBinPath + "azure");
        }

        Collections.addAll(cmdList, args);

        final Process p;
        try {
            Runtime runtime = Runtime.getRuntime();
            p = runtime.exec(
                    cmdList.toArray(new String[args.length + 1]),
                    env, //new String[] {"PRECOMPILE_STREAMLINE_FILES=1"},
                    new File(System.getProperty("java.io.tmpdir")));

            String response = convertStreamToString(p.getInputStream());
            String errResponse = convertStreamToString(p.getErrorStream());

            if(p.waitFor() == 0) {
                return response;
            } else {

                AzureCmdException ex = new AzureCmdException("Error executing command \n", errResponse);
                ex.printStackTrace();

                throw ex;
            }
        } catch (IOException ex) {
            throw new AzureCmdException("Error accessing Windows Azure command line results.", ex.getLocalizedMessage());
        } catch (InterruptedException ex) {
            throw new AzureCmdException("Error accessing Windows Azure command line results.", ex.getLocalizedMessage());
        }
    }

    private String convertStreamToString(java.io.InputStream is) throws IOException {
        return new String(FileUtil.adaptiveLoadBytes(is));
    }
}
