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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

import java.util.concurrent.Callable;

public abstract class NodeActionListenerAsync extends NodeActionListener {
    private String progressMessage;

    public NodeActionListenerAsync(@NotNull String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public ListenableFuture<Void> actionPerformedAsync(final NodeActionEvent actionEvent) {
        Callable<Boolean> booleanCallable = beforeAsyncActionPerfomed();

        boolean shouldRun = true;

        try {
            shouldRun = booleanCallable.call();
        } catch (Exception ignored) {
        }

        final SettableFuture<Void> future = SettableFuture.create();

        if (shouldRun) {
            DefaultLoader.getIdeHelper().runInBackground(actionEvent.getAction().getNode().getProject(), progressMessage, true, false, null, new Runnable() {
                @Override
                public void run() {
                    try {
                        actionPerformed(actionEvent);
                        future.set(null);
                    } catch (AzureCmdException e) {
                        future.setException(e);
                    }
                }
            });
        } else {
            future.set(null);
        }

        return future;
    }

    @NotNull
    protected abstract Callable<Boolean> beforeAsyncActionPerfomed();
}