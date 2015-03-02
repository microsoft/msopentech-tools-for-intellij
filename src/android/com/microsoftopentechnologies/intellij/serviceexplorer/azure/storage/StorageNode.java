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

package com.microsoftopentechnologies.intellij.serviceexplorer.azure.storage;

import com.microsoftopentechnologies.intellij.model.vm.StorageAccount;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;

public class StorageNode extends Node {
    private static final String WAIT_ICON_PATH = "storageaccount.png";
    private final StorageAccount storageAccount;

    public StorageNode(Node parent, StorageAccount sm) {
        super(sm.getName(), sm.getName(), parent, WAIT_ICON_PATH, true);
        this.storageAccount = sm;
    }
}
