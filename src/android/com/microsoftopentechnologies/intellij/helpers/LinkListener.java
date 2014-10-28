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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.net.URI;
import java.util.Map;


public class LinkListener implements MouseListener {
    private Font original;
    private String mLink;

    public LinkListener(String link){
        mLink = link;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent)  {
        try {
            Desktop.getDesktop().browse(new URI(mLink));
        } catch (Exception e) {
            UIHelper.showException("Error:", e);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    public void mouseEntered(MouseEvent mouseEvent) {
        mouseEvent.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));

        original = mouseEvent.getComponent().getFont();
        Map attributes = original.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        mouseEvent.getComponent().setFont(original.deriveFont(attributes));
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        mouseEvent.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        mouseEvent.getComponent().setFont(original);
    }
}
