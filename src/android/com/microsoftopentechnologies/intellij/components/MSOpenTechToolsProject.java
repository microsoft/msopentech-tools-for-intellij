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
package com.microsoftopentechnologies.intellij.components;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.microsoftopentechnologies.intellij.helpers.AndroidStudioHelper;
import com.microsoftopentechnologies.intellij.helpers.ProjectFileMonitor;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MSOpenTechToolsProject extends AbstractProjectComponent {
    private static final String TEMPLATES_ROOT_DIR = "build/intermediates/exploded-aar/Microsoft/templates/%s";
    private static final String TEMPLATES_RESOURCE_PATH = "/com/microsoftopentechnologies/intellij/templates/MobileServiceTemplate/";
    private static final String TEMPLATE_ZIP_NAME = "templates.zip";
    private static final String CACHED_TEMPLATE_ZIP_NAME = "templates-%s.zip";

    private VirtualFileListener virtualFileListener = null;

    protected MSOpenTechToolsProject(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        try {
            // get project root dir and check if this is an Android project
            if (AndroidStudioHelper.isAndroidGradleModule(myProject.getBaseDir())) {
                createActivityTemplates();
            }
            else {
                // if a new project is being created then this method is called before
                // any file is created; we install a file listener to handle the template
                // file creation in case the project being created is an Android gradle
                // project
                virtualFileListener = new VirtualFileAdapter() {
                    @Override
                    public void contentsChanged(VirtualFileEvent event) {
                        super.fileCreated(event);

                        // if this is a build.gradle file and is an android build file then
                        // copy the template files
                        try {
                            if(event.getFileName().contains("build.gradle") &&
                                    AndroidStudioHelper.isAndroidGradleBuildFile(event.getFile())) {
                                createActivityTemplates();

                                // now that we have the activity templates installed in this project
                                // we don't need to listen for file changes anymore
                                ProjectFileMonitor.getInstance().removeProjectFileListener(myProject, virtualFileListener);
                                virtualFileListener = null;
                            }
                        } catch (IOException ignored) {}
                    }
                };
                ProjectFileMonitor.getInstance().addProjectFileListener(myProject, virtualFileListener);
            }

        } catch (IOException ignored) {}
    }

    @Override
    public void projectClosed() {
        if(virtualFileListener != null) {
            ProjectFileMonitor.getInstance().removeProjectFileListener(myProject, virtualFileListener);
            virtualFileListener = null;
        }
    }

    private void createActivityTemplates() throws IOException {
        // create the root dir to contain our templates zip if the
        // dir doesn't exist already
        File rootDir = new File(myProject.getBaseDir().getPath(),
                String.format(
                        TEMPLATES_ROOT_DIR,
                        MSOpenTechToolsApplication.getCurrent().getSettings().getPluginVersion()));
        rootDir.mkdirs();

        // we proceed only if "templates.zip" doesn't already exist in the path
        File templatesZip = new File(rootDir, TEMPLATE_ZIP_NAME);
        if(!templatesZip.exists()) {
            File cachedZip = getTemplatesZip();
            Files.copy(cachedZip, templatesZip);
        }
    }

    private File getTemplatesZip() throws IOException {
        // we cache the templates zip for the current version of the plugin
        File cachedZip = new File(System.getProperty("java.io.tmpdir"),
                String.format(
                        CACHED_TEMPLATE_ZIP_NAME,
                        MSOpenTechToolsApplication.getCurrent().getSettings().getPluginVersion()));

        BufferedReader reader = null;
        InputStream inputStream = null;
        ZipOutputStream outputStream = null;

        try {
            if (!cachedZip.exists()) {
                // read list of files to copy to zip and create the zip file
                outputStream = new ZipOutputStream(
                        new BufferedOutputStream(new FileOutputStream(cachedZip)));

                reader = new BufferedReader(
                        new InputStreamReader(
                                MSOpenTechToolsProject.class.getResourceAsStream(
                                        TEMPLATES_RESOURCE_PATH + "fileList.txt")));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    inputStream = MSOpenTechToolsProject.class.getResourceAsStream(
                            TEMPLATES_RESOURCE_PATH + line
                    );

                    ZipEntry entry = new ZipEntry(line);
                    outputStream.putNextEntry(entry);

                    ByteStreams.copy(inputStream, outputStream);
                    inputStream.close();
                    inputStream = null;
                }
                reader.close(); reader = null;
                outputStream.close(); outputStream = null;
            }
        }
        catch (IOException e) {
            // if creation of the zip file fails and leaves a partially
            // created zip file in the file system then we delete it so
            // that we attempt creating it again the next time around
            // which we wouldn't if we discover that the file exists
            // already
            if(cachedZip.exists()) {
                cachedZip.delete();
            }

            throw e;
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if(inputStream != null) {
                    inputStream.close();
                }
                if(outputStream != null) {
                    outputStream.close();
                }
            }
            catch (IOException ignored) {}
        }

        return cachedZip;
    }
}
