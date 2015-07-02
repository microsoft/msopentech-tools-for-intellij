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
package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.forms.TableForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.helpers.Name;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Table;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.MobileServiceNode;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice.TableNode;

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
        form.setServiceName(mobileServiceNode.getMobileService().getName());
        form.setSubscriptionId(mobileServiceNode.getMobileService().getSubcriptionId());
        form.setProject((Project) mobileServiceNode.getProject());

        ArrayList<String> existingTables = new ArrayList<String>();
        for (Table table : mobileServiceNode.getMobileService().getTables())
            existingTables.add(table.getName());

        form.setExistingTableNames(existingTables);

        form.setAfterSave(new Runnable() {
            @Override
            public void run() {
                // refresh the tables node
                mobileServiceNode.getTablesNode().removeAllChildNodes();
                try {
                    mobileServiceNode.loadServiceNode(
                            AzureManagerImpl.getManager().getTableList(
                                    mobileServiceNode.getMobileService().getSubcriptionId(),
                                    mobileServiceNode.getMobileService().getName()),
                            "_tables",
                            MobileServiceNode.TABLES,
                            mobileServiceNode.getTablesNode(),
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

        UIHelperImpl.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}
