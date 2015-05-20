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
package com.microsoftopentechnologies.intellij.helpers.storage;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.helpers.IDEHelperImpl;
import com.microsoftopentechnologies.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoftopentechnologies.tooling.msservices.model.storage.Table;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class TableExplorerFileEditorProvider implements FileEditorProvider, DumbAware {
    public static Key<Table> TABLE_KEY = new Key<Table>("table");

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        ClientStorageAccount storageAccount = virtualFile.getUserData(IDEHelperImpl.STORAGE_KEY);
        Table table = virtualFile.getUserData(TABLE_KEY);

        return (storageAccount != null && table != null);
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        TableFileEditor tableFileEditor = new TableFileEditor();

        ClientStorageAccount storageAccount = virtualFile.getUserData(IDEHelperImpl.STORAGE_KEY);
        Table table = virtualFile.getUserData(TABLE_KEY);

        tableFileEditor.setTable(table);
        tableFileEditor.setStorageAccount(storageAccount);
        tableFileEditor.setProject(project);

        tableFileEditor.fillGrid();

        return tableFileEditor;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor fileEditor) {
        Disposer.dispose(fileEditor);
    }

    @NotNull
    @Override
    public FileEditorState readState(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void writeState(@NotNull FileEditorState fileEditorState, @NotNull Project project, @NotNull Element element) {
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return "Azure-Storage-Table-Editor";
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
