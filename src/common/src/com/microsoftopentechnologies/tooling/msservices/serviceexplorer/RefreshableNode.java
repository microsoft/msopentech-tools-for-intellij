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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

import java.util.List;

public abstract class RefreshableNode extends Node {
    public RefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public RefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void loadActions() {
        addAction("Refresh", new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                load();
            }
        });

        super.loadActions();
    }

    // Sub-classes are expected to override this method if they wish to
    // refresh items synchronously. The default implementation does nothing.
    protected abstract void refreshItems() throws AzureCmdException;

    // Sub-classes are expected to override this method if they wish
    // to refresh items asynchronously. The default implementation simply
    // delegates to "refreshItems" *synchronously* and completes the Future
    // with the result of calling getChildNodes.
    protected void refreshItems(SettableFuture<List<Node>> future) {
        setLoading(true);
        try {
            refreshItems();
            future.set(getChildNodes());
        } catch (AzureCmdException e) {
            future.setException(e);
        } finally {
            setLoading(false);
        }
    }

    public ListenableFuture<List<Node>> load() {
        final RefreshableNode node = this;
        final SettableFuture<List<Node>> future = SettableFuture.create();

        DefaultLoader.getIdeHelper().runInBackground(getProject(), "Loading " + getName() + "...", false, true, null,
                new Runnable() {
                    @Override
                    public void run() {
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
                                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
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

                        node.refreshItems(future);
                    }
                }
        );

        return future;
    }
}