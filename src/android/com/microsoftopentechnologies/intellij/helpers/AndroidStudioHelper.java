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
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AndroidStudioHelper {
    private static final String mobileServicesTemplateName = "AzureServicesActivity";
    private static final String officeTemplateName = "Office365Activity";

    public static boolean isAndroidStudio() {
        return ApplicationInfo.getInstance().getVersionName().startsWith("Android Studio");
    }

    public static void newActivityTemplateManager() throws IOException, InterruptedException {
        String[] cmd = null;

        String templatePath = URLDecoder.decode(ApplicationComponent.class.getResource("").getPath().replace("file:/", ""), "UTF-8");
        templatePath = templatePath.replace("/", File.separator);
        templatePath = templatePath.substring(0, templatePath.indexOf(File.separator + "lib"));
        templatePath = templatePath + File.separator + "plugins" + File.separator + "android" + File.separator;
        templatePath = templatePath + "lib" + File.separator + "templates" + File.separator + "activities" + File.separator;

        String[] env = null;

        if(!new File(templatePath + mobileServicesTemplateName).exists()) {
            String tmpdir = getTempLocation();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(ServiceCodeReferenceHelper.getTemplateResource("ActivityTemplate.zip"));
            unZip(bufferedInputStream, tmpdir);

            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                try {
                    copyFolder(new File(tmpdir + mobileServicesTemplateName), new File(templatePath + mobileServicesTemplateName));
                    copyFolder(new File(tmpdir + officeTemplateName), new File(templatePath + officeTemplateName));
                } catch (IOException ex) {
                    PrintWriter printWriter = new PrintWriter(tmpdir + "\\script.bat");
                    printWriter.println("@echo off");
                    printWriter.println("md \"" + templatePath + mobileServicesTemplateName + "\"");
                    printWriter.println("md \"" + templatePath + officeTemplateName + "\"");
                    printWriter.println("xcopy \"" + tmpdir + mobileServicesTemplateName + "\" \"" + templatePath + mobileServicesTemplateName + "\" /s /i /Y");
                    printWriter.println("xcopy \"" + tmpdir + officeTemplateName + "\" \"" + templatePath + officeTemplateName + "\" /s /i /Y");
                    printWriter.flush();
                    printWriter.close();

                    String[] tmpcmd = {
                        tmpdir + "\\elevate.exe",
                        "script.bat",
                        "1"
                    };

                    cmd = tmpcmd;

                    ArrayList<String> tempenvlist = new ArrayList<String>();
                    for(String envval : System.getenv().keySet())
                        tempenvlist.add(String.format("%s=%s", envval, System.getenv().get(envval)));

                    tempenvlist.add("PRECOMPILE_STREAMLINE_FILES=1");
                    env = new String[tempenvlist.size()];
                    tempenvlist.toArray(env);

                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec(cmd, env, new File(tmpdir));
                    proc.waitFor();

                    //wait for elevate command to finish
                    Thread.sleep(3000);

                    if(!new File(templatePath + mobileServicesTemplateName).exists())
                        UIHelper.showException("Error copying template files. Please refer to documentation to copy manually.", new Exception());
                }
            } else {
                if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {

                    String[] strings = {
                            "osascript",
                    //        "-e",
                    //        "do shell script \"mkdir -p \\\"/" + templatePath + mobileServicesTemplateName + "\\\"\"",
                    //        "-e",
                    //        "do shell script \"mkdir -p \\\"/" + templatePath + officeTemplateName + "\\\"\"",
                            "-e",
                            "do shell script \"cp -Rp \\\"" + tmpdir + mobileServicesTemplateName + "\\\" \\\"/" + templatePath + "\\\"\"",
                            "-e",
                            "do shell script \"cp -Rp \\\"" + tmpdir + officeTemplateName + "\\\" \\\"/" + templatePath + "\\\"\""
                    };

                    exec(strings, tmpdir);
                } else {
                    try {

                        copyFolder(new File(tmpdir + mobileServicesTemplateName), new File(templatePath + mobileServicesTemplateName));
                        copyFolder(new File(tmpdir + officeTemplateName), new File(templatePath + officeTemplateName));


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
                                    "cp",
                                    "-Rp",
                                    tmpdir + mobileServicesTemplateName,
                                    templatePath + mobileServicesTemplateName
                            }, tmpdir);


                            exec(new String[]{
                                    "echo",
                                    password,
                                    "|",
                                    "sudo",
                                    "-S",
                                    "cp",
                                    "-Rp",
                                    tmpdir + officeTemplateName,
                                    templatePath + officeTemplateName
                            }, tmpdir);
                        }
                    }
                }
            }
        }
    }

    public static void deleteActivityTemplates(Object caller) throws IOException, InterruptedException {
        String[] cmd = null;

        String templatePath = URLDecoder.decode(ApplicationComponent.class.getResource("").getPath().replace("file:/", ""), "UTF-8");
        templatePath = templatePath.replace("/", File.separator);
        templatePath = templatePath.substring(0, templatePath.indexOf(File.separator + "lib"));
        templatePath = templatePath + File.separator + "plugins" + File.separator + "android" + File.separator;
        templatePath = templatePath + "lib" + File.separator + "templates" + File.separator + "activities" + File.separator;

        String[] env = null;

        String tmpdir = getTempLocation();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(ServiceCodeReferenceHelper.getTemplateResource("ActivityTemplate.zip"));
        unZip(bufferedInputStream, tmpdir);

        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            try{
                VirtualFile mobileTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + mobileServicesTemplateName));
                VirtualFile officeTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + officeTemplateName));

                if(mobileTemplate != null)
                    mobileTemplate.delete(caller);

                if(officeTemplate != null)
                    officeTemplate.delete(caller);
            } catch (IOException ex) {
                PrintWriter printWriter = new PrintWriter(tmpdir + "\\script.bat");
                printWriter.println("@echo off");
                printWriter.println("del \"" + templatePath + mobileServicesTemplateName + "\" /Q /S");
                printWriter.println("del \"" + templatePath + officeTemplateName + "\" /Q /S");
                printWriter.flush();
                printWriter.close();

                String[] tmpcmd = {
                        tmpdir + "\\elevate.exe",
                        "script.bat",
                        "1"
                };

                cmd = tmpcmd;

                ArrayList<String> tempenvlist = new ArrayList<String>();
                for(String envval : System.getenv().keySet())
                    tempenvlist.add(String.format("%s=%s", envval, System.getenv().get(envval)));

                tempenvlist.add("PRECOMPILE_STREAMLINE_FILES=1");
                env = new String[tempenvlist.size()];
                tempenvlist.toArray(env);

                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec(cmd, env, new File(tmpdir));
                proc.waitFor();
            }
        } else if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            VirtualFile mobileTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + mobileServicesTemplateName));
            VirtualFile officeTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + officeTemplateName));

            if(mobileTemplate != null && officeTemplate != null) {
               exec(new String[]{
                        "osascript",
                        "-e",
                        "do shell script \"rm -r \\\"/" + templatePath + mobileServicesTemplateName + "\\\"\"",
                        "-e",
                        "do shell script \"rm -r \\\"/" + templatePath + officeTemplateName + "\\\"\""
                }, tmpdir);
            }
        } else {
            try {
                VirtualFile mobileTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + mobileServicesTemplateName));
                VirtualFile officeTemplate = LocalFileSystem.getInstance().findFileByIoFile(new File(templatePath + officeTemplateName));

                mobileTemplate.delete(caller);
                officeTemplate.delete(caller);
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
                            tmpdir + mobileServicesTemplateName,
                            templatePath + mobileServicesTemplateName
                    }, tmpdir);


                    exec(new String[]{
                            "echo",
                            password,
                            "|",
                            "sudo",
                            "-S",
                            "rm",
                            "-r",
                            tmpdir + officeTemplateName,
                            templatePath + officeTemplateName
                    }, tmpdir);
                }
            }
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
        sb.append("MobileServiceTemplate");
        sb.append(File.separator);

        return sb.toString();
    }


    public static void unZip(InputStream zipFileIS, String outputFolder) throws IOException {

        byte[] buffer = new byte[1024];

        //create output directory is not exists
        File folder = new File(outputFolder);
        if(!folder.exists()){
            folder.mkdir();
        }

        //get the zip file content
        ZipInputStream zis = new ZipInputStream(zipFileIS);
        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();

        while(ze!=null){

            String fileName = ze.getName();
            File newFile = new File(outputFolder + File.separator + fileName);

            if(ze.isDirectory()) {
                newFile.mkdirs();
            } else {

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.flush();
                fos.close();
            }
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
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
