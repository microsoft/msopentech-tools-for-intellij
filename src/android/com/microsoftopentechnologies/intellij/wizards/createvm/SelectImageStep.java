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

package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.vm.VirtualMachineImage;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class SelectImageStep extends WizardStep<CreateVMWizardModel> {
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JComboBox imageTypeComboBox;
    private JList imageLabelList;
    private JEditorPane imageDescriptionTextPane;
    private JPanel imageInfoPanel;

    CreateVMWizardModel model;

    private void createUIComponents() {
        imageInfoPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {

                double height = 0;
                for (Component component : this.getComponents()) {
                    height += component.getHeight();
                }

                Dimension preferredSize = super.getPreferredSize();
                preferredSize.setSize(preferredSize.getWidth(), height);
                return preferredSize;
            }
        };
    }

    private enum PublicImages {
        WindowsServer,
        SharePoint,
        BizTalkServer,
        SQLServer,
        VisualStudio,
        Linux,
        Other;

        @Override
        public String toString() {
            switch (this) {
                case WindowsServer:
                    return "Windows Server";
                case BizTalkServer:
                    return "BizTalk Server";
                case SQLServer:
                    return "SQL Server";
                case VisualStudio:
                    return "Visual Studio";
                default:
                    return super.toString();
            }
        }
    }

    private enum MSDNImages {
        BizTalkServer,
        Dynamics,
        VisualStudio,
        Other;

        @Override
        public String toString() {
            switch (this) {
                case BizTalkServer:
                    return "BizTalk Server";
                case VisualStudio:
                    return "Visual Studio";
                default:
                    return super.toString();
            }

        }
    }

    private enum PrivateImages {
        VMImages;

        @Override
        public String toString() {
            switch (this) {
                case VMImages:
                    return "VM Images";
                default:
                    return super.toString();
            }
        }
    }

    Map<Enum, List<VirtualMachineImage>> virtualMachineImages;
    private Project project;

    public SelectImageStep(final CreateVMWizardModel model, Project project) {

        super("Select a Virtual Machine Image", null);

        this.model = model;
        this.project = project;

        model.configStepList(createVmStepsList, 1);

        final ArrayList imageTypeList = new ArrayList();
        imageTypeList.add("Public Images");
        imageTypeList.addAll(Arrays.asList(PublicImages.values()));
        imageTypeList.add("MSDN Images");
        imageTypeList.addAll(Arrays.asList(MSDNImages.values()));
        imageTypeList.add("Private Images");
        imageTypeList.addAll(Arrays.asList(PrivateImages.values()));


        imageTypeComboBox.setModel(new DefaultComboBoxModel(imageTypeList.toArray()) {
            @Override
            public void setSelectedItem(Object o) {
                if (o instanceof Enum) {
                    super.setSelectedItem(o);
                }
            }
        });

        imageTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof Enum) {
                    return super.getListCellRendererComponent(jList, "  " + o.toString(), i, b, b1);
                } else {
                    JLabel label = new JLabel(o.toString());
                    Font f = label.getFont();
                    label.setFont(f.deriveFont(f.getStyle()
                            | Font.BOLD | Font.ITALIC));
                    return label;
                }
            }
        });

        imageTypeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                fillList();
            }
        });

        imageLabelList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
                String cellValue = o.toString();

                if (o instanceof VirtualMachineImage) {
                    VirtualMachineImage virtualMachineImage = (VirtualMachineImage) o;

                    cellValue = String.format("%s (%s)",
                            virtualMachineImage.getLabel(),
                            new SimpleDateFormat("yyyy-MM-dd").format(virtualMachineImage.getPublishedDate().getTime()));
                }

                this.setToolTipText(cellValue);
                return super.getListCellRendererComponent(jList, cellValue, i, b, b1);
            }
        });

        imageLabelList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                VirtualMachineImage virtualMachineImage = (VirtualMachineImage) imageLabelList.getSelectedValue();
                model.setVirtualMachineImage(virtualMachineImage);

                if (virtualMachineImage != null) {
                    imageDescriptionTextPane.setText(model.getHtmlFromVMImage(virtualMachineImage));
                    imageDescriptionTextPane.setCaretPosition(0);
                    model.getCurrentNavigationState().NEXT.setEnabled(true);
                }
            }
        });

        imageDescriptionTextPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                        } catch (Exception e) {
                            UIHelper.showException("Error opening link", e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        if (virtualMachineImages == null) {
            imageTypeComboBox.setEnabled(false);
            model.getCurrentNavigationState().NEXT.setEnabled(false);

            imageLabelList.setListData(new String[]{"loading..."});
            imageLabelList.setEnabled(false);

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading virtual machine images...", false) {
                @Override
                public void run(ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);

                    try {
                        for (VirtualMachineImage virtualMachineImage : AzureSDKManagerImpl.getManager().getVirtualMachineImages(model.getSubscription().getId().toString())) {

                            if (virtualMachineImage.isShowInGui()) {
                                Enum type = null;
                                if (virtualMachineImage.getCategory().equals("Public")) {
                                    for (PublicImages publicImage : PublicImages.values()) {
                                        if (virtualMachineImage.getPublisherName().contains(publicImage.toString())) {
                                            type = publicImage;
                                        } else if (virtualMachineImage.getOperatingSystemType().equals(publicImage.toString())) {
                                            type = publicImage;
                                        }
                                    }

                                    if (type == null) {
                                        type = PublicImages.Other;
                                    }
                                } else if (virtualMachineImage.getCategory().equals("Private")) {
                                    type = PrivateImages.VMImages;
                                } else {
                                    for (MSDNImages msdnImages : MSDNImages.values()) {
                                        if (virtualMachineImage.getPublisherName().contains(msdnImages.toString())) {
                                            type = msdnImages;
                                        } else if (virtualMachineImage.getOperatingSystemType().equals(msdnImages.toString())) {
                                            type = msdnImages;
                                        }
                                    }

                                    if (type == null) {
                                        type = MSDNImages.Other;
                                    }
                                }

                                if (virtualMachineImages == null) {
                                    virtualMachineImages = new HashMap<Enum, List<VirtualMachineImage>>();
                                }

                                if (!virtualMachineImages.containsKey(type)) {
                                    virtualMachineImages.put(type, new ArrayList<VirtualMachineImage>());
                                }

                                virtualMachineImages.get(type).add(virtualMachineImage);
                            }
                        }

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                imageTypeComboBox.setEnabled(true);
                                imageLabelList.setEnabled(true);

                                imageTypeComboBox.setSelectedIndex(1);
                            }
                        });

                    } catch (AzureCmdException e) {
                        UIHelper.showException("Error getting Virtual Machine Images", e);
                    }
                }
            });
        }

        return rootPanel;
    }

    private void fillList() {
        model.getCurrentNavigationState().NEXT.setEnabled(false);

        Enum imageType = (Enum) imageTypeComboBox.getSelectedItem();

        List<VirtualMachineImage> machineImages = virtualMachineImages.get(imageType);
        imageLabelList.setListData(machineImages == null ? new Object[]{} : machineImages.toArray());
        if (machineImages != null && machineImages.size() > 0) {
            imageLabelList.setSelectedIndex(0);
        }
    }
}