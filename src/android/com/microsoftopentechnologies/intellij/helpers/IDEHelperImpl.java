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
package com.microsoftopentechnologies.intellij.helpers;

import com.google.common.collect.ImmutableMap;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoftopentechnologies.intellij.forms.OpenSSLFinderForm;
import com.microsoftopentechnologies.intellij.helpers.storage.*;
import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.IDEHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.Nullable;
import com.microsoftopentechnologies.tooling.msservices.helpers.ServiceCodeReferenceHelper;
import com.microsoftopentechnologies.tooling.msservices.model.storage.*;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class IDEHelperImpl implements IDEHelper {
    public static Key<ClientStorageAccount> STORAGE_KEY = new Key<ClientStorageAccount>("clientStorageAccount");
    private Map<Class<? extends StorageServiceTreeItem>, Key<? extends StorageServiceTreeItem>> name2Key = ImmutableMap.of(BlobContainer.class, BlobExplorerFileEditorProvider.CONTAINER_KEY,
            Queue.class, QueueExplorerFileEditorProvider.QUEUE_KEY,
            Table.class, TableExplorerFileEditorProvider.TABLE_KEY);

    @Override
    public void openFile(@NotNull File file, @NotNull final Node node) {
        final VirtualFile finalEditfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    openFile(node.getProject(), finalEditfile);
                } finally {
                    node.setLoading(false);
                }
            }
        });
    }

    @Override
    public void saveFile(@NotNull final File file, @NotNull final ByteArrayOutputStream buff, @NotNull final Node node) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    final VirtualFile editfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                    if (editfile != null) {
                        editfile.setWritable(true);
                        editfile.setBinaryContent(buff.toByteArray());

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                FileEditorManager.getInstance((Project) node.getProject()).openFile(editfile, true);
                            }
                        });
                    }
                } catch (Throwable e) {
                    DefaultLoader.getUIHelper().showException("Error writing temporal editable file:", e);
                } finally {
                    node.setLoading(false);
                }
            }
        });
    }

    @Override
    public void replaceInFile(@NotNull Object moduleObject, @NotNull Pair<String, String>... replace) {
        Module module = (Module) moduleObject;

        if (module.getModuleFile() != null && module.getModuleFile().getParent() != null) {
            VirtualFile vf = module.getModuleFile().getParent().findFileByRelativePath(ServiceCodeReferenceHelper.STRINGS_XML);

            if (vf != null) {
                FileDocumentManager fdm = FileDocumentManager.getInstance();
                com.intellij.openapi.editor.Document document = fdm.getDocument(vf);

                if (document != null) {
                    String content = document.getText();

                    for (Pair<String, String> pair : replace) {
                        content = content.replace(pair.getLeft(), pair.getRight());
                    }

                    document.setText(content);
                    fdm.saveDocument(document);
                }
            }
        }
    }

    @Override
    public void copyJarFiles2Module(@NotNull Object moduleObject, @NotNull File zipFile, @NotNull String zipPath)
            throws IOException {
        Module module = (Module) moduleObject;
        final VirtualFile moduleFile = module.getModuleFile();

        if (moduleFile != null) {
            moduleFile.refresh(false, false);

            final VirtualFile moduleDir = module.getModuleFile().getParent();

            if (moduleDir != null) {
                moduleDir.refresh(false, false);

                copyJarFiles(module, moduleDir, zipFile, zipPath);
            }
        }
    }

    @Override
    public boolean isFileEditing(@NotNull Object projectObject, @NotNull File file) {
        VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        boolean fileIsEditing = false;

        if (scriptFile != null) {
            fileIsEditing = FileEditorManager.getInstance((Project) projectObject).getEditors(scriptFile).length != 0;
        }

        return fileIsEditing;
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(@NotNull Object projectObject,
                                                            @Nullable ClientStorageAccount storageAccount,
                                                            @NotNull T item,
                                                            @Nullable String itemType,
                                                            @NotNull final String itemName,
                                                            @Nullable final String iconName) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(STORAGE_KEY, storageAccount);

        itemVirtualFile.setFileType(new FileType() {
            @NotNull
            @Override
            public String getName() {
                return itemName;
            }

            @NotNull
            @Override
            public String getDescription() {
                return itemName;
            }

            @NotNull
            @Override
            public String getDefaultExtension() {
                return "";
            }

            @Nullable
            @Override
            public Icon getIcon() {
                return UIHelperImpl.loadIcon(iconName);
            }

            @Override
            public boolean isBinary() {
                return true;
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
                return "UTF8";
            }
        });

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public void openItem(@NotNull final Object projectObject, @NotNull final Object itemVirtualFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).openFile((VirtualFile) itemVirtualFile, true, true);
            }
        }, ModalityState.any());
    }

    @Nullable
    @Override
    public <T extends StorageServiceTreeItem> Object getOpenedFile(@NotNull Object projectObject,
                                                                   @NotNull ClientStorageAccount storageAccount,
                                                                   @NotNull T item) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            T editedItem = editedFile.getUserData((Key<T>) name2Key.get(item.getClass()));
            ClientStorageAccount editedStorageAccount = editedFile.getUserData(STORAGE_KEY);

            if (editedStorageAccount != null
                    && editedItem != null
                    && editedStorageAccount.getName().equals(storageAccount.getName())
                    && editedItem.getName().equals(item.getName())) {
                return editedFile;
            }
        }

        return null;
    }

    @Override
    public void closeFile(@NotNull final Object projectObject, @NotNull final Object openedFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).closeFile((VirtualFile) openedFile);
            }
        }, ModalityState.any());
    }

    @Override
    public void refreshQueue(@NotNull final Object projectObject, @NotNull final ClientStorageAccount storageAccount,
                             @NotNull final Queue queue) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount, queue);
                if (file != null) {
                    final QueueFileEditor queueFileEditor = (QueueFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            queueFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }


    @Override
    public void refreshBlobs(@NotNull final Object projectObject, @NotNull final ClientStorageAccount storageAccount,
                             @NotNull final BlobContainer container) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount, container);
                if (file != null) {
                    final BlobExplorerFileEditor containerFileEditor = (BlobExplorerFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            containerFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void refreshTable(@NotNull final Object projectObject, @NotNull final ClientStorageAccount storageAccount,
                             @NotNull final Table table) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount, table);
                if (file != null) {
                    final TableFileEditor tableFileEditor = (TableFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tableFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @Override
    public void runInBackground(@Nullable final Object project, @NotNull final String name, final boolean canBeCancelled,
                                final boolean isIndeterminate, @Nullable final String indicatorText,
                                final Runnable runnable) {
        // background tasks via ProgressManager can be scheduled only on the
        // dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable((Project) project,
                        name, canBeCancelled) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        if (isIndeterminate) {
                            indicator.setIndeterminate(true);
                        }

                        if (indicatorText != null) {
                            indicator.setText(indicatorText);
                        }

                        runnable.run();
                    }
                });
            }
        }, ModalityState.any());
    }

    @Nullable
    @Override
    public String getProperty(@NotNull String name) {
        return PropertiesComponent.getInstance().getValue(name);
    }

    @NotNull
    @Override
    public String getProperty(@NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance().getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value) {
        PropertiesComponent.getInstance().setValue(name, value);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().saveSettings();
            }
        }, ModalityState.any());
    }

    @Override
    public void unsetProperty(@NotNull String name) {
        PropertiesComponent.getInstance().unsetValue(name);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().saveSettings();
            }
        }, ModalityState.any());
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return PropertiesComponent.getInstance().isValueSet(name);
    }

    @Nullable
    @Override
    public String getProperty(@NotNull Object projectObject, @NotNull String name) {
        return PropertiesComponent.getInstance((Project) projectObject).getValue(name);
    }

    @NotNull
    @Override
    public String getProperty(@NotNull Object projectObject, @NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance((Project) projectObject).getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull Object projectObject, @NotNull String name, @NotNull String value) {
        final Project project = (Project) projectObject;
        PropertiesComponent.getInstance(project).setValue(name, value);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                project.save();
            }
        }, ModalityState.any());
    }

    @Override
    public void unsetProperty(@NotNull Object projectObject, @NotNull String name) {
        final Project project = (Project) projectObject;
        PropertiesComponent.getInstance(project).unsetValue(name);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                project.save();
            }
        }, ModalityState.any());
    }

    @Override
    public boolean isPropertySet(@NotNull Object projectObject, @NotNull String name) {
        return PropertiesComponent.getInstance((Project) projectObject).isValueSet(name);
    }

    @NotNull
    @Override

    public String promptForOpenSSLPath() {
        OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm();
        openSSLFinderForm.setModal(true);
        UIHelperImpl.packAndCenterJDialog(openSSLFinderForm);
        openSSLFinderForm.setVisible(true);

        return getProperty("MSOpenSSLPath", "");
    }

    @Nullable
    @Override
    public String[] getProperties(@NotNull String name) {
        return PropertiesComponent.getInstance().getValues(name);
    }

    @Override
    public void setProperties(@NotNull String name, @NotNull String[] value) {
        PropertiesComponent.getInstance().setValues(name, value);
        ApplicationManager.getApplication().saveSettings();
    }

    private static void openFile(@NotNull final Object projectObject, @Nullable final VirtualFile finalEditfile) {
        try {
            if (finalEditfile != null) {
                finalEditfile.setWritable(true);

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        FileEditorManager.getInstance((Project) projectObject).openFile(finalEditfile, true);
                    }
                });
            }
        } catch (Throwable e) {
            DefaultLoader.getUIHelper().showException("Error writing temporal editable file:", e);
        }
    }

    private static void copyJarFiles(@NotNull final Module module, @NotNull VirtualFile baseDir,
                                     @NotNull File zipFile, @NotNull String zipPath)
            throws IOException {
        if (baseDir.isDirectory()) {
            final ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();

                if (!zipEntry.isDirectory() && zipEntry.getName().startsWith(zipPath) &&
                        zipEntry.getName().endsWith(".jar") &&
                        !(zipEntry.getName().endsWith("-sources.jar") || zipEntry.getName().endsWith("-javadoc.jar"))) {
                    VirtualFile libsVf = null;

                    for (VirtualFile vf : baseDir.getChildren()) {
                        if (vf.getName().equals("libs")) {
                            libsVf = vf;
                            break;
                        }
                    }

                    if (libsVf == null) {
                        libsVf = baseDir.createChildDirectory(module.getProject(), "libs");
                    }

                    final VirtualFile libs = libsVf;
                    final String fileName = zipEntry.getName().split("/")[1];

                    if (libs.findChild(fileName) == null) {
                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            InputStream mobileserviceInputStream = zip.getInputStream(zipEntry);
                                            VirtualFile msVF = libs.createChildData(module.getProject(), fileName);
                                            msVF.setBinaryContent(getArray(mobileserviceInputStream));
                                        } catch (Throwable ex) {
                                            DefaultLoader.getUIHelper().showException("Error trying to configure Azure Mobile Services", ex);
                                        }
                                    }
                                });
                            }
                        }, ModalityState.defaultModalityState());
                    }
                }
            }
        }
    }

    @NotNull
    private static byte[] getArray(@NotNull InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }
}