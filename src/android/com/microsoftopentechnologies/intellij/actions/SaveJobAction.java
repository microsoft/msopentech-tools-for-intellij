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


package com.microsoftopentechnologies.intellij.actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.model.Job;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

public class SaveJobAction extends AnAction {
    @Override
    public void update(AnActionEvent event) {
        final JTree tree = UIHelper.getProjectTree();


        Job job = null;
        DefaultMutableTreeNode selectedNode;

        if (tree != null) {
            TreePath tp = tree.getLeadSelectionPath();
            if (tp != null && tp.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                selectedNode = (DefaultMutableTreeNode) tp.getLastPathComponent();
                if (selectedNode.getUserObject() instanceof Job)
                    job = (Job) selectedNode.getUserObject();
            }
        }
        event.getPresentation().setVisible(job != null);
    }

    public void actionPerformed(final AnActionEvent event) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                final JTree tree = UIHelper.getProjectTree();

                try {

                    Job job = null;
                    DefaultMutableTreeNode selectedNode = null;
                    TreePath tp = tree.getLeadSelectionPath();
                    if(tp != null && tp.getLastPathComponent() instanceof DefaultMutableTreeNode){
                        selectedNode = (DefaultMutableTreeNode) tp.getLastPathComponent();
                        if(selectedNode != null && selectedNode.getUserObject() instanceof Job)
                            job = (Job) selectedNode.getUserObject();
                    }

                    if(selectedNode != null && job != null) {
                        PropertiesComponent pc = PropertiesComponent.getInstance(event.getProject());

                        String serviceName = pc.getValue("serviceName");
                        String subscriptionId = pc.getValue("subscriptionId");

                        UIHelper.saveJob(event.getProject(), job, serviceName, subscriptionId);
                    }
                } catch(Throwable ex) {
                    tree.setCursor(Cursor.getDefaultCursor());
                    UIHelper.showException("Error uploading script:", ex);
                }
            }
        });

    }

}
