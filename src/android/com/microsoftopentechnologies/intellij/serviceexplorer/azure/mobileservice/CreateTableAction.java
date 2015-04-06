package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.Table;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

@Name("Create table")
public class CreateTableAction extends NodeActionListener {
    private MobileServiceNode mobileServiceNode;

    public CreateTableAction(MobileServiceNode mobileServiceNode) {
        this.mobileServiceNode = mobileServiceNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        TableForm form = new TableForm();
        form.setServiceName(mobileServiceNode.mobileService.getName());
        form.setSubscriptionId(mobileServiceNode.mobileService.getSubcriptionId());
        form.setProject((Project) mobileServiceNode.getProject());

        ArrayList<String> existingTables = new ArrayList<String>();
        for (Table table : mobileServiceNode.mobileService.getTables())
            existingTables.add(table.getName());

        form.setExistingTableNames(existingTables);

        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                // refresh the tables node
                mobileServiceNode.tablesNode.removeAllChildNodes();
                try {
                    mobileServiceNode.loadServiceNode(
                            AzureRestAPIManagerImpl.getManager().getTableList(
                                    mobileServiceNode.mobileService.getSubcriptionId(),
                                    mobileServiceNode.mobileService.getName()),
                            "_tables",
                            MobileServiceNode.TABLES,
                            mobileServiceNode.tablesNode,
                            TableNode.class,
                            Table.class);
                } catch (NoSuchMethodException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (IllegalAccessException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (InvocationTargetException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (InstantiationException e1) {
                    mobileServiceNode.handleError(e1);
                } catch (AzureCmdException e1) {
                    mobileServiceNode.handleError(e1);
                }
            }
        });

        DefaultLoader.getUIHelper().packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
