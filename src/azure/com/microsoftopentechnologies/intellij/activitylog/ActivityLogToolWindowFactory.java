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

package com.microsoftopentechnologies.intellij.activitylog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.PlatformColors;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventListener;
import com.microsoftopentechnologies.intellij.AzurePlugin;
import com.microsoftopentechnologies.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class ActivityLogToolWindowFactory implements ToolWindowFactory {
    public static final String ACTIVITY_LOG_WINDOW = "Azure Activity Log";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss", Locale.getDefault());

    private TableView<DeploymentTableItem> table;
    private HashMap<String, DeploymentTableItem> rows = new HashMap<String, DeploymentTableItem>();
    private Project project;

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        this.project = project;
        table = new TableView<DeploymentTableItem>(new ListTableModel<DeploymentTableItem>(DESC, PROGRESS, STATUS, START_TIME));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // add mouse listener for links in table
        table.addMouseListener(createTableMouseListener());

        toolWindow.getComponent().add(new JBScrollPane(table));
        registerDeploymentListener();
    }

    private MouseListener createTableMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.getSelectedColumn() == 2) {
                    DeploymentTableItem item = table.getSelectedObject();
                    if (item != null && item.link != null) {
                        try {
                            Desktop.getDesktop().browse(URI.create(item.link));
                        } catch (IOException e1) {
                            PluginUtil.displayErrorDialogAndLog(message("error"), message("error"), e1);
                        }
                    }
                }
            }
        };
    }

    public void registerDeploymentListener() {
        AzurePlugin.addDeploymentEventListener(
                new DeploymentEventListener() {

                    @Override
                    public void onDeploymentStep(final DeploymentEventArgs args) {
                        // unique identifier for deployment
                        String key = args.getId() + args.getStartTime().getTime();
                        if (rows.containsKey(key)) {
                            final DeploymentTableItem item = rows.get(key);
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    item.progress += args.getDeployCompleteness();
                                    if (args.getDeployMessage().equalsIgnoreCase(message("runStatus"))) {
                                        String html = String.format("%s%s%s%s", "  ", "<html><a href=\"" + args.getDeploymentURL() + "\">", message("runStatusVisible"), "</a></html>");
                                        item.description = message("runStatusVisible");
                                        item.link = args.getDeploymentURL();
                                        if (!ToolWindowManager.getInstance(project).getToolWindow(ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW).isVisible()) {
                                            ToolWindowManager.getInstance(project).notifyByBalloon(ACTIVITY_LOG_WINDOW, MessageType.INFO, html, null,
                                                    new BrowserHyperlinkListener());
                                        }
                                    } else {
                                        item.description = args.getDeployMessage();
                                    }
                                    table.getListTableModel().fireTableDataChanged();
                                }
                            });
                        } else {
                            final DeploymentTableItem item = new DeploymentTableItem(args.getId(), args.getDeployMessage(),
                                    dateFormat.format(args.getStartTime()), args.getDeployCompleteness());
                            rows.put(key, item);
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    table.getListTableModel().addRow(item);
                                }
                            });
                        }
                    }
                });
    }

    private class ProgressBarRenderer implements TableCellRenderer {
        private final JProgressBar progressBar = new JProgressBar();
        private final JLabel label = new JLabel();

        public ProgressBarRenderer() {
            progressBar.setMaximum(100);
        }

        @Override
        public Component getTableCellRendererComponent(@NotNull JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if ((Integer) value < 100) {
                progressBar.setValue((Integer) value);
                return progressBar;
            } else {
                label.setText("");
                return label;
            }
        }
    }

    private class LinkRenderer implements TableCellRenderer {
        private final JLabel label = new JLabel();

        public LinkRenderer() {
            label.setForeground(PlatformColors.BLUE);
            Font font = label.getFont();
            Map attributes = font.getAttributes();
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            label.setFont(font.deriveFont(attributes));
        }

        @Override
        public Component getTableCellRendererComponent(@NotNull JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            label.setText((String) value);
            return label;
        }
    }

    private final ColumnInfo<DeploymentTableItem, String> DESC = new ColumnInfo<DeploymentTableItem, String>(message("desc")) {
        public String valueOf(DeploymentTableItem object) {
            return object.deploymentId;
        }
    };

    private final ColumnInfo<DeploymentTableItem, Integer> PROGRESS = new ColumnInfo<DeploymentTableItem, Integer>("Progress") {
        private TableCellRenderer renderer = new ProgressBarRenderer();

        public Integer valueOf(DeploymentTableItem object) {
            return object.progress;
        }


        public TableCellRenderer getRenderer(DeploymentTableItem object) {
            return renderer;
        }
    };

    private final ColumnInfo<DeploymentTableItem, String> STATUS = new ColumnInfo<DeploymentTableItem, String>(message("status")) {
        private TableCellRenderer renderer = new LinkRenderer();

        public String valueOf(DeploymentTableItem object) {
            return object.description;
        }

        public TableCellRenderer getRenderer(DeploymentTableItem object) {
            if (object.link != null) {
                return renderer;
            } else {
                return super.getRenderer(object);
            }
        }
    };

    private final ColumnInfo<DeploymentTableItem, String> START_TIME = new ColumnInfo<DeploymentTableItem, String>(message("startTime")) {
        public String valueOf(DeploymentTableItem object) {
            return object.startDate;
        }
    };

    private class DeploymentTableItem {
        private String deploymentId;
        private String description;
        private String startDate;
        private String link;
        private int progress;

        public DeploymentTableItem(String deploymentId, String description, String startDate, int progress) {
            this.deploymentId = deploymentId;
            this.description = description;
            this.startDate = startDate;
            this.progress = progress;
        }
    }
}
