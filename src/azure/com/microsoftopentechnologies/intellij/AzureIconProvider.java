/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.microsoftopentechnologies.intellij.module.AzureModuleType;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AzureIconProvider extends IconProvider implements DumbAware {
    public static final Icon WorkerRole = IconLoader.getIcon("/icons/RoleFolder.gif");

    @Override
    public Icon getIcon(@NotNull final PsiElement element, final int flags) {
        if (element instanceof PsiDirectory) {
            final PsiDirectory psiDirectory = (PsiDirectory) element;
            final VirtualFile vFile = psiDirectory.getVirtualFile();
            final Project project = psiDirectory.getProject();
            Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(vFile);
            if (module != null && AzureModuleType.AZURE_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)) && PluginUtil.isRoleFolder(vFile, module)) {
                return WorkerRole;
            }
        }
        return null;
    }
}