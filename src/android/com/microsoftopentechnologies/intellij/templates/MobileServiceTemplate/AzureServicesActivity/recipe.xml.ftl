<?xml version="1.0"?>
<!--
Copyright 2014 Microsoft Open Technologies, Inc.
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-->
<recipe>
<#if includeMobileServices>
	<dependency mavenUrl="com.google.code.gson:gson:2.3" />
	<dependency mavenUrl="com.google.guava:guava:18.0" />
	<dependency mavenUrl="com.microsoft.azure:azure-mobile-services-android-sdk:2.+@aar" />

</#if>
<#if includeNotificationHub>
	<dependency mavenUrl="com.google.android.gms:play-services:3.1.+" />
	<dependency mavenUrl="com.microsoft.azure:azure-notifications-handler:1.0.1@aar" />

</#if>
    <merge from="AndroidManifest.xml.ftl"
            to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <merge from="res/values/strings.xml.ftl"
            to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <instantiate from="src/app_package/AzureServiceActivity.java.ftl"
            to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
</recipe>