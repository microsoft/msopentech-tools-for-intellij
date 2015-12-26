MS Open Tech Tools:  IntelliJ and Android Studio Plugin for Microsoft Services (DEPRECATED)
================================================================================

> :warning: **NOTE** This project has been deprecated in favor of the newer plugin versions released by Microsoft, which are equivalent in functionality, but split across 3 separate plugins:
* [Azure Services Explorer](https://plugins.jetbrains.com/plugin/8052) is a required pre-requisite for the other two plugins
* [Azure Toolkit for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/8053) is for Java developers on Azure
* [Microsoft Cloud Services for Android](https://plugins.jetbrains.com/plugin/8077) is for Android developers

> If you are a user of this older combined plugin, it is recommended to uninstall it, then install the [Azure Services Explorer](https://plugins.jetbrains.com/plugin/8052) plugin, and then one or two of the other plugins, depending on your needs. 

> Issues logged in this project will no longer be addresseed.

*Original readme:*

Plugin for easy and fast development to enable developers of Android Apps to connect to Office 365 services and Azure Mobile Services, and developers of Java middleware to connect to Azure compute services. 

For Android app developers, this plugin provides a highly productive integrated development environment within IntelliJ IDEA and Android Studio, which they can use to integrate Android apps with Office 365 and Azure Mobile Services. For middleware Java developers, the plugin provides an integrated environment to access Microsoft Azure compute services. (Access to Office 365 data and services is coming soon.) 

At minimum you’ll need to install the following: 

Prerequisites
=============
* Install the IntelliJ IDEA or Android Studio.
* Android SDK - [http://developer.android.com/sdk/index.html](http://developer.android.com/sdk/index.html) minimum version is Android 2.3 / API 9. 
* Install the Java JDK (For changing the plugin you need JDK 1.6. For creating android apps using the latest from http://www.oracle.com/technetwork/java/javase/downloads/index.html is sufficient
* For windows OS you may need to manually install the appropriate Open SSL version to your local machine (for example C:\OpenSSL-Win64\) and add that location in the system environment variable PATH(add “C:\OpenSSL-Win64\bin”). 

Azure Notes
===========
* You will need an Azure account. If you don't have an account, you can create a free trial account in just a couple of minutes. For details, see [Azure Free Trial](http://www.windowsazure.com/en-us/pricing/free-trial/?WT.mc_id=AE564AB28).

* When connecting an android project to a mobile service, the plugin will download the Windows Azure Mobile Services Client SDK for Android from http://www.windowsazure.com/en-us/develop/mobile/android/ that is available on GitHub under the Apache 2.0 license: https://github.com/WindowsAzure/azure-mobile-services/tree/master/sdk/android

**Other Dependencies:**
1. ProjectManagerLibrary.jar from Utils/ProjectManagerLibrary/jar
2. AzureCommons.jar from Utils/AzureCommons/jar - might need to build
3. .cspack.jar from StarterKit/CSPackAntTask/jar
4. azure-core-0.7.0.jar from PluginsAndFeatures\AddLibrary\AzureLibraries\com.microsoftopentechnologies.windowsazure.tools.sdk
and all jars from 'dependencies' folder

Also, need to add antIntegration.jar from <IDEA_INSTALLATION>/plugins/ant/lib to SDK classpath.

