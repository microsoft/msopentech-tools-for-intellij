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

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import sun.misc.IOUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;


public class AndroidStudioHelper {
    private static final String mobileServicesTemplateName = "AzureServicesActivity";
    private static final String officeTemplateName = "Office365Activity";

    public static boolean isAndroidStudio() {
        return ApplicationInfo.getInstance().getVersionName().startsWith("Android Studio");
    }

    public static void newActivityTemplateManager(boolean deleteTemplates, Object caller) throws IOException, InterruptedException {
        String templatePath = URLDecoder.decode(ApplicationComponent.class.getResource("").getPath().replace("file:/", ""), "UTF-8");
        templatePath = templatePath.replace("/", File.separator);
        templatePath = templatePath.substring(0, templatePath.indexOf(File.separator + "lib"));
        templatePath = templatePath + File.separator + "plugins" + File.separator + "android" + File.separator;
        templatePath = templatePath + "lib" + File.separator + "templates" + File.separator + "activities" + File.separator;

        String[] env = null;

        if(deleteTemplates || !new File(templatePath + mobileServicesTemplateName).exists()) {
            String tmpDir = getTempLocation();
            copyResourcesRecursively(new File(tmpDir));

            tmpDir = tmpDir + "MobileServiceTemplate" + File.separator;

            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                try {

                    if(deleteTemplates) {
                        VirtualFile mobileTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + mobileServicesTemplateName));
                        VirtualFile officeTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + officeTemplateName));

                        if(mobileTemplate != null)
                            mobileTemplate.delete(caller);

