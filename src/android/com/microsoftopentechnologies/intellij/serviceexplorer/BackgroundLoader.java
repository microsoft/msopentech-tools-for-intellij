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
package com.microsoftopentechnologies.intellij.serviceexplorer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackgroundLoader extends Task.Backgroundable {
    private Node node;
    private SettableFuture<List<Node>> future;

    public BackgroundLoader(Node node, SettableFuture<List<Node>> future, Project project, String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        this.node = node;
        this.future = future;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setIndeterminate(true);

        final String nodeName = node.getName();
        node.setName(nodeName + " (Refreshing...)");

        Futures.addCallback(future, new FutureCallback<List<Node>>() {
            @Override
            public void onSuccess(List<Node> nodes) {
                updateName(null);
            }

            @Override
            public void onFailure(Throwable throwable) {
                updateName(throwable);
            }

            private void updateName(final Throwable throwable) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        node.setName(nodeName);
                        if (throwable != null) {
                            DefaultLoader.getUIHelper().showException("An error occurred while loading " + node.getName() + ".",
                                    throwable,
                                    "Error Loading " + node.getName(),
                                    false,
                                    true);
                        }
                    }
                });
            }
        });

        try {
            node.refreshItems(future);
        } catch (AzureCmdException e) {
            future.setException(e);
        }
    }
}