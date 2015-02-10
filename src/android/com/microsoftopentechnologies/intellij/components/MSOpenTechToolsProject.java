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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.helpers.ServiceCodeReferenceHelper;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MSOpenTechToolsProject extends AbstractProjectComponent {
    private static final String TEMPLATES_ROOT_DIR = "build/intermediates/exploded-aar/Microsoft/templates/%s";
    private static final String TEMPLATES_RESOURCE_PATH = "/com/microsoftopentechnologies/intellij/templates/MobileServiceTemplate/";
    private static final String TEMPLATE_ZIP_NAME = "templates.zip";
    private static final String CACHED_TEMPLATE_ZIP_NAME = "templates-%s.zip";

    protected MSOpenTechToolsProject(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        try {
            // get project root dir and check if this is an Android project
            VirtualFile baseDir = myProject.getBaseDir();
            if (!ServiceCodeReferenceHelper.isAndroidGradleModule(baseDir)) {
                return;
            }

            // create the root dir to contain our templates zip if the
            // dir doesn't exist already
            File rootDir = new File(baseDir.getPath(),
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

        } catch (IOException ignored) {}
    }

    private File getTemplatesZip() {
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
        } catch (IOException e) {
            UIHelper.showException("Unable to open plugin jar file.", e);
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
