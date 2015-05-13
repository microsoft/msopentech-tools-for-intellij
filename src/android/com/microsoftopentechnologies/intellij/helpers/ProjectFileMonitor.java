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

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ProjectFileMonitor implements VirtualFileListener {
    private static ProjectFileMonitor projectFileMonitor = null;
    private static ReentrantLock instanceLock = new ReentrantLock();

    private Map<Project, List<VirtualFileListener>> projectFileListenersMap = new HashMap<Project, List<VirtualFileListener>>();
    private ReentrantLock projectFileListenersMapLock = new ReentrantLock();

    private boolean fileListenerRegistered = false;
    private ReentrantLock registrationLock = new ReentrantLock();

    private interface FileListenerDelegate {
        void run(VirtualFileListener fileListener);
    }

    private ProjectFileMonitor() {
    }

    public void addProjectFileListener(Project project, VirtualFileListener fileListener) {
        projectFileListenersMapLock.lock();

        try {
            // lazily register this object with the virtual file manager
            if (!fileListenerRegistered) {
                registrationLock.lock();

                try {
                    if (!fileListenerRegistered) {
                        VirtualFileManager.getInstance().addVirtualFileListener(this);
                        fileListenerRegistered = true;
                    }
                } finally {
                    registrationLock.unlock();
                }
            }

            if (!projectFileListenersMap.containsKey(project)) {
                projectFileListenersMap.put(project, new ArrayList<VirtualFileListener>());
            }

            List<VirtualFileListener> fileListeners = projectFileListenersMap.get(project);

            if (!fileListeners.contains(fileListener)) {
                fileListeners.add(fileListener);
            }
        } finally {
            projectFileListenersMapLock.unlock();
        }
    }

    public void removeProjectFileListener(Project project, VirtualFileListener fileListener) {
        projectFileListenersMapLock.lock();

        try {
            if (projectFileListenersMap.containsKey(project)) {
                List<VirtualFileListener> fileListeners = projectFileListenersMap.get(project);

                if (fileListeners.contains(fileListener)) {
                    fileListeners.remove(fileListener);

                    if (fileListeners.isEmpty()) {
                        projectFileListenersMap.remove(project);
                    }
                }
            }

            // if there are no more project file listeners then unregister this object
            // from the virtual file manager
            if (projectFileListenersMap.isEmpty()) {
                registrationLock.lock();

                try {
                    if (projectFileListenersMap.isEmpty()) {
                        VirtualFileManager.getInstance().removeVirtualFileListener(this);
                        fileListenerRegistered = false;
                    }
                } finally {
                    registrationLock.unlock();
                }
            }
        } finally {
            projectFileListenersMapLock.unlock();
        }
    }

    private void fireEventForProject(VirtualFileEvent fileEvent, FileListenerDelegate action) {
        projectFileListenersMapLock.lock();

        try {
            Project project = guessProjectForFile(fileEvent.getFile());

            if (project != null && projectFileListenersMap.containsKey(project)) {
                ImmutableList<VirtualFileListener> fileListeners = ImmutableList.copyOf(projectFileListenersMap.get(project));

                for (VirtualFileListener fileListener : fileListeners) {
                    action.run(fileListener);
                }
            }
        } finally {
            projectFileListenersMapLock.unlock();
        }
    }

    private Project guessProjectForFile(VirtualFile virtualFile) {
        ProjectManager projectManager = ProjectManager.getInstance();

        if (projectManager == null) {
            return null;
        }

        Project[] openProjects = projectManager.getOpenProjects();
        if (openProjects.length == 0) {
            return null;
        }

        // if there's only one project open then we assume this file belongs to
        // that project
        if (openProjects.length == 1 && !openProjects[0].isDisposed()) {
            return openProjects[0];
        }

        // for each open project, see if this file resides in a sub-folder of the project's
        // root folder; if yes, then chances are, this file belongs to that project; we
        // don't use ProjectLocator.getInstance().guessProjectForFile(virtualFile) because
        // when you have multiple projects open it guesses the wrong project for the new files
        // when creating a new project
        String filePath = virtualFile.getCanonicalPath();

        if (filePath != null) {
            filePath = filePath.toLowerCase();

            for (Project openProject : openProjects) {
                if (openProject.isInitialized() && !openProject.isDisposed()) {
                    String projectPath = openProject.getBaseDir().getCanonicalPath();

                    if (projectPath != null && filePath.startsWith(projectPath.toLowerCase())) {
                        return openProject;
                    }
                }
            }
        }

        return null;
    }

    public static ProjectFileMonitor getInstance() {
        if (projectFileMonitor == null) {
            instanceLock.lock();

            try {
                if (projectFileMonitor == null) {
                    projectFileMonitor = new ProjectFileMonitor();
                }
            } finally {
                instanceLock.unlock();
            }
        }

        return projectFileMonitor;
    }

    public void propertyChanged(@NotNull final VirtualFilePropertyEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.propertyChanged(event);
            }
        });
    }

    public void contentsChanged(@NotNull final VirtualFileEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.contentsChanged(event);
            }
        });
    }

    public void fileCreated(@NotNull final VirtualFileEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.fileCreated(event);
            }
        });
    }

    public void fileDeleted(@NotNull final VirtualFileEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.fileDeleted(event);
            }
        });
    }

    public void fileMoved(@NotNull final VirtualFileMoveEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.fileMoved(event);
            }
        });
    }

    public void fileCopied(@NotNull final VirtualFileCopyEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.fileCopied(event);
            }
        });
    }

    public void beforePropertyChange(@NotNull final VirtualFilePropertyEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.beforePropertyChange(event);
            }
        });
    }

    public void beforeContentsChange(@NotNull final VirtualFileEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.beforeContentsChange(event);
            }
        });
    }

    public void beforeFileDeletion(@NotNull final VirtualFileEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.beforeFileDeletion(event);
            }
        });
    }

    public void beforeFileMovement(@NotNull final VirtualFileMoveEvent event) {
        fireEventForProject(event, new FileListenerDelegate() {
            @Override
            public void run(VirtualFileListener fileListener) {
                fileListener.beforeFileMovement(event);
            }
        });
    }
}