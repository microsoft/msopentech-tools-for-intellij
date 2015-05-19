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

package com.microsoftopentechnologies.tooling.msservices.helpers.azure.rest.model;

public class TableData {
    private String idType;
    private String hasDeleted;
    private Metric metrics;
    private String name;
    private String selflink;

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getHasDeleted() {
        return hasDeleted;
    }

    public void setHasDeleted(String hasDeleted) {
        this.hasDeleted = hasDeleted;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSelflink() {
        return selflink;
    }

    public void setSelflink(String selflink) {
        this.selflink = selflink;
    }

    public Metric getMetrics() {
        return metrics;
    }

    public void setMetrics(Metric metrics) {
        this.metrics = metrics;
    }

    public class Metric {
        private int indexCount;
        private int recordCount;
        private long sizeBytes;

        public int getIndexCount() {
            return indexCount;
        }

        public void setIndexCount(int indexCount) {
            this.indexCount = indexCount;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }
    }
}
