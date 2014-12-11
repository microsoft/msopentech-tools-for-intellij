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

package com.microsoftopentechnologies.intellij.helpers.collections;

import java.util.Collection;
import java.util.EventObject;

public class ListChangedEvent extends EventObject {
    private ListChangedAction action;
    private Collection<?> newItems;
    private Collection<?> oldItems;

    public ListChangedEvent(
            ObservableList<?> source,
            ListChangedAction action,
            Collection<?> newItems,
            Collection<?> oldItems) {
        super(source);
        this.action = action;
        this.newItems = newItems;
        this.oldItems = oldItems;
    }

    public ListChangedAction getAction() {
        return action;
    }

    public Collection<?> getNewItems() {
        return newItems;
    }

    public Collection<?> getOldItems() {
        return oldItems;
    }
}
