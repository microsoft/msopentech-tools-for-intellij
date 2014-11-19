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
package com.microsoftopentechnologies.intellij.helpers;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.forms.ErrorMessageForm;
import com.microsoftopentechnologies.intellij.forms.ImportSubscriptionForm;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureTreeLoader;
import com.microsoftopentechnologies.intellij.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

public class UIHelper {
    private static JTree mProjectTree;

    public static void setProjectTree(JTree projectTree) {
        mProjectTree = projectTree;
    }

    public static JTree getProjectTree() {
        return mProjectTree;
    }

    public static void packAndCenterJDialog(JDialog form) {
        form.pack();
        form.setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - form.getWidth() / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - form.getHeight() / 2);
    }

    public static void showException(final String message, final Throwable ex) {
        showException(message, ex, "Error");
    }

    public static void showException(final String message, final Throwable ex, final String title) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                String headerMessage = message + " ";
                String details = "";

                if (ex != null) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    String stackTrace = sw.toString();

                    if (ex instanceof AzureCmdException) {
                        String errorLog = ((AzureCmdException) ex).getErrorLog();

                        if (errorLog == null) {
                            errorLog = stackTrace;
                        } else {
                            //Not showing error if no account info found
                            if (errorLog.contains("No publish settings file found.") ||
                                    errorLog.contains("No account information found.")) {
                                JOptionPane.showMessageDialog(null, "No account information found. Please import subscription information.", "Error", JOptionPane.ERROR_MESSAGE);

                                // TODO: This should probably be showing the "Manage Subscriptions" form instead since
                                // we also support A/D auth now.
                                ImportSubscriptionForm isf = new ImportSubscriptionForm();
                                UIHelper.packAndCenterJDialog(isf);
                                isf.setVisible(true);

                                return;
                            }
                        }

                        details = errorLog;
                    } else {
                        details = stackTrace;
                        String exMessage = (ex.getLocalizedMessage() == null || ex.getLocalizedMessage().isEmpty()) ? ex.getMessage() : ex.getLocalizedMessage();
                        headerMessage = headerMessage + exMessage;
                    }
                }

                ErrorMessageForm em = new ErrorMessageForm(title);
                em.setCursor(Cursor.getDefaultCursor());
                UIHelper.packAndCenterJDialog(em);
                em.showErrorMessageForm(headerMessage, details);
                em.setVisible(true);
            }
        });
    }

    public static NodeRenderer getTreeNodeRenderer() {
        return new NodeRenderer() {
            private ImageIcon serviceImg = loadIcon("service.png");
            private ImageIcon tableImg = loadIcon("table.png");
            private ImageIcon scriptImg = loadIcon("script.png");
            private ImageIcon customAPIImg = loadIcon("api.png");
            private ImageIcon jobImg = loadIcon("job.png");

            @Override
            protected void doPaint(Graphics2D g) {
                super.doPaint(g);
                setOpaque(false);
            }

            @Override
            public void customizeCellRenderer(JTree jTree,
                                              Object value,
                                              boolean selected,
                                              boolean expanded,
                                              boolean isLeaf,
                                              int row,
                                              boolean focused) {

                super.customizeCellRenderer(jTree, value, selected,
                        expanded, isLeaf, row, focused);

                if (value instanceof DefaultMutableTreeNode) {
                    Object data = ((DefaultMutableTreeNode) value).getUserObject();

                    if (data instanceof Table) {
                        setIcon(tableImg);
                    } else if (data instanceof Script) {
                        setIcon(scriptImg);
                    } else if (data instanceof Service) {
                        setIcon(serviceImg);
                    } else if (data instanceof CustomAPI) {
                        setIcon(customAPIImg);
                    } else if (data instanceof Job) {
                        setIcon(jobImg);
                    }
                }
            }
        };
    }

    public static ImageIcon loadIcon(String name) {
        java.net.URL url = UIHelper.class.getResource("/com/microsoftopentechnologies/intellij/icons/" + name);
        return new ImageIcon(url);
    }

    public static void treeClick(final JTree tree, final DefaultMutableTreeNode selectedNode, final UUID subscriptionId, final String serviceName, final Project mProject) {

        if (selectedNode.getChildCount() == 0 && selectedNode.getUserObject() instanceof MobileServiceTreeItem) {

            //if children not loaded yet or last leaf
            final MobileServiceTreeItem selectedObject = (MobileServiceTreeItem) selectedNode.getUserObject();

            if (!selectedObject.isLoading()) {
                selectedObject.setLoading(true);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(selectedNode);

                ProgressManager.getInstance().run(new Task.Backgroundable(mProject, "Loading Mobile Services data...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {


                        AzureTreeLoader atl = new AzureTreeLoader(subscriptionId, serviceName, mProject, tree, progressIndicator);

                        if (selectedObject instanceof Service) {
                            progressIndicator.setIndeterminate(true);
                            progressIndicator.setText("Checking service");

                            if (AzureRestAPIHelper.existsMobileService(serviceName))
                                atl.serviceLoader(selectedNode);
                            else {

                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        selectedObject.setLoading(false);
                                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                        model.reload(selectedNode);

                                        JOptionPane.showMessageDialog(tree,
                                                "The mobile service " + serviceName +
                                                        " could not be reached. Please try again after some time.",
                                                "Microsoft Services Plugin",
                                                JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            }
                        } else if (selectedObject instanceof Table)
                            atl.tableLoader((Table) selectedObject, selectedNode);
                        else if (selectedObject instanceof MobileServiceScriptTreeItem)
                            atl.scriptLoader((MobileServiceScriptTreeItem) selectedObject, selectedNode);

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                selectedObject.setLoading(false);
                                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                model.reload(selectedNode);
                            }
                        });

                    }
                });
            }
        }
    }

    public static void saveScript(Project project, final DefaultMutableTreeNode selectedNode, final Script script, final String serviceName, final String subscriptionId) throws AzureCmdException {
        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(script.getLocalFilePath(serviceName)));
        if (editorFile != null) {
            FileEditor[] fe = FileEditorManager.getInstance(project).getAllEditors(editorFile);

            if (fe.length > 0 && fe[0].isModified()) {
                int i = JOptionPane.showConfirmDialog(null, "The file is modified. Do you want to save pending changes?", "Upload Script", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (i) {
                    case JOptionPane.YES_OPTION:
                        ApplicationManager.getApplication().saveAll();
                        break;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;
                }
            }

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading table script", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        AzureRestAPIManager.getManager().uploadTableScript(UUID.fromString(subscriptionId), serviceName, script.getName(), script.getLocalFilePath(serviceName));

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (script.getSelfLink() == null)
                                    script.setSelfLink("");
                                selectedNode.setUserObject(script);
                            }
                        });
                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error uploading script", e);
                    }
                }
            });


        }
    }

    public static void saveCustomAPI(Project project, final CustomAPI customAPI, final String serviceName, final String subscriptionId) throws AzureCmdException {
        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(customAPI.getLocalFilePath(serviceName)));
        if (editorFile != null) {
            FileEditor[] fe = FileEditorManager.getInstance(project).getAllEditors(editorFile);

            if (fe.length > 0 && fe[0].isModified()) {
                int i = JOptionPane.showConfirmDialog(null, "The file is modified. Do you want to save pending changes?", "Upload Script", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (i) {
                    case JOptionPane.YES_OPTION:
                        ApplicationManager.getApplication().saveAll();
                        break;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;
                }
            }

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading custom api script", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        AzureRestAPIManager.getManager().uploadAPIScript(UUID.fromString(subscriptionId), serviceName, customAPI.getName(), customAPI.getLocalFilePath(serviceName));
                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error uploading script", e);
                    }
                }
            });
        }
    }

    public static void saveJob(Project project, final Job job, final String serviceName, final String subscriptionId) throws AzureCmdException {
        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(job.getLocalFilePath(serviceName)));
        if (editorFile != null) {
            FileEditor[] fe = FileEditorManager.getInstance(project).getAllEditors(editorFile);

            if (fe.length > 0 && fe[0].isModified()) {
                int i = JOptionPane.showConfirmDialog(null, "The file is modified. Do you want to save pending changes?", "Upload Script", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (i) {
                    case JOptionPane.YES_OPTION:
                        ApplicationManager.getApplication().saveAll();
                        break;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;
                }
            }

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Uploading job script", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        AzureRestAPIManager.getManager().uploadJobScript(UUID.fromString(subscriptionId), serviceName, job.getName(), job.getLocalFilePath(serviceName));
                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error uploading script", e);
                    }
                }
            });
        }
    }
}

