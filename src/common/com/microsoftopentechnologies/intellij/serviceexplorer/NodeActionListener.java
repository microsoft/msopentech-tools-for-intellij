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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.EventListener;
import java.util.concurrent.ExecutionException;

public abstract class NodeActionListener implements EventListener {
    protected static boolean isDynamic;
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

    public void actionPerformed(NodeActionEvent e)  {}

    public ListenableFuture<Void> actionPerformedAsync(NodeActionEvent e) {
        actionPerformed(e);
        return Futures.immediateFuture(null);
    }

    protected void afterActionPerformed(NodeActionEvent e) {
        // mark node as done loading
        e.getAction().getNode().setLoading(false);
    }
}
