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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class AzureTreeLoader {

    private UUID mSubscriptionId;
    private String mServiceName;
    private Project mProject;
    private JTree tree;
    private ProgressIndicator progressIndicator;

    public AzureTreeLoader(UUID subscriptionId, String serviceName, Project project, JTree jTree, ProgressIndicator pi){
        mSubscriptionId = subscriptionId;
        mServiceName = serviceName;
        mProject = project;
        tree = jTree;
        progressIndicator = pi;
    }

    public void serviceLoader(final DefaultMutableTreeNode selectedNode){

        try {
            progressIndicator.setIndeterminate(false);
            progressIndicator.setText("Getting tables");
            progressIndicator.setFraction(0.25f);
            final List<Table> tableList = AzureRestAPIManager.getManager().getTableList(mSubscriptionId, mServiceName);

            progressIndicator.setText("Getting apis");
            progressIndicator.setFraction(0.5f);
            final List<CustomAPI> apiList = AzureRestAPIManager.getManager().getAPIList(mSubscriptionId, mServiceName);

            progressIndicator.setText("Getting jobs");
            progressIndicator.setFraction(0.75f);
            final List<Job> jobs = AzureRestAPIManager.getManager().listJobs(mSubscriptionId, mServiceName);

            progressIndicator.setText("Setting UI");
            progressIndicator.setFraction(1f);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    DefaultMutableTreeNode tableRootNode = new DefaultMutableTreeNode("Tables");
                    for (Table t : tableList) {
                        DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(t.getName());
                        tableNode.setUserObject(t);
                        tableRootNode.add(tableNode);
                    }
                    selectedNode.add(tableRootNode);

                    DefaultMutableTreeNode apiRootNode = new DefaultMutableTreeNode("Custom APIs");
                    for (CustomAPI api : apiList) {
                        DefaultMutableTreeNode apiNode = new DefaultMutableTreeNode(api.getName());
                        apiNode.setUserObject(api);
                        apiRootNode.add(apiNode);
                    }

                    selectedNode.add(apiRootNode);

                    DefaultMutableTreeNode jobRootNode = new DefaultMutableTreeNode("Scheduled jobs");
                    for (Job job : jobs) {
                        DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(job.getName());
                        jobNode.setUserObject(job);
                        jobRootNode.add(jobNode);
                    }

                    selectedNode.add(jobRootNode);
                }
            });

        } catch (AzureCmdException e) {
            UIHelper.showException("Error getting service info", e);
        }
    }

    public void tableLoader(final Table table, final DefaultMutableTreeNode selectedNode) {
        try{
            progressIndicator.setText("Getting table info");
            final Table tableinfo = AzureRestAPIManager.getManager().showTableDetails(mSubscriptionId, mServiceName, table.getName());

            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    DefaultMutableTreeNode scripts = new DefaultMutableTreeNode("Scripts");
                    DefaultMutableTreeNode columns = new DefaultMutableTreeNode("Columns");

                    for (String operation : Script.getOperationList()) {
                        DefaultMutableTreeNode scriptnode = new DefaultMutableTreeNode(operation);

                        Script s = new Script();
                        s.setOperation(operation);
                        s.setBytes(0);
                        s.setName(String.format("%s.%s", tableinfo.getName(), operation));

                        for (Script script : tableinfo.getScripts()) {
                            if (script.getOperation().equals(operation)) {
                                s = script;
                            }
                        }

                        scriptnode.setUserObject(s);
                        scripts.add(scriptnode);
                    }

                    for (Column col : tableinfo.getColumns()) {
                        if (!col.getName().startsWith("__")) {
                            DefaultMutableTreeNode colnode = new DefaultMutableTreeNode(col.getName());
                            colnode.setUserObject(col);
                            columns.add(colnode);
                        }
                    }

                    selectedNode.add(scripts);
                    selectedNode.add(columns);

                }
            });
        } catch (AzureCmdException e) {
            UIHelper.showException("Error getting service info", e);
        }
    }

    public void scriptLoader(final MobileServiceScriptTreeItem script, final DefaultMutableTreeNode selectedNode) {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByIoFile(new File(script.getLocalFilePath(mServiceName)));
                boolean fileIsEditing = false;

                if (scriptFile != null)
                    fileIsEditing = FileEditorManager.getInstance(mProject).getEditors(scriptFile).length != 0;

                if(!fileIsEditing){

                    try {

                        File temppath = new File(script.getLocalDirPath(mServiceName));
                        temppath.mkdirs();


                        if (script instanceof Script && ((Script)script).getSelfLink() == null){

                            InputStream is = this.getClass().getResourceAsStream(String.format("/com/microsoftopentechnologies/intellij/templates/%s.js", ((Script)script).getOperation()));
                            final ByteArrayOutputStream buff = new ByteArrayOutputStream();

                            int b;
                            while ((b = is.read()) != -1)
                                buff.write(b);

                            final File tempf = new File(temppath, ((Script)script).getOperation() + ".js");
                            tempf.createNewFile();

                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        final VirtualFile editfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempf);
                                        if (editfile != null) {
                                            editfile.setWritable(true);

                                            editfile.setBinaryContent(buff.toByteArray());

                                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    FileEditorManager.getInstance(mProject).openFile(editfile, true);
                                                }
                                            });
                                        }
                                    } catch (Throwable e) {
                                        tree.setCursor(Cursor.getDefaultCursor());
                                        UIHelper.showException("Error writing temporal editable file:", e);
                                    } finally {
                                        script.setLoading(false);
                                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                        model.reload(selectedNode);
                                        tree.setCursor(Cursor.getDefaultCursor());
                                    }
                                }
                            });

                        } else {

                            boolean download = false;
                            final File file = new File(script.getLocalFilePath(mServiceName));
                            if(file.exists()) {
                                String[] options = new String[] { "Use remote" , "Use local"};
                                int optionDialog = JOptionPane.showOptionDialog(null,
                                        "There is a local copy of the script. Do you want you replace it with the remote version?",
                                        "Edit script",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null,
                                        options,
                                        options[1]);

                                if(optionDialog == JOptionPane.YES_OPTION) {
                                    download = true;
                                }

                            } else {
                                download = true;
                            }



                            if(download) {
                                ProgressManager.getInstance().run(new Task.Backgroundable(mProject, "Loading Mobile Services data...", false) {
                                    @Override
                                    public void run(@NotNull ProgressIndicator progressIndicator) {
                                        progressIndicator.setIndeterminate(true);
                                        progressIndicator.setText("Downloading script");
                                        try {

                                            if(script instanceof Script)
                                                AzureRestAPIManager.getManager().downloadTableScript(mSubscriptionId, mServiceName, script.getName(), script.getLocalFilePath(mServiceName));
                                            else if(script instanceof CustomAPI)
                                                AzureRestAPIManager.getManager().downloadAPIScript(mSubscriptionId, mServiceName, script.getName(), script.getLocalFilePath(mServiceName));
                                            else if(script instanceof Job)
                                                AzureRestAPIManager.getManager().downloadJobScript(mSubscriptionId, mServiceName, script.getName(), script.getLocalFilePath(mServiceName));

                                            final VirtualFile finalEditfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                                            ApplicationManager.getApplication().runReadAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    openFile(finalEditfile, script, selectedNode);
                                                }
                                            });
                                        } catch (Throwable e) {
                                            UIHelper.showException("Error writing temporal editable file:", e);
                                            script.setLoading(false);
                                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                                    model.reload(selectedNode);
                                                    tree.setCursor(Cursor.getDefaultCursor());
                                                }
                                            });
                                        }
                                    }
                                });

                            } else {
                                final VirtualFile finalEditfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                                ApplicationManager.getApplication().runReadAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        openFile(finalEditfile, script, selectedNode);
                                    }
                                });
                            }
                        }
                    } catch (Throwable e) {
                        UIHelper.showException("Error writing temporal editable file:", e);
                    } finally {
                        script.setLoading(false);
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                model.reload(selectedNode);
                                tree.setCursor(Cursor.getDefaultCursor());
                            }
                        });
                    }
                }
            }
        });

    }


    private void openFile(final VirtualFile finalEditfile, MobileServiceScriptTreeItem script, final TreeNode selectedNode) {
        try {

            if (finalEditfile != null) {
                finalEditfile.setWritable(true);

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        FileEditorManager.getInstance(mProject).openFile(finalEditfile, true);
                    }
                });
            }

        } catch (Throwable e) {
            tree.setCursor(Cursor.getDefaultCursor());
            UIHelper.showException("Error writing temporal editable file:", e);
        } finally {
            script.setLoading(false);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    model.reload(selectedNode);
                    tree.setCursor(Cursor.getDefaultCursor());
                }
            });
        }

    }
}