/**
 * Copyright 2014 Microsoft Open Technologies Inc.
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
package com.microsoftopentechnologies.intellij.ui;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoftopentechnologies.intellij.ui.components.AzureWizardStep;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.microsoftopentechnologies.intellij.ui.messages.AzureBundle.message;

public class KeyFeaturesStep extends AzureWizardStep {
    private JPanel rootPanel;
    private JCheckBox sessionAffinityCheckBox;
    private JCheckBox cachingCheckBox;
    private JCheckBox debuggingCheckBox;
    private JXHyperlink ssnAffLnk;
    private JXHyperlink cachLnk;
    private JXHyperlink debugLnk;

    public KeyFeaturesStep(final String title) {
        super(title, message("keyFtrPgMsg"));
        init();
    }

    public void init() {
        initLink(ssnAffLnk, message("ssnAffLnk"));
        initLink(cachLnk, message("cachLnk"));
        initLink(debugLnk, message("debugLnk"));
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        rootPanel.revalidate();
        return rootPanel;
    }

    private void initLink(JXHyperlink link, String linkText) {
        link.setURI(URI.create(linkText));
        link.setText(message("lblLearnMore"));
    }

    public Map<String, Boolean> getValues() {
        Map <String, Boolean> values = new HashMap<String, Boolean>();
        values.put("ssnAffChecked", sessionAffinityCheckBox.isSelected());
        values.put("cacheChecked", cachingCheckBox.isSelected());
        values.put("debugChecked", debuggingCheckBox.isSelected());
        return values;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }
}
