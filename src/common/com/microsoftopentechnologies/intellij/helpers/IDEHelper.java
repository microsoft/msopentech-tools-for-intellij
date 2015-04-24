package com.microsoftopentechnologies.intellij.helpers;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoftopentechnologies.intellij.helpers.aadauth.BrowserLauncher;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.StorageServiceTreeItem;
import com.microsoftopentechnologies.intellij.serviceexplorer.Node;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface IDEHelper {
    void openFile(File file, Node node);

    void runInBackground(Object project, String name, boolean canBeCancelled, boolean isIndeterminate, String indicatorText, Runnable runnable);

    void saveFile(File file, ByteArrayOutputStream buff, Node node);

    void replaceInFile(Object module, Pair<String, String>... replace);

    void copyJarFiles2Module(Object moduleObject, File zipFile) throws IOException;

    boolean isFileEditing(Object projectObject, File file);

    <T extends StorageServiceTreeItem> void  openItem(Object projectObject, StorageAccount storageAccount, T blobContainer, String itemType, final String itemName,
                                               final String iconName);

    public <T extends StorageServiceTreeItem> Object getOpenedFile(Object projectObject, StorageAccount storageAccount, T blobContainer);

    public void closeFile(Object projectObject, Object openedFile);

    void invokeAuthLauncherTask(Object projectObject, BrowserLauncher browserLauncher, String windowTitle);

    void invokeBackgroundLoader(Object projectObject, Node node, SettableFuture<List<Node>> future, String name);

    void invokeLater(Runnable runnable);

    void invokeAndWait(Runnable runnable);

    void executeOnPooledThread(Runnable runnable);

    String getProperty(String name);

    String getProperty(String name, String defaultValue);

    void setProperty(String name, String value);

    void unsetProperty(String name);

    boolean isPropertySet(String name);

    String promptForOpenSSLPath();
}
