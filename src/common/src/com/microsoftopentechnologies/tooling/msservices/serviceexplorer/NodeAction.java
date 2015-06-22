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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.List;

public class NodeAction {
    private String name;
    private boolean enabled = true;
    private List<NodeActionListener> listeners = new ArrayList<NodeActionListener>();
    private Node node; // the node with which this action is associated

    public NodeAction(Node node, String name) {
        this.node = node;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addListener(NodeActionListener listener) {
        listeners.add(listener);
    }

    public List<NodeActionListener> getListeners() {
        return listeners;
    }

    public void fireNodeActionEvent() {
        if (!listeners.isEmpty()) {
            final NodeActionEvent event = new NodeActionEvent(this);
            for (final NodeActionListener listener : listeners) {
                listener.beforeActionPerformed(event);
                Futures.addCallback(listener.actionPerformedAsync(event), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        listener.afterActionPerformed(event);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        listener.afterActionPerformed(event);
                    }
                });
            }
        }
    }

    public Node getNode() {
        return node;
    }

    public boolean isEnabled() {
        // if the node to which this action is attached is in a
        // "loading" state then we disable the action regardless
        // of what "enabled" is
        return !node.isLoading() && enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
