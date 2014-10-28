"MS Open Tech Tools:  IntelliJ and Android Studio Plugin for Microsoft Services"
================================================================================

Plugin for easy and fast development of apps connecting to and integrating into Microsoft Services. Current services available are: Windows Azure Mobile Services (storage, authentication, custom scripts...),  Push Notification and Office365 Services.

The Android tools offered in the plugin work with both [IntelliJ IDEA](http://www.jetbrains.com/idea/) and [Android Studio](http://developer.android.com/sdk/installing/studio.html). 
At minimum you’ll need to install the following: 

Prerequisites
=============
* Install the IntelliJ IDEA or Android Studio.
* Android SDK - [http://developer.android.com/sdk/index.html](http://developer.android.com/sdk/index.html) minimum version is Android 2.3 / API 9. 
* Java JRE 1.6 and above - [http://www.oracle.com/technetwork/java/javase/downloads/index.html ](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Install the Java JDK (For changing the plugin you need JDK 1.6. For creating android apps using the latest 1.7 from http://www.oracle.com/technetwork/java/javase/downloads/index.html is sufficient)
* For windows OS you may need to manually install the appropriate Open SSL version to your local machine (for example C:\OpenSSL-Win64\) and add that location in the system environment variable PATH(add “C:\OpenSSL-Win64\bin”). 

Azure Notes
===========
* You will need an Azure account. If you don't have an account, you can create a free trial account in just a couple of minutes. For details, see [Azure Free Trial](http://www.windowsazure.com/en-us/pricing/free-trial/?WT.mc_id=AE564AB28).

* When connecting an android project to a mobile service, the plugin will download the Windows Azure Mobile Services Client SDK for Android from http://www.windowsazure.com/en-us/develop/mobile/android/ that is available on GitHub under the Apache 2.0 license: https://github.com/WindowsAzure/azure-mobile-services/tree/master/sdk/android

**Other Dependencies:**
1. ProjectManagerLibrary.jar from Utils/ProjectManagerLibrary/jar
2. AzureCommons.jar from Utils/AzureCommons/jar - might need to build
3. AzureManagementUtil.jar from Utils/AzureManagementUtil.jar - might need to build
4. azure-core-0.6.0.jar from PluginsAndFeatures\AddLibrary\AzureLibraries\com.microsoftopentechnologies.windowsazure.tools.sdk
and all jars from 'dependencies' folder

Also, need to add antIntegration.jar from <IDEA_INSTALLATION>/plugins/ant/lib to SDK classpath.
