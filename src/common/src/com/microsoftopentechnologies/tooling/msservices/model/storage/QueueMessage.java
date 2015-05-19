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
package com.microsoftopentechnologies.tooling.msservices.model.storage;

import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.model.ServiceTreeItem;

import java.util.Calendar;

public class QueueMessage implements ServiceTreeItem {
    private boolean loading;
    private String id;
    private String queueName;
    private String content;
    private Calendar insertionTime;
    private Calendar expirationTime;
    private int dequeueCount;

    public QueueMessage(@NotNull String id,
                        @NotNull String queueName,
                        @NotNull String content,
                        @NotNull Calendar insertionTime,
                        @NotNull Calendar expirationTime,
                        int dequeueCount) {
        this.id = id;
        this.queueName = queueName;
        this.content = content;
        this.insertionTime = insertionTime;
        this.expirationTime = expirationTime;
        this.dequeueCount = dequeueCount;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(@NotNull String queueName) {
        this.queueName = queueName;
    }

    @NotNull
    public String getContent() {
        return content;
    }

    public void setContent(@NotNull String content) {
        this.content = content;
    }

    @NotNull
    public Calendar getInsertionTime() {
        return insertionTime;
    }

    public void setInsertionTime(@NotNull Calendar insertionTime) {
        this.insertionTime = insertionTime;
    }

    @NotNull
    public Calendar getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(@NotNull Calendar expirationTime) {
        this.expirationTime = expirationTime;
    }

    public int getDequeueCount() {
        return dequeueCount;
    }

    public void setDequeueCount(int dequeueCount) {
        this.dequeueCount = dequeueCount;
    }

    @Override
    public String toString() {
        return id + (loading ? " (loading...)" : "");
    }
}