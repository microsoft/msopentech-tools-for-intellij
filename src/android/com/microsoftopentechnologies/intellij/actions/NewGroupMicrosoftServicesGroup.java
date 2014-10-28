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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.helpers.AndroidStudioHelper;
import com.microsoftopentechnologies.intellij.helpers.ServiceCodeReferenceHelper;

import java.io.File;

public class NewGroupMicrosoftServicesGroup extends com.intellij.openapi.actionSystem.DefaultActionGroup {

    @Override
    public void update(AnActionEvent e) {

        try {
            if(AndroidStudioHelper.isAndroidStudio()) {
                e.getPresentation().setVisible(false);
            } else {

                Module module = e.getData(DataKeys.MODULE);

                File moduleFile = new File(module.getModuleFilePath());
                VirtualFile moduleFolder = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile.getParentFile());
                Boolean isAndroidModule = ServiceCodeReferenceHelper.isAndroidGradleModule(moduleFolder);

                e.getPresentation().setVisible(isAndroidModule);
            }
        } catch (Throwable t) {
            e.getPresentation().setVisible(false);
        }
    }

}
