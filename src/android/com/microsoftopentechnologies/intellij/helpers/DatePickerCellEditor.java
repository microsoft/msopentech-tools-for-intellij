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

import com.intellij.ui.JBColor;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.DateSelectionModel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;

public abstract class DatePickerCellEditor extends DefaultCellEditor {

    Constructor constructor;
    Object value;

    public DatePickerCellEditor() {
        super(new JTextField());
        this.getComponent().setName("Table.editor");
    }

    @Override
    public boolean stopCellEditing() {
        String var1 = (String) super.getCellEditorValue();

        try {
            if ("".equals(var1)) {
                if (this.constructor.getDeclaringClass() == String.class) {
                    this.value = var1;
                }

                super.stopCellEditing();
            }

            checkAccess(this.constructor.getModifiers());
            this.value = this.constructor.newInstance(var1);
        } catch (Exception var3) {
            ((JComponent) this.getComponent()).setBorder(new LineBorder(JBColor.RED));
            return false;
        }

        return super.stopCellEditing();
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object value, boolean b, int row, int col) {
        this.value = null;
        ((JComponent) this.getComponent()).setBorder(new LineBorder(JBColor.BLACK));

        try {
            Class columnClass = jTable.getColumnClass(col);
            if (columnClass == Object.class) {
                columnClass = String.class;
            }

            checkPackageAccess(columnClass);
            checkAccess(columnClass.getModifiers());
            this.constructor = columnClass.getConstructor(String.class);
        } catch (Exception ignored) {
            return null;
        }

        final Component component = super.getTableCellEditorComponent(jTable, value, b, row, col);

        if (!isCellDate(jTable, row, col)) {
            return component;
        }

        JButton button = new JButton("...");
        button.setPreferredSize(new Dimension(button.getPreferredSize().height, 40));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JXMonthView monthView = new JXMonthView();
                monthView.setSelectionMode(DateSelectionModel.SelectionMode.SINGLE_SELECTION);
                monthView.setTraversable(true);
                monthView.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        String date = new SimpleDateFormat().format(monthView.getSelectionDate());
                        ((JTextField) component).setText(date);
                    }
                });

                JDialog frame = new JDialog();
                frame.getContentPane().add(monthView);
                frame.setModal(true);
                frame.setAlwaysOnTop(true);
                frame.setMinimumSize(monthView.getPreferredSize());
                UIHelperImpl.packAndCenterJDialog(frame);
                frame.setVisible(true);
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(component, BorderLayout.CENTER);
        panel.add(button, BorderLayout.LINE_END);

        return panel;

    }

    protected abstract boolean isCellDate(JTable jTable, int row, int col);

    public Object getCellEditorValue() {
        return this.value;
    }

    private static void checkAccess(int var0) {
        if (System.getSecurityManager() != null && !Modifier.isPublic(var0)) {
            throw new SecurityException("Resource is not accessible");
        }
    }

    private static void checkPackageAccess(Class var0) {
        checkPackageAccess(var0.getName());
    }

    private static void checkPackageAccess(String var0) {
        SecurityManager var1 = System.getSecurityManager();
        if (var1 != null) {
            String var2 = var0.replace('/', '.');
            int var3;
            if (var2.startsWith("[")) {
                var3 = var2.lastIndexOf(91) + 2;
                if (var3 > 1 && var3 < var2.length()) {
                    var2 = var2.substring(var3);
                }
            }

            var3 = var2.lastIndexOf(46);
            if (var3 != -1) {
                var1.checkPackageAccess(var2.substring(0, var3));
            }
        }
    }
}
