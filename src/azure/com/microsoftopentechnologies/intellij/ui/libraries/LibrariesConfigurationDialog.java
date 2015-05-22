package com.microsoftopentechnologies.intellij.ui.libraries;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.ui.components.DefaultDialogWrapper;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class LibrariesConfigurationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JPanel librariesPanel;
    private JBList librariesList;

    private List<AzureLibrary> currentLibs;
    private Module module;

    public LibrariesConfigurationDialog(Module module, List<AzureLibrary> currentLibs) {
        super(true);
        this.currentLibs = currentLibs;
        this.module = module;
        init();
    }

    @Override
    protected void init() {
        librariesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        librariesList = new JBList(currentLibs);
        librariesPanel = ToolbarDecorator.createDecorator(librariesList)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addLibrary();
                    }
                }).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeLibrary();
                    }
                }).setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        editLibrary();
                    }
                }).disableUpDownActions().createPanel();
    }

    private void addLibrary() {
        AddLibraryWizardModel model = new AddLibraryWizardModel(module);
        AddLibraryWizardDialog wizard = new AddLibraryWizardDialog(model);

        wizard.setTitle(message("addLibraryTitle"));
        wizard.show();
        if (wizard.isOK()) {
            AzureLibrary azureLibrary = model.getSelectedLibrary();
            final LibrariesContainer.LibraryLevel level = LibrariesContainer.LibraryLevel.MODULE;
            AccessToken token = WriteAction.start();
            try {
                final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                Library newLibrary = LibrariesContainerFactory.createContainer(modifiableModel).createLibrary(azureLibrary.getName(), level, new ArrayList<OrderRoot>());
                if (model.isExported()) {
                    for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
                        if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                                && azureLibrary.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                            ((ModuleLibraryOrderEntryImpl) orderEntry).setExported(true);
                            break;
                        }
                    }
                }
                Library.ModifiableModel newLibraryModel = newLibrary.getModifiableModel();
                File file = new File(String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, azureLibrary.getLocation()));
                AddLibraryUtility.addLibraryRoot(file, newLibraryModel);
                // if some files already contained in plugin dependencies, take them from there - true for azure sdk library
                if (azureLibrary.getFiles().length > 0) {
                    AddLibraryUtility.addLibraryFiles(new File(String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, "lib")), newLibraryModel, azureLibrary.getFiles());
                }
                newLibraryModel.commit();
                modifiableModel.commit();
                ((DefaultListModel) librariesList.getModel()).addElement(azureLibrary);
            } catch (Exception ex) {
                PluginUtil.displayErrorDialogAndLog(message("error"), message("addLibraryError"), ex);
            } finally {
                token.finish();
            }
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        }
    }

    private void removeLibrary() {
        AzureLibrary azureLibrary = (AzureLibrary) librariesList.getSelectedValue();
        AccessToken token = WriteAction.start();
        try {
            final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
            modifiableModel.getModuleLibraryTable().removeLibrary(modifiableModel.getModuleLibraryTable().getLibraryByName(azureLibrary.getName()));
            modifiableModel.commit();
        } finally {
            token.finish();
        }
        ((DefaultListModel) librariesList.getModel()).removeElement(azureLibrary);
    }

    private void editLibrary() {
        AzureLibrary azureLibrary = (AzureLibrary) librariesList.getSelectedValue();
        final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        OrderEntry libraryOrderEntry = null;
        for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
            if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                    && azureLibrary.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                libraryOrderEntry = orderEntry;
                break;
            }
        }
        if (libraryOrderEntry != null) {
            LibraryPropertiesPanel libraryPropertiesPanel = new LibraryPropertiesPanel(module, azureLibrary, true,
                    ((ModuleLibraryOrderEntryImpl)libraryOrderEntry).isExported());
            DefaultDialogWrapper libraryProperties = new DefaultDialogWrapper(module.getProject(), libraryPropertiesPanel);
            libraryProperties.show();
            if (libraryProperties.isOK()) {
                AccessToken token = WriteAction.start();
                try {
                    ((ModuleLibraryOrderEntryImpl) libraryOrderEntry).setExported(libraryPropertiesPanel.isExported());
                    modifiableModel.commit();
                } finally {
                    token.finish();
                }
                LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
            }
        } else {
            PluginUtil.displayInfoDialog("Library not found", "Library was not found");
        }
    }
}
