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
package com.microsoftopentechnologies.intellij.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AzureModuleType extends ModuleType<AzureModuleBuilder> {
    @NonNls public static final String AZURE_MODULE = "AZURE_MODULE";
    public static final String MODULE_NAME = "Azure Module";
    public static final Icon AZURE_MODULE_ICON = IconLoader.getIcon("/icons/ProjectFolder.png");
    private static AzureModuleType MODULE_TYPE = new AzureModuleType();

    public static ModuleType getModuleType() {
        return ModuleTypeManager.getInstance().findByID(AZURE_MODULE);
    }

    public AzureModuleType() {
        this(AZURE_MODULE);
    }

    protected AzureModuleType(@NonNls String id) {
        super(id);
    }

    public static ModuleType getInstance() {
        return MODULE_TYPE;
    }

    @NotNull
    @Override
    public AzureModuleBuilder createModuleBuilder() {
        return new AzureModuleBuilder();
    }

    @NotNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @NotNull
    @Override
    public String getDescription() {
        return ProjectBundle.message("module.type.java.description");
    }

    @Override
    public Icon getBigIcon() {
        return getAzureModuleIcon();
    }

    @Override
    public Icon getNodeIcon(boolean isOpened) {
        return getAzureModuleNodeIconClosed();
    }

    private static Icon getAzureModuleIcon() {
        return AZURE_MODULE_ICON;
    }

    private static Icon getAzureModuleNodeIconClosed() {
        return AZURE_MODULE_ICON;
    }

    public static boolean isAzureModule(@NotNull Module module) {
        return AZURE_MODULE.equals(ModuleType.get(module).getId());
    }
}
