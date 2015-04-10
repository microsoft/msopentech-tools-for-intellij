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
package com.microsoftopentechnologies.intellij.model.storage;

import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

public class TableEntity implements ServiceTreeItem {
    public enum PropertyType {
        Boolean,
        Calendar,
        Double,
        Uuid,
        Integer,
        Long,
        String,
    }

    public static class Property {
        private Object value;
        private PropertyType type;

        public Property(@NotNull Boolean value) {
            this.value = value;
            this.type = PropertyType.Boolean;
        }

        public Property(@NotNull Calendar value) {
            this.value = value;
            this.type = PropertyType.Calendar;
        }

        public Property(@NotNull Double value) {
            this.value = value;
            this.type = PropertyType.Double;
        }

        public Property(@NotNull UUID value) {
            this.value = value;
            this.type = PropertyType.Uuid;
        }

        public Property(@NotNull Integer value) {
            this.value = value;
            this.type = PropertyType.Integer;
        }

        public Property(@NotNull Long value) {
            this.value = value;
            this.type = PropertyType.Long;
        }

        public Property(@NotNull String value) {
            this.value = value;
            this.type = PropertyType.String;
        }

        @NotNull
        public Boolean getValueAsBoolean() throws AzureCmdException {
            if (type.equals(PropertyType.Boolean) && this.value instanceof Boolean) {
                return (Boolean) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Boolean type", "");
            }
        }

        @NotNull
        public Calendar getValueAsCalendar() throws AzureCmdException {
            if (type.equals(PropertyType.Calendar) && this.value instanceof Calendar) {
                return (Calendar) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Calendar type", "");
            }
        }

        @NotNull
        public Double getValueAsDouble() throws AzureCmdException {
            if (type.equals(PropertyType.Double) && this.value instanceof Double) {
                return (Double) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Double type", "");
            }
        }

        @NotNull
        public UUID getValueAsUuid() throws AzureCmdException {
            if (type.equals(PropertyType.Uuid) && this.value instanceof UUID) {
                return (UUID) this.value;
            } else {
                throw new AzureCmdException("Property value is not of UUID type", "");
            }
        }

        @NotNull
        public Integer getValueAsInteger() throws AzureCmdException {
            if (type.equals(PropertyType.Integer) && this.value instanceof Integer) {
                return (Integer) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Integer type", "");
            }
        }

        @NotNull
        public Long getValueAsLong() throws AzureCmdException {
            if (type.equals(PropertyType.Long) && this.value instanceof Long) {
                return (Long) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Long type", "");
            }
        }

        @NotNull
        public String getValueAsString() throws AzureCmdException {
            if (type.equals(PropertyType.String) && this.value instanceof String) {
                return (String) this.value;
            } else {
                throw new AzureCmdException("Property value is not of String type", "");
            }
        }

        @NotNull
        public PropertyType getType() {
            return type;
        }
    }

    private boolean loading;
    private String partitionKey;
    private String rowKey;
    private String tableName;
    private String eTag;
    private Calendar timestamp;
    private Map<String, Property> properties;
    private String subscriptionId;

    public TableEntity(@NotNull String partitionKey,
                       @NotNull String rowKey,
                       @NotNull String tableName,
                       @NotNull String eTag,
                       @NotNull Calendar timestamp,
                       @NotNull Map<String, Property> properties,
                       @NotNull String subscriptionId) {
        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
        this.tableName = tableName;
        this.eTag = eTag;
        this.timestamp = timestamp;
        this.properties = properties;
        this.subscriptionId = subscriptionId;
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
    public String getPartitionKey() {
        return partitionKey;
    }

    @NotNull
    public String getRowKey() {
        return rowKey;
    }

    @NotNull
    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NotNull String tableName) {
        this.tableName = tableName;
    }

    @NotNull
    public String getETag() {
        return eTag;
    }

    public void setETag(@NotNull String eTag) {
        this.eTag = eTag;
    }

    @NotNull
    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NotNull Calendar timestamp) {
        this.timestamp = timestamp;
    }

    @NotNull
    public Map<String, Property> getProperties() {
        return properties;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return partitionKey + " - " + rowKey + (loading ? " (loading...)" : "");
    }
}