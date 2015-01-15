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

package com.microsoftopentechnologies.intellij.serviceexplorer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class NodeActionListenerAsync extends NodeActionListener {
    private String progressMessage;

    public NodeActionListenerAsync(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public ListenableFuture<Void> actionPerformedAsync(final NodeActionEvent actionEvent) {
        final SettableFuture<Void> future = SettableFuture.create();
        ProgressManager.getInstance().run(new Task.Backgroundable(actionEvent.getAction().getNode().getProject(), progressMessage) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    runInBackground(actionEvent);
                    future.set(null);
                } catch (AzureCmdException e) {
                    future.setException(e);
                }
            }
        });

        return future;
    }

    protected void runInBackground(NodeActionEvent actionEvent) throws AzureCmdException {}
}
