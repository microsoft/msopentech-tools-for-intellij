/**
 * Copyright 2015 Microsoft Open Technologies, Inc.
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
package com.microsoftopentechnologies.intellij.ui.libraries;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AddLibraryUtility {
    static void addLibraryRoot(File file, Library.ModifiableModel libraryModel) {
        if (file.isFile()) {
            libraryModel.addRoot(VfsUtil.getUrlForLibraryRoot(file), OrderRootType.CLASSES);
        } else {
            for (File file0 : file.listFiles()) {
                addLibraryRoot(file0, libraryModel);
            }
        }
    }

    static void addLibraryFiles(File file, Library.ModifiableModel libraryModel, String[] files) {
        List filesList = Arrays.asList(files);
        for (File file0 : file.listFiles()) {
            if (filesList.contains(file0.getName())) {
                addLibraryRoot(file0, libraryModel);
            }
        }
    }
}
