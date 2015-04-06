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
package com.microsoftopentechnologies.intellij.model.vm;

import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;


import java.util.Calendar;

public class VirtualMachineImage implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String type;
    private String category;
    private String publisherName;
    private Calendar publishedDate;
    private String label;
    private String description;
    private String operatingSystemType;
    private String location;
    private String eulaUri;
    private String privacyUri;
    private String pricingUri;
    private String recommendedVMSize;
    private boolean showInGui;

    public VirtualMachineImage(String name, String type, String category,
                               String publisherName, Calendar publishedDate, String label,
                               String description, String operatingSystemType, String location,
                               String eulaUri, String privacyUri, String pricingUri,
                               String recommendedVMSize, boolean showInGui) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.publisherName = publisherName;
        this.publishedDate = publishedDate;
        this.label = label;
        this.description = description;
        this.operatingSystemType = operatingSystemType;
        this.location = location;
        this.eulaUri = eulaUri;
        this.privacyUri = privacyUri;
        this.pricingUri = pricingUri;
        this.recommendedVMSize = recommendedVMSize;
        this.showInGui = showInGui;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }


    public String getName() {
        return name;
    }


    public String getType() {
        return type;
    }


    public String getCategory() {
        return category;
    }


    public String getPublisherName() {
        return publisherName;
    }


    public Calendar getPublishedDate() {
        return publishedDate;
    }


    public String getLabel() {
        return label;
    }


    public String getDescription() {
        return description;
    }


    public String getOperatingSystemType() {
        return operatingSystemType;
    }


    public String getLocation() {
        return location;
    }


    public String getEulaUri() {
        return eulaUri;
    }


    public String getPrivacyUri() {
        return privacyUri;
    }


    public String getPricingUri() {
        return pricingUri;
    }


    public String getRecommendedVMSize() {
        return recommendedVMSize;
    }

    public boolean isShowInGui() {
        return showInGui;
    }

    @Override
    public String toString() {
        return label + (loading ? " (loading...)" : "");
    }
}