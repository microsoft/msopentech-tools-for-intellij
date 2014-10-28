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

package com.microsoftopentechnologies.intellij.helpers;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;

public abstract class InputValidator<T extends JComponent> extends InputVerifier  {
    private Border originalBorder;

    @Override
    public final boolean verify(JComponent jComponent) {

        String error = validate((T) jComponent);

        jComponent.setToolTipText(error == null ? "" : error);
        if(originalBorder == null)
            originalBorder = jComponent.getBorder();

        jComponent.setBorder(error == null ? originalBorder : BorderFactory.createLineBorder(JBColor.RED));

        return (error == null);
    }

    @Override
    public final boolean shouldYieldFocus(JComponent jComponent) {
        super.shouldYieldFocus(jComponent);
        return true;
    }

    public abstract String validate(T component);
}
