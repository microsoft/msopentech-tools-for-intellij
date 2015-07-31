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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

import java.util.EventListener;

public abstract class NodeActionListener implements EventListener {
    protected static String name;

    public NodeActionListener() {
        // need a nullary constructor defined in order for
        // Class.newInstance to work on sub-classes
    }

    public NodeActionListener(Node node) {
    }

    protected void beforeActionPerformed(NodeActionEvent e) {
        // mark node as loading
        e.getAction().getNode().setLoading(true);
    }

    protected abstract void actionPerformed(NodeActionEvent e)
            throws AzureCmdException;

    public ListenableFuture<Void> actionPerformedAsync(NodeActionEvent e) {
        try {
            actionPerformed(e);
            return Futures.immediateFuture(null);
        } catch (AzureCmdException ex) {
            return Futures.immediateFailedFuture(ex);
        }
    }

    protected void afterActionPerformed(NodeActionEvent e) {
        // mark node as done loading
        e.getAction().getNode().setLoading(false);
    }
}
