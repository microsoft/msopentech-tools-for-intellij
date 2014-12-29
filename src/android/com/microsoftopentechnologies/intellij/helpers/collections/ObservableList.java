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

import com.google.common.collect.ForwardingList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ObservableList<E> extends ForwardingList<E> {
    private final List<E> delegate = new ArrayList<E>();
    protected List<ListChangeListener> changeListeners = new ArrayList<ListChangeListener>();
    private boolean freezeEvents = false;

    @Override
    protected List<E> delegate() {
        return delegate;
    }

    @Override
    public boolean add(E element) {
        return standardAdd(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        freezeEvents = true;
        boolean changed = standardAddAll(c);
        if(changed) {
            freezeEvents = false;
            fireChangeListenerEvent(ListChangedAction.add, c, null);
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        freezeEvents = true;
        boolean changed = standardAddAll(index, c);
        if(changed) {
            freezeEvents = false;
            fireChangeListenerEvent(ListChangedAction.add, c, null);
        }

        return changed;
    }

    @Override
    public void add(int index, E element) {
        super.add(index, element);

        // notify post-add
        fireChangeListenerEvent(
                ListChangedAction.add,
                Arrays.asList(element),
                null);
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if(index > -1)
            remove(index);
        return index != -1;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        freezeEvents = true;
        boolean changed = standardRemoveAll(c);
        if(changed) {
            freezeEvents = false;
            fireChangeListenerEvent(ListChangedAction.remove, null, c);
        }

        return standardRemoveAll(c);
    }

    @Override
    public E remove(int index) {
        E removed = super.remove(index);

        // notify post-remove
        fireChangeListenerEvent(
                ListChangedAction.remove,
                null,
                Arrays.asList(removed));

        return removed;
    }

    private void fireChangeListenerEvent(ListChangedAction action,
                                         Collection<?> newItems,
                                         Collection<?> oldItems) {
        if(!changeListeners.isEmpty() && !freezeEvents) {
            ListChangedEvent listChangedEvent = new ListChangedEvent(
                    this,
                    action,
                    newItems,
                    oldItems);
            for (ListChangeListener listener : changeListeners) {
                listener.listChanged(listChangedEvent);
            }
        }
    }

    public void addChangeListener(ListChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ListChangeListener listener) {
        changeListeners.remove(listener);
    }

    public void removeAllChangeListeners() {
        // we remove items from the collection one by one instead
        // of simply calling "clear" because we want the "remove" event
        // to fire for each item that's removed
        while(!changeListeners.isEmpty()) {
            changeListeners.remove(0);
        }
    }
}
