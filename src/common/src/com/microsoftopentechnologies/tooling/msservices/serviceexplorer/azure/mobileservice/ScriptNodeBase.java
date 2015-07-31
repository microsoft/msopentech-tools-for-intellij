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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.mobileservice;

import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileService;
import com.microsoftopentechnologies.tooling.msservices.model.ms.MobileServiceScriptTreeItem;
import com.microsoftopentechnologies.tooling.msservices.model.ms.Script;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public abstract class ScriptNodeBase extends Node {
    public ScriptNodeBase(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    protected abstract void downloadScript(MobileService mobileService, String scriptName, String localFilePath) throws AzureCmdException;

    protected void onNodeClickInternal(final MobileServiceScriptTreeItem script) {
        // TODO: This function is far too long and confusing. Refactor this to smaller well-defined sub-routines.

        // find the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = findParentByType(MobileServiceNode.class);
        final MobileService mobileService = mobileServiceNode.getMobileService();

        boolean fileIsEditing = DefaultLoader.getIdeHelper().isFileEditing(getProject(), new File(script.getLocalFilePath(mobileService.getName())));

        if (!fileIsEditing) {
            try {
                File temppath = new File(script.getLocalDirPath(mobileService.getName()));
                temppath.mkdirs();

                if (script instanceof Script && ((Script) script).getSelfLink() == null) {
                    InputStream is = this.getClass().getResourceAsStream(
                            String.format("/com/microsoftopentechnologies/tooling/msservices/templates/%s.js",
                                    ((Script) script).getOperation()));
                    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

                    int b;
                    while ((b = is.read()) != -1)
                        buff.write(b);

                    final File tempf = new File(temppath, ((Script) script).getOperation() + ".js");
                    tempf.createNewFile();

                    DefaultLoader.getIdeHelper().saveFile(tempf, buff, this);
                } else {
                    boolean download = false;
                    final File file = new File(script.getLocalFilePath(mobileService.getName()));
                    if (file.exists()) {
                        String[] options = new String[]{"Use remote", "Use local"};
                        int optionDialog = JOptionPane.showOptionDialog(null,
                                "There is a local copy of the script. Do you want you replace it with the remote version?",
                                "Service Explorer",
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
                        DefaultLoader.getIdeHelper().runInBackground(getProject(), "Loading Mobile Services data...", false, true, "Downloading script", new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    downloadScript(mobileService, script.getName(), script.getLocalFilePath(mobileService.getName()));
                                    DefaultLoader.getIdeHelper().openFile(file, ScriptNodeBase.this);

                                } catch (Throwable e) {
                                    DefaultLoader.getUIHelper().showException("Error writing temporal editable file:", e);
                                }
                            }
                        });
                    } else {
                        DefaultLoader.getIdeHelper().openFile(file, ScriptNodeBase.this);
                    }
                }
            } catch (Throwable e) {
                DefaultLoader.getUIHelper().showException("Error writing temporal editable file:", e);
            }
        }
    }
}