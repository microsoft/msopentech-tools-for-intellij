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

package com.microsoftopentechnologies.intellij.helpers.azure;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.forms.CustomAPIForm;
import com.microsoftopentechnologies.intellij.forms.JobForm;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.intellij.forms.ViewLogForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.*;
import com.microsoftopentechnologies.intellij.model.vm.Endpoint;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachine;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AzureTreeLoader {

    private Project mProject;
    private JTree tree;
    private ProgressIndicator progressIndicator;

    public AzureTreeLoader(Project project, JTree jTree){
        mProject = project;
        tree = jTree;

    }

    public void treeClick(final DefaultMutableTreeNode selectedNode) {

        final AzureTreeLoader atl = this;

        if (selectedNode.getChildCount() == 0 && selectedNode.getUserObject() instanceof ServiceTreeItem) {

            //if children not loaded yet or last leaf
            final ServiceTreeItem selectedObject = (ServiceTreeItem) selectedNode.getUserObject();

            if (!selectedObject.isLoading()) {
                selectedObject.setLoading(true);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(selectedNode);

                ProgressManager.getInstance().run(new Task.Backgroundable(mProject, "Loading Mobile Services data...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {

                        atl.progressIndicator = progressIndicator;

                        if(selectedObject instanceof AzureType) {
                            azureTypeLoader((AzureType) selectedObject, selectedNode);
                        } else if (selectedObject instanceof Service) {
                            serviceLoader((Service) selectedObject ,selectedNode);
                        } else if (selectedObject instanceof VirtualMachine) {
                            vmLoader((VirtualMachine) selectedObject, selectedNode);
                        } else if (selectedObject instanceof Table)
                            tableLoader((Table) selectedObject, selectedNode);
                        else if (selectedObject instanceof MobileServiceScriptTreeItem)
                            scriptLoader((MobileServiceScriptTreeItem) selectedObject, selectedNode);




                    }
                });
            }
        }
    }


    private Service getParentService(DefaultMutableTreeNode selectedNode) {
        for (Object obj : selectedNode.getUserObjectPath()) {
            if(obj instanceof Service) {
                return (Service) obj;
            }
        }
        return null;
    }

    private Subscription getParentSubscription(DefaultMutableTreeNode selectedNode) {
        for (Object obj : selectedNode.getUserObjectPath()) {
            if(obj instanceof Subscription) {
                return (Subscription) obj;
            }
        }
        return null;
    }


    private void azureTypeLoader(final AzureType selectedObject, final DefaultMutableTreeNode selectedNode) {

        final UUID mSubscriptionId = getParentSubscription(selectedNode).getId();

        ApplicationManager.getApplication().invokeLater(new Runnable() {

            @Override
            public void run() {
                switch(selectedObject) {
                    case MobileServices:

                        ProgressManager.getInstance().run(new Task.Backgroundable(mProject, "Loading mobile services...", false) {
                            @Override
                            public void run(@NotNull ProgressIndicator progressIndicator) {
                                try {
                                    progressIndicator.setIndeterminate(true);

                                    final List<Service> services = AzureRestAPIManager.getManager().getServiceList(mSubscriptionId);

                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override
                                        public void run() {

                                            for (Service service : services) {

                                                DefaultMutableTreeNode serviceTree = new DefaultMutableTreeNode(service.getName());
                                                serviceTree.setUserObject(service);
                                                selectedNode.add(serviceTree);

                                                selectedObject.setLoading(false);
                                                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                                model.reload(selectedNode);
                                            }
                                        }
                                    });
                                } catch (AzureCmdException e) {
                                    UIHelper.showException("Error querying mobile services data", e);
                                }
                            }
                        });

                        break;
                    case VirtualMachines:

                        ProgressManager.getInstance().run(new Task.Backgroundable(mProject, "Loading virtual machines...", false) {
                            @Override
                            public void run(@NotNull ProgressIndicator progressIndicator) {
                                try {
                                    progressIndicator.setIndeterminate(true);

                                    final List<VirtualMachine> virtualMachines = new AzureSDKManagerImpl().getVirtualMachines(mSubscriptionId.toString());

                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override
                                        public void run() {

                                            for (VirtualMachine vm : virtualMachines) {

                                                DefaultMutableTreeNode serviceTree = new DefaultMutableTreeNode(vm.getName());
                                                serviceTree.setUserObject(vm);
                                                selectedNode.add(serviceTree);

                                                selectedObject.setLoading(false);
                                                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                                model.reload(selectedNode);
                                            }
                                        }
                                    });
                                } catch (AzureCmdException e) {
                                    UIHelper.showException("Error querying mobile services data", e);
                                }
                            }
                        });

                        break;

                }

            }
        });
    }


    private void vmLoader(final VirtualMachine selectedObject, final DefaultMutableTreeNode selectedNode) {
        progressIndicator.setIndeterminate(true);
        progressIndicator.setText("Getting endpoints");

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Endpoint endpoint : selectedObject.getEndpoints()) {
                    DefaultMutableTreeNode endpointNode = new DefaultMutableTreeNode(endpoint.getName());
                    endpointNode.setUserObject(endpoint);
                    selectedNode.add(endpointNode);
                }

                selectedObject.setLoading(false);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(selectedNode);
            }
        });
    }

    private void serviceLoader(final Service selectedObject, final DefaultMutableTreeNode selectedNode){
        try {
            progressIndicator.setIndeterminate(true);
            progressIndicator.setText("Checking service");

            final String serviceName = selectedObject.getName();

            if (AzureRestAPIHelper.existsMobileService(serviceName)) {

                UUID mSubscriptionId = getParentSubscription(selectedNode).getId();
                Service service = (Service) selectedNode.getUserObject();

                progressIndicator.setIndeterminate(false);
                progressIndicator.setText("Getting tables");
                progressIndicator.setFraction(0.25f);
                final List<Table> tableList = AzureRestAPIManager.getManager().getTableList(mSubscriptionId, service.getName());

                progressIndicator.setText("Getting apis");
                progressIndicator.setFraction(0.5f);
                final List<CustomAPI> apiList = AzureRestAPIManager.getManager().getAPIList(mSubscriptionId, service.getName());

                progressIndicator.setText("Getting jobs");
                progressIndicator.setFraction(0.75f);
                final List<Job> jobs = AzureRestAPIManager.getManager().listJobs(mSubscriptionId, service.getName());

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
            } else {

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
        } catch (AzureCmdException e) {
            UIHelper.showException("Error getting service info", e);
        }
    }


    private void tableLoader(final Table table, final DefaultMutableTreeNode selectedNode) {
        try{
            UUID mSubscriptionId = getParentSubscription(selectedNode).getId();

            String serviceName = getParentService(selectedNode).getName();

            progressIndicator.setText("Getting table info");

            final Table tableinfo = AzureRestAPIManager.getManager().showTableDetails(mSubscriptionId, serviceName, table.getName());

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

    private void scriptLoader(final MobileServiceScriptTreeItem script, final DefaultMutableTreeNode selectedNode) {
        final UUID mSubscriptionId = getParentSubscription(selectedNode).getId();
        final String serviceName = getParentService(selectedNode).getName();


        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByIoFile(new File(script.getLocalFilePath(serviceName)));
                boolean fileIsEditing = false;

                if (scriptFile != null)
                    fileIsEditing = FileEditorManager.getInstance(mProject).getEditors(scriptFile).length != 0;

                if (!fileIsEditing) {

                    try {

                        File temppath = new File(script.getLocalDirPath(serviceName));
                        temppath.mkdirs();


                        if (script instanceof Script && ((Script) script).getSelfLink() == null) {

                            InputStream is = this.getClass().getResourceAsStream(String.format("/com/microsoftopentechnologies/intellij/templates/%s.js", ((Script) script).getOperation()));
                            final ByteArrayOutputStream buff = new ByteArrayOutputStream();

                            int b;
                            while ((b = is.read()) != -1)
                                buff.write(b);

                            final File tempf = new File(temppath, ((Script) script).getOperation() + ".js");
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
                            final File file = new File(script.getLocalFilePath(serviceName));
                            if (file.exists()) {
                                String[] options = new String[]{"Use remote", "Use local"};
                                int optionDialog = JOptionPane.showOptionDialog(null,
                                        "There is a local copy of the script. Do you want you replace it with the remote version?",
                                        "Edit script",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null,
                                        options,
                                        options[1]);

                                if (optionDialog == JOptionPane.YES_OPTION) {
                                    download = true;
                                }

                            } else {
                                download = true;
                            }


                            if (download) {
                                ProgressManager.getInstance().run(new Task.Backgroundable(mProject, "Loading Mobile Services data...", false) {
                                    @Override
                                    public void run(@NotNull ProgressIndicator progressIndicator) {
                                        progressIndicator.setIndeterminate(true);
                                        progressIndicator.setText("Downloading script");
                                        try {

                                            if (script instanceof Script)
                                                AzureRestAPIManager.getManager().downloadTableScript(mSubscriptionId, serviceName, script.getName(), script.getLocalFilePath(serviceName));
                                            else if (script instanceof CustomAPI)
                                                AzureRestAPIManager.getManager().downloadAPIScript(mSubscriptionId, serviceName, script.getName(), script.getLocalFilePath(serviceName));
                                            else if (script instanceof Job)
                                                AzureRestAPIManager.getManager().downloadJobScript(mSubscriptionId, serviceName, script.getName(), script.getLocalFilePath(serviceName));

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

    public JBMenuItem[] getMenuItems(final Project p, final ServiceTreeItem selectedItem, final DefaultMutableTreeNode selectedNode, final JTree tree) {

        Object[] userObjectPath = selectedNode.getUserObjectPath();

        Subscription subscription = (Subscription) userObjectPath[1];
        Service service = (Service) userObjectPath[2];

        final String serviceName = service.getName();
        final String subscriptionId = subscription.getId().toString();

        if (selectedItem instanceof Service) {
            JBMenuItem newTableMenuItem = new JBMenuItem("Create table");
            JBMenuItem newAPIMenuItem = new JBMenuItem("Create API");
            JBMenuItem newJobMenuItem = new JBMenuItem("Create new job");
            JBMenuItem showLog = new JBMenuItem("Show log");
            JBMenuItem refresh = new JBMenuItem("Refresh");

            final Service item = (Service) selectedItem;

            newTableMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    TableForm form = new TableForm();
                    form.setServiceName(item.getName());
                    form.setSubscriptionId(item.getSubcriptionId());
                    form.setProject(p);

                    ArrayList<String> existingTables = new ArrayList<String>();
                    for (Table table : item.getTables())
                        existingTables.add(table.getName());

                    form.setExistingTableNames(existingTables);

                    form.setAfterSave(new Runnable() {
                        @Override
                        public void run() {
                            selectedNode.removeAllChildren();
                            treeClick(selectedNode);
                        }
                    });

                    UIHelper.packAndCenterJDialog(form);
                    form.setVisible(true);
                }
            });

            newAPIMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    CustomAPIForm form = new CustomAPIForm();
                    form.setServiceName(item.getName());
                    form.setSubscriptionId(item.getSubcriptionId());
                    form.setProject(p);

                    form.setAfterSave(new Runnable() {
                        @Override
                        public void run() {
                            selectedNode.removeAllChildren();
                            treeClick(selectedNode);
                        }
                    });

                    UIHelper.packAndCenterJDialog(form);
                    form.setVisible(true);
                }
            });

            newJobMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JobForm form = new JobForm();
                    form.setServiceName(item.getName());
                    form.setSubscriptionId(item.getSubcriptionId());
                    form.setTitle("Create new Job");

                    form.setAfterSave(new Runnable() {
                        @Override
                        public void run() {
                            selectedNode.removeAllChildren();
                            treeClick(selectedNode);
                        }
                    });

                    UIHelper.packAndCenterJDialog(form);
                    form.setVisible(true);
                }
            });

            showLog.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final ViewLogForm form = new ViewLogForm();

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            form.queryLog(item.getSubcriptionId(), item.getName());
                        }
                    });

                    UIHelper.packAndCenterJDialog(form);
                    form.setVisible(true);

                }
            });

            refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    selectedNode.removeAllChildren();
                    treeClick(selectedNode);
                }
            });

            return new JBMenuItem[]{
                    newTableMenuItem,
                    newAPIMenuItem,
                    newJobMenuItem,
                    showLog,
                    refresh
            };
        }

        if (selectedItem instanceof Table) {


            JBMenuItem editTable = new JBMenuItem("Edit Table");

            final Table table = (Table) selectedItem;

            editTable.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                    ProgressManager.getInstance().run(new Task.Backgroundable(p, "Loading table info", false) {
                        @Override
                        public void run(@NotNull ProgressIndicator progressIndicator) {
                            try {
                                final Table selectedTable = AzureRestAPIManager.getManager().showTableDetails(UUID.fromString(subscriptionId), serviceName, table.getName());

                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        TableForm form = new TableForm();
                                        form.setServiceName(serviceName);
                                        form.setSubscriptionId(UUID.fromString(subscriptionId));
                                        form.setEditingTable(selectedTable);
                                        form.setProject(p);
                                        UIHelper.packAndCenterJDialog(form);
                                        form.setVisible(true);
                                    }
                                });

                            } catch (Throwable ex) {
                                UIHelper.showException("Error creating table", ex);
                            }

                        }
                    });

                }
            });

            return new JBMenuItem[]{editTable};
        }

        if (selectedItem instanceof Script) {
            JBMenuItem uploadScript = new JBMenuItem("Update script");

            final Script script = (Script) selectedItem;
            uploadScript.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                UIHelper.saveScript(p, selectedNode, script, serviceName, subscriptionId);
                            } catch (AzureCmdException e) {
                                UIHelper.showException("Error uploading script:", e);
                            }
                        }
                    });
                }
            });

            return new JBMenuItem[]{
                    uploadScript
            };
        }


        if (selectedItem instanceof CustomAPI) {
            final CustomAPI customAPI = (CustomAPI) selectedItem;

            JBMenuItem uploadAPI = new JBMenuItem("Update Custom API");
            JBMenuItem editAPI = new JBMenuItem("Edit Custom API");

            uploadAPI.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        UIHelper.saveCustomAPI(p, customAPI, serviceName, subscriptionId);
                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error uploading script:", e);
                    }
                }
            });

            editAPI.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final CustomAPIForm form = new CustomAPIForm();
                    form.setEditingCustomAPI(customAPI);
                    form.setServiceName(serviceName);

                    form.setSubscriptionId(UUID.fromString(subscriptionId));
                    form.setProject(p);
                    form.setAfterSave(new Runnable() {
                        @Override
                        public void run() {
                            selectedNode.setUserObject(form.getEditingCustomAPI());
                        }
                    });
                    UIHelper.packAndCenterJDialog(form);
                    form.setVisible(true);
                }
            });

            return new JBMenuItem[]{
                    uploadAPI,
                    editAPI,
            };
        }


        if (selectedItem instanceof Job) {
            final Job job = (Job) selectedItem;

            JBMenuItem uploadJob = new JBMenuItem("Update job");
            JBMenuItem editJob = new JBMenuItem("Edit job");

            uploadJob.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        UIHelper.saveJob(p, job, serviceName, subscriptionId);
                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error uploading script:", e);
                    }
                }
            });

            editJob.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final JobForm form = new JobForm();
                    form.setJob(job);
                    form.setServiceName(serviceName);
                    form.setTitle("Edit job");
                    form.setSubscriptionId(UUID.fromString(subscriptionId));
                    form.setAfterSave(new Runnable() {
                        @Override
                        public void run() {
                            selectedNode.setUserObject(form.getEditingJob());
                        }
                    });
                    form.pack();
                    form.setVisible(true);
                }
            });

            return new JBMenuItem[]{
                    uploadJob,
                    editJob,
            };
        }

        return null;
    }

    public static NodeRenderer getTreeNodeRenderer() {
        return new NodeRenderer() {
            private ImageIcon serviceImg = UIHelper.loadIcon("service.png");
            private ImageIcon tableImg = UIHelper.loadIcon("table.png");
            private ImageIcon scriptImg = UIHelper.loadIcon("script.png");
            private ImageIcon customAPIImg = UIHelper.loadIcon("api.png");
            private ImageIcon jobImg = UIHelper.loadIcon("job.png");
            private ImageIcon msImg = UIHelper.loadIcon("mobileservices.png");
            private ImageIcon vmImg = UIHelper.loadIcon("virtualmachines.png");
            private ImageIcon vmRunImg = UIHelper.loadIcon("virtualmachinerun.png");
            private ImageIcon vmStopImg = UIHelper.loadIcon("virtualmachinestop.png");
            private ImageIcon vmWaitImg = UIHelper.loadIcon("virtualmachinewait.png");
            private ImageIcon vmEndPointImg = UIHelper.loadIcon("endpoint.png");

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
                    } else if (data instanceof AzureType) {

                        switch((AzureType)data) {
                            case MobileServices:
                                setIcon(msImg);
                                break;
                            case VirtualMachines:
                                setIcon(vmImg);
                                break;
                        }

                    } else if (data instanceof VirtualMachine) {
                        VirtualMachine virtualMachine = (VirtualMachine) data;
                        if(virtualMachine.getStatus().equals("Running"))
                            setIcon(vmRunImg);
                        else if(virtualMachine.getStatus().equals("Suspended"))
                            setIcon(vmStopImg);
                        else
                            setIcon(vmWaitImg);
                    } else if (data instanceof Endpoint) {
                        setIcon(vmEndPointImg);
                    }
                }
            }
        };
    }


}