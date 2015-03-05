package com.microsoftopentechnologies.intellij.helpers.storage;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.Key;
import com.microsoftopentechnologies.intellij.model.storage.BlobContainer;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class BlobExplorerFileEditor implements FileEditor {
    private JPanel mainPanel;
    private JTextField queryTextField;
    private JButton backButton;
    private JTable blobListTable;
    private JButton queryButton;

    private StorageAccount storageAccount;
    private BlobContainer blobContainer;

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return blobListTable;
    }

    @NotNull
    @Override
    public String getName() {
        return blobContainer.getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {}

    @Override
    public void deselectNotify() {}

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {}

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {}

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {

    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setBlobContainer(BlobContainer blobContainer) {
        this.blobContainer = blobContainer;
    }
}
