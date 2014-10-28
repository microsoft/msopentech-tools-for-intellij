Microsoft Services Tools Plugin for Android
===========================================

Plugin for easy and fast development of apps connecting to and integrating into Microsoft Services. Current services available are: Windows Azure Mobile Services (storage, authentication, custom scripts...) and Push Notification. 

**Prerequisites** 
* Install the IntelliJ IDEA
* Install the Android SDK - http://developer.android.com/sdk/index.html#download (The minimum required for the Android SDK is 1.5)
* Install the Java JDK (For changing the plugin you need JDK 1.6. For creating android apps using the latest 1.7 from http://www.oracle.com/technetwork/java/javase/downloads/index.html is sufficient)
* Install the Windows Azure command Line Interface from: http://www.windowsazure.com/en-us/downloads/

**Note**
The plugin will download the following on your computer, when connecting an android project to a mobile service (only if it's not has downloaded it earlier):
* The Windows Azure Mobile Services Client SDK for Android from http://www.windowsazure.com/en-us/develop/mobile/android/ that is available on GitHub under the Apache 2.0 license: https://github.com/WindowsAzure/azure-mobile-services/tree/master/sdk/android

Azure Notes
===========

Dependencies:
1. ProjectManagerLibrary.jar from Utils/ProjectManagerLibrary/jar
2. AzureCommons.jar from Utils/AzureCommons/jar - might need to build
3. AzureManagementUtil.jar from Utils/AzureManagementUtil.jar - might need to build
4. azure-core-0.6.0.jar from PluginsAndFeatures\AddLibrary\AzureLibraries\com.microsoftopentechnologies.windowsazure.tools.sdk
and all jars from 'dependencies' folder

Also, need to add antIntegration.jar from <IDEA_INSTALLATION>/plugins/ant/lib to SDK classpath.