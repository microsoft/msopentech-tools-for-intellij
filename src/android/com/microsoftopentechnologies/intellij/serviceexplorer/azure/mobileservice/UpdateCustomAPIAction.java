package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.CustomAPINode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;

import javax.swing.*;
import java.io.File;
import java.util.UUID;

@Name("Update Custom API")
public class UpdateCustomAPIAction extends NodeActionListener {
    private CustomAPINode customAPINode;

    public UpdateCustomAPIAction(CustomAPINode customAPINode) {
        this.customAPINode = customAPINode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        try {
            // get the parent MobileServiceNode node
            MobileServiceNode mobileServiceNode = (MobileServiceNode) customAPINode.findParentByType(MobileServiceNode.class);
            MobileService mobileService = mobileServiceNode.getMobileService();
            saveCustomAPI((Project) customAPINode.getProject(), mobileService.getName(), mobileService.getSubcriptionId());
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException("Error uploading script:", e1);
        }
    }

    public void saveCustomAPI(Project project, final String serviceName, final UUID subscriptionId) throws AzureCmdException {
        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(customAPINode.getCustomAPI().getLocalFilePath(serviceName)));
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
                public void run(ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        AzureRestAPIManagerImpl.getManager().uploadAPIScript(subscriptionId, serviceName, customAPINode.getCustomAPI().getName(),
                                customAPINode.getCustomAPI().getLocalFilePath(serviceName));
                    } catch (AzureCmdException e) {
                        DefaultLoader.getUIHelper().showException("Error uploading script", e);
                    }
                }
            });
        }
    }
}
