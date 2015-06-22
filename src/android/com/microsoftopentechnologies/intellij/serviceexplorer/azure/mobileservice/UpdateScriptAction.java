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
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Script;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.TableScriptNode;

import javax.swing.*;
import java.io.File;

@Name("Update script")
public class UpdateScriptAction extends NodeActionListener {
    private TableScriptNode tableScriptNode;

    public UpdateScriptAction(TableScriptNode tableScriptNode) {
        this.tableScriptNode = tableScriptNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = tableScriptNode.findParentByType(MobileServiceNode.class);
        final MobileService mobileService = mobileServiceNode.getMobileService();

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    saveScript(
                            (Project) tableScriptNode.getProject(),
                            tableScriptNode.getScript(),
                            mobileService.getName(),
                            mobileService.getSubcriptionId());
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error uploading script:", e);
                }
            }
        });
    }

    public void saveScript(Project project, final Script script, final String serviceName, final String subscriptionId) throws AzureCmdException {
        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(script.getLocalFilePath(serviceName)));
        if (editorFile != null) {
            FileEditor[] fe = FileEditorManager.getInstance(project).getAllEditors(editorFile);

            if (fe.length > 0 && fe[0].isModified()) {
                int i = JOptionPane.showConfirmDialog(
                        null,
                        "The file is modified. Do you want to save pending changes?",
                        "Service Explorer",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

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
                        AzureManagerImpl.getManager().uploadTableScript(
                                subscriptionId,
                                serviceName,
                                script.getName(),
                                script.getLocalFilePath(serviceName));
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (script.getSelfLink() == null)
                                    script.setSelfLink("");
                            }
                        });
                    } catch (AzureCmdException e) {
                        DefaultLoader.getUIHelper().showException("Error uploading script", e);
                    }
                }
            });
        }
    }
}
