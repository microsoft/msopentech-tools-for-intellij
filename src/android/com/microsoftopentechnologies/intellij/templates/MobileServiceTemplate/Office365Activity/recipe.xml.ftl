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
<#if includeOutlookServices>
	<dependency mavenUrl="com.microsoft.services:outlook-services:(,1.0)" />
	
</#if>
<#if includeFileServices>
	<dependency mavenUrl="com.microsoft.services:file-services:(,1.0)" />
	
</#if>
<#if includeOutlookServices || includeFileServices>
	<dependency mavenUrl="com.microsoft.services:odata-engine-interfaces:(,1.0)" />
	<dependency mavenUrl="com.microsoft.services:odata-engine-helpers:(,1.0)" />
	<dependency mavenUrl="com.microsoft.services:odata-engine-android-impl:(,1.0)" />
	<dependency mavenUrl="com.microsoft.services:odata-engine-java-impl:(,1.0)" />
	
</#if>
<#if includeListServices>
	<dependency mavenUrl="com.microsoft.services:list-services:(,1.0)" />
	<dependency mavenUrl="com.google.guava:guava:18.0" />
	
</#if>
    <merge from="AndroidManifest.xml.ftl"
            to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <merge from="res/values/strings.xml.ftl"
            to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <instantiate from="src/app_package/Office365Activity.java.ftl"
            to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
</recipe>
