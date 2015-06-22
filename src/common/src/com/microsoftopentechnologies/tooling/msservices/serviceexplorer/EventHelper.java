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
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer;

import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;

import java.util.concurrent.Semaphore;

public class EventHelper {
    public interface EventStateHandle {
        boolean isEventTriggered();
    }

    public interface EventWaitHandle {
        void waitEvent(@NotNull Runnable callback) throws AzureCmdException;
    }

    public interface EventHandler {
        EventWaitHandle registerEvent()
                throws AzureCmdException;

        void unregisterEvent(@NotNull EventWaitHandle waitHandle)
                throws AzureCmdException;

        void interruptibleAction(@NotNull EventStateHandle eventState)
                throws AzureCmdException;

        void eventTriggeredAction() throws AzureCmdException;
    }

    private static class EventSyncInfo implements EventStateHandle {
        private final Object eventSync = new Object();
        Semaphore semaphore = new Semaphore(0);

        EventWaitHandle eventWaitHandle;
        boolean registeredEvent = false;
        boolean eventTriggered = false;
        AzureCmdException exception;

        public boolean isEventTriggered() {
            synchronized (eventSync) {
                return eventTriggered;
            }
        }
    }

    public static void runInterruptible(@NotNull final EventHandler eventHandler)
            throws AzureCmdException {
        final EventSyncInfo eventSyncInfo = new EventSyncInfo();

        eventSyncInfo.eventWaitHandle = eventHandler.registerEvent();
        eventSyncInfo.registeredEvent = true;

        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventSyncInfo.eventWaitHandle.waitEvent(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (eventSyncInfo.eventSync) {
                                if (eventSyncInfo.registeredEvent) {
                                    eventSyncInfo.registeredEvent = false;
                                    eventSyncInfo.eventTriggered = true;
                                    eventSyncInfo.semaphore.release();
                                }
                            }
                        }
                    });
                } catch (AzureCmdException ignored) {
                }
            }
        });

        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventHandler.interruptibleAction(eventSyncInfo);

                    synchronized (eventSyncInfo.eventSync) {
                        if (eventSyncInfo.registeredEvent) {
                            eventSyncInfo.registeredEvent = false;
                            eventSyncInfo.semaphore.release();
                        }
                    }
                } catch (AzureCmdException ex) {
                    synchronized (eventSyncInfo.eventSync) {
                        if (eventSyncInfo.registeredEvent) {
                            eventSyncInfo.registeredEvent = false;
                            eventSyncInfo.exception = ex;
                            eventSyncInfo.semaphore.release();
                        }
                    }
                }
            }
        });

        try {
            eventSyncInfo.semaphore.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } finally {
            eventHandler.unregisterEvent(eventSyncInfo.eventWaitHandle);
        }

        synchronized (eventSyncInfo.eventSync) {
            if (!eventSyncInfo.eventTriggered) {
                if (eventSyncInfo.exception != null) {
                    throw eventSyncInfo.exception;
                }

                eventHandler.eventTriggeredAction();
            }
        }
    }
}