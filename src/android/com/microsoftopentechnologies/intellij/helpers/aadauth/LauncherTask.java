/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.intellij.helpers.aadauth;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.tooling.msservices.helpers.Nullable;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.ADJarLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.BrowserLauncher;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

public class LauncherTask extends Task.Modal {
    private BrowserLauncher browserLauncher;

    public LauncherTask(BrowserLauncher browserLauncher, @Nullable Project project, @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        this.browserLauncher = browserLauncher;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        try {
            progressIndicator.setIndeterminate(true);

            // download the browser app jar
            progressIndicator.setText("Loading authentication components...");
            File appJar = ADJarLoader.load();

            // popup auth UI
            progressIndicator.setText("Signing in...");
            browserLauncher.launch(appJar);
        } catch (ExecutionException e) {
            browserLauncher.reportError(e);
        } catch (MalformedURLException e) {
            browserLauncher.reportError(e);
        } catch (ClassNotFoundException e) {
            browserLauncher.reportError(e);
        } catch (NoSuchMethodException e) {
            browserLauncher.reportError(e);
        } catch (InvocationTargetException e) {
            browserLauncher.reportError(e);
        } catch (IllegalAccessException e) {
            browserLauncher.reportError(e);
        } catch (IOException e) {
            browserLauncher.reportError(e);
        } catch (InterruptedException e) {
            browserLauncher.reportError(e);
        }
    }
}