                        if(officeTemplate != null)
                            officeTemplate.delete(caller);

                    }

                    copyFolder(new File(tmpDir + mobileServicesTemplateName), new File(templatePath + mobileServicesTemplateName));
                    copyFolder(new File(tmpDir + officeTemplateName), new File(templatePath + officeTemplateName));

                } catch (IOException ex) {
                    PrintWriter printWriter = new PrintWriter(tmpDir + "\\script.bat");
                    printWriter.println("@echo off");

                    if(deleteTemplates) {
                        printWriter.println("rd \"" + templatePath + mobileServicesTemplateName + "\" /Q /S");
                        printWriter.println("rd \"" + templatePath + officeTemplateName + "\" /Q /S");
                    }

                    printWriter.println("md \"" + templatePath + mobileServicesTemplateName + "\"");
                    printWriter.println("md \"" + templatePath + officeTemplateName + "\"");
                    printWriter.println("xcopy \"" + tmpDir + mobileServicesTemplateName + "\" \"" + templatePath + mobileServicesTemplateName + "\" /s /i /Y");
                    printWriter.println("xcopy \"" + tmpDir + officeTemplateName + "\" \"" + templatePath + officeTemplateName + "\" /s /i /Y");
                    printWriter.flush();
                    printWriter.close();

                    String[] tmpcmd = {
                         "cmd",
                        "/c",
                        tmpDir + "elevate.exe "
                    };

                    ArrayList<String> tempenvlist = new ArrayList<String>();
                    for(String envval : System.getenv().keySet())
                        tempenvlist.add(String.format("%s=%s", envval, System.getenv().get(envval)));

                    tempenvlist.add("PRECOMPILE_STREAMLINE_FILES=1");
                    env = new String[tempenvlist.size()];
                    tempenvlist.toArray(env);

                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec(tmpcmd, env, new File(tmpDir));
                    proc.waitFor();

                    //wait for elevate command to finish
                    Thread.sleep(3000);

                    if(!new File(templatePath + mobileServicesTemplateName).exists())
                        UIHelper.showException("Error copying template files. Please refer to documentation to copy manually.", new Exception());
                }
            } else if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {

                String[] deleteAndCopy = {
                        "osascript",
                        "do shell script \"rm -r \\\"/" + templatePath + mobileServicesTemplateName + "\\\"\"",
                        "-e",
                        "do shell script \"rm -r \\\"/" + templatePath + officeTemplateName + "\\\"\"",
                        "-e",
                        "do shell script \"cp -Rp \\\"" + tmpDir + mobileServicesTemplateName + "\\\" \\\"/" + templatePath + "\\\"\"",
                        "-e",
                        "do shell script \"cp -Rp \\\"" + tmpDir + officeTemplateName + "\\\" \\\"/" + templatePath + "\\\"\""
                };

                String[] copy = {
                        "osascript",
                        "-e",
                        "do shell script \"cp -Rp \\\"" + tmpDir + mobileServicesTemplateName + "\\\" \\\"/" + templatePath + "\\\"\"",
                        "-e",
                        "do shell script \"cp -Rp \\\"" + tmpDir + officeTemplateName + "\\\" \\\"/" + templatePath + "\\\"\""
                };

                exec(deleteTemplates ? deleteAndCopy : copy, tmpDir);
            } else {
                try {

                    if(deleteTemplates) {
                        VirtualFile mobileTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + mobileServicesTemplateName));
                        VirtualFile officeTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + officeTemplateName));

                        if(mobileTemplate != null)
                            mobileTemplate.delete(caller);

                        if(officeTemplate != null)
                            officeTemplate.delete(caller);

                    }

                    copyFolder(new File(tmpDir + mobileServicesTemplateName), new File(templatePath + mobileServicesTemplateName));
                    copyFolder(new File(tmpDir + officeTemplateName), new File(templatePath + officeTemplateName));


                } catch (IOException ex) {

                    JPasswordField pf = new JPasswordField();
                    int okCxl = JOptionPane.showConfirmDialog(null, pf, "To copy Microsoft Services templates, the plugin needs your password:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (okCxl == JOptionPane.OK_OPTION) {
                        String password = new String(pf.getPassword());


                        exec(new String[]{
                                "echo",
                                password,
                                "|",
                                "sudo",
                                "-S",
                                "rm",
                                "-r",
                                tmpDir + mobileServicesTemplateName,
                                templatePath + mobileServicesTemplateName
                        }, tmpDir);


                        exec(new String[]{
                                "echo",
                                password,
                                "|",
                                "sudo",
                                "-S",
                                "rm",
                                "-r",
                                tmpDir + officeTemplateName,
                                templatePath + officeTemplateName
                        }, tmpDir);


                        exec(new String[]{
                                "echo",
                                password,
                                "|",
                                "sudo",
                                "-S",
                                "cp",
                                "-Rp",
                                tmpDir + mobileServicesTemplateName,
                                templatePath + mobileServicesTemplateName
                        }, tmpDir);


                        exec(new String[]{
                                "echo",
                                password,
                                "|",
                                "sudo",
                                "-S",
                                "cp",
                                "-Rp",
                                tmpDir + officeTemplateName,
                                templatePath + officeTemplateName
                        }, tmpDir);
                    }
                }
            }
        }
    }

    private static void copyResourcesRecursively(File targetDir) throws IOException {
        InputStream fileList = ServiceCodeReferenceHelper.getTemplateResource("MobileServiceTemplate/fileList.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(fileList));

        String line;
        while((line = in.readLine()) != null) {

            String[] pathParts = line.split("/");
            String fileName = pathParts[pathParts.length - 1];
            String path = line.replace(fileName, "");

            String targetPath = targetDir.getAbsolutePath() + File.separator
                    + "MobileServiceTemplate" + File.separator
                    + path.replace("/", File.separator);

            File targetFolder = new File(targetPath);
            targetFolder.mkdirs();

            File targetFile = new File(targetFolder, fileName);
            targetFile.createNewFile();
            targetFile.setWritable(true);

            InputStream inputStream = ServiceCodeReferenceHelper.getTemplateResource("MobileServiceTemplate/" + line);

            byte[] content = IOUtils.readFully(inputStream, -1, true);

            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
            fileOutputStream.write(content);
            fileOutputStream.flush();
            fileOutputStream.close();

        }
    }


    private static void exec(String[] cmd, String tmpdir) throws IOException, InterruptedException {
        String[] env = new String[]{"PRECOMPILE_STREAMLINE_FILES=1"};

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd, env, new File(tmpdir));

        // any error message?
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), true);


        // kick them off
        errorGobbler.start();

        proc.waitFor();
    }

    private static String getTempLocation() {

        String tmpdir = System.getProperty("java.io.tmpdir");
        StringBuilder sb = new StringBuilder();
        sb.append(tmpdir);

        if(!tmpdir.endsWith(File.separator))
            sb.append(File.separator);

        sb.append("TempAzure");
        sb.append(File.separator);

        return sb.toString();
    }

    private static class StreamGobbler extends Thread {
        InputStream is;
        boolean isError;

        public StreamGobbler(InputStream is, boolean isError)
        {
            this.is = is;
            this.isError = isError;
        }

        public void run()
        {
            try
            {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                StringBuilder sb = new StringBuilder();
                while ( (line = br.readLine()) != null)
                    sb.append(line + "\n");

                is.close();

                String streamContent = sb.toString();

                if(isError && !streamContent.isEmpty())
                    UIHelper.showException("Error copying Microsoft Services templates", new AzureCmdException("Error copying Microsoft Services templates", "Error: " + streamContent));

            } catch (IOException ioe) {
                UIHelper.showException("Error copying Microsoft Services templates", ioe);
            }
        }
    }

    public static void copyFolder(File src, File dest)
            throws IOException {

        if (src.isDirectory()) {

            //if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile, destFile);
            }

        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }
}
