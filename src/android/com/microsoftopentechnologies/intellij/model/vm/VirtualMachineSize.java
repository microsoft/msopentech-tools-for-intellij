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
package com.microsoftopentechnologies.intellij.model.vm;

import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;
import org.jetbrains.annotations.NotNull;

public class VirtualMachineSize implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String label;
    private int cores;
    private int memoryInMB;

    public VirtualMachineSize(@NotNull String name, @NotNull String label, int cores, int memoryInMB) {
        this.name = name;
        this.label = label;
        this.cores = cores;
        this.memoryInMB = memoryInMB;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    public int getCores() {
        return cores;
    }

    public int getMemoryInMB() {
        return memoryInMB;
    }

    @Override
    public String toString() {
        return label + (loading ? " (loading...)" : "");
    }
}