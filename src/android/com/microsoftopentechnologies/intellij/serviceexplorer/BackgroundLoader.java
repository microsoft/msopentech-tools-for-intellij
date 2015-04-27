package com.microsoftopentechnologies.intellij.serviceexplorer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
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
