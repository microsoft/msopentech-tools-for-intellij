# Get started with MS Open Tech Tools for Android #
This tutorial shows you how to add a cloud-based backend services to an Android app using Azure Mobile Services, Notification Hub and Office365 Services. 
### Prerequisites ##
This tutorial is based on a Microsoft Services plugin that is working with both [IntelliJ IDEA](http://www.jetbrains.com/idea/) and [Android Studio](http://developer.android.com/sdk/installing/studio.html). 
At minimum you’ll need to install the: 

1. Android SDK - [http://developer.android.com/sdk/index.html](http://developer.android.com/sdk/index.html) minimum version is Android 2.3 / API 9. 

1. Java JRE 1.6 and above - [http://www.oracle.com/technetwork/java/javase/downloads/index.html ](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
3.	Azure account. If you don't have an account, you can create a free trial account in just a couple of minutes. For details, see [Azure Free Trial](http://www.windowsazure.com/en-us/pricing/free-trial/?WT.mc_id=AE564AB28).
Once you have the Azure account, download the .publishsettings file 
[https://manage.windowsazure.com/publishsettings](https://manage.windowsazure.com/publishsettings) 

For windows OS you may need to manually install the appropriate Open SSL version to your local machine and add that location in the system environment variable PATH.

For example, if you run on Win64 you’ll need to install Win64 OpenSSL v1.0.1j. to local folder for example C:\OpenSSL-Win64\. After installation is complete open the Windows System environment variables and in the PATH add “C:\OpenSSL-Win64\bin”.
### Install Microsoft Open Tech Plugin  
Both IntelliJ and Android Studio allows to install the plugin by browsing repositories or by installing the plugin from the disk.
 
In this tutorial we show you how to download the binaries from the github repository [https://github.com/MSOpenTech/msopentech-tools-for-intellij](https://github.com/MSOpenTech/msopentech-tools-for-intellij). 

Step 1 Download the binaries from the github repository to your local folder. (Optional if installation is from the repository).

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/DownloadFromGithub.png?token=AFgG-B4AHrvHWLXeTMfKjYexOWRMLWugks5UWDUxwA%3D%3D)
 
Step 2 Install Plugin inside IntelliJ or Android Studio

Developer follows the usual plugin installation steps provided by IntelliJ by going to File > Settings >Plugins

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/Step1-GoToFileSetings.png?token=AFgG-PgUKGS4RU96890etgUAQ6ru_8scks5UWDZIwA%3D%3D)
 
In the Settings window select Plugins and in the Plugin window select Install plugin from Disk. Navigate to local folder and select the zip file corresponding to the IDE.

Note that in IntelliJ you’ll select the “msopentech-tools-for-intellij.zip”

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/IntelliJ_NavigatePluginDisk.png?token=AFgG-AymdnnkT9cj1uKO98LMXdLGIcnlks5UWDaPwA%3D%3D)
 
Note that in Android Studio you’ll select the “msopentech-tools-for-androidstudio.zip”

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_NavigatePluginDisk.png?token=AFgG-G40JYAZvn3Mu65bW8JTfSW_dCrkks5UWDbXwA%3D%3D)
 
Both Android Studio and IntellliJ will require to restart the IDE to finish the plugin installation.  
### Create a new Android App in IntelliJ ##
Follow these steps to create a new mobile service:


- Open IntelliJ
- Select Create new Android project of type Gradle:Android Module. Note that in v0.1 the only supported project is Gradle type.

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/IntelliJ_CreateAndroidProject.png?token=AFgG-PQOmX7-1LnlfUTSkqfnocLWYcg6ks5UWDeMwA%3D%3D)
  

•	Name the project and hit Next 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/IntelliJ_CreateAndroidProject2.png?token=AFgG-PSMPu1W8qlcdiL5_4dJonvLSQ1yks5UWDfSwA%3D%3D)
 

•	Select Blank Activity

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/IntelliJ_CreateAndroidProject3.png?token=AFgG-OL1Sz4PamFlwVR4ItwQ09a4SXYRks5UWDf0wA%3D%3D)
 
In the following dialog leave the default Activity name “Main Activity” and click next 

In the following dialog after validating the name and location of the project click Finish.


### Create a new Android App in Android Studio ##
Create a new project 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_CreateNewProject.png?token=AFgG-NLlx6mA01rpU7LNjqjZY7toeLR-ks5UWDhNwA%3D%3D)
 
![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_CreateNewProject2.png?token=AFgG-CGxIE803F5rEThbjEb_MYZ4PnkYks5UWDh6wA%3D%3D)
 

Select No activity in the following window 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_CreateNewProject_NoActivity.png?token=AFgG-CWzOe5-uPenI9hiIQQNyo0mEO5eks5UWDifwA%3D%3D)

 
Click Finish.

### Connect to an Azure Mobile Service and Notification Hub and generate the helper classes ##

### A.	Create an Azure Services Activity ##
 
Navigate to you project tree and right click on the app folder

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_SelectAzureActivity.png?token=AFgG-IktAFBUEfrROAtyGViHvwGz6JqPks5UWDjywA%3D%3D)
 
In the activity template select the services you are interested in. Based on your selection the corresponding Android SDKs will be downloaded on your disk 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_AzureMobileServiceActivity.png?token=AFgG-DNqBgtuyi2JU6RuxVplF2NYDKRgks5UWDoqwA%3D%3D)

This will prompt you to set up the Azure subscriptions and manage the Azure Mobile service you want to connect to the project without having to go back and forth between the Azure portal and Android Studio. 

### B.	Manage Subscriptions ##

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_ManageSubscriptionsEmpty.png?token=AFgG-AyTNLfHOfPhLYRLfPxOawUG_6v8ks5UWDpbwA%3D%3D)
 
To get the Azure subscription you need to either download the Azure publish settings file by selecting Add Subscription/Import either by selecting the Add Subscription/SignIn

Add Subscription/Import allows you to import the Azure Publish settings file. First you’ll need to download the subscription file indicated in the link and save the file locally. 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/DownloadMicrosoftAzureSubscription.png?token=AFgG-Ooml7c9if0SAl5MXpYDsqEIBInCks5UWDqawA%3D%3D)
 
This will open a browser to the profile info download page in the Azure Portal. The download will proceed automatically, unless the browser is not logged in to the Azure Portal, which will cause to prompt for credentials, and then proceed to the download. The browser will prompt you to save the publish settings file. 

Once the file is downloaded, select it in the dialog by pressing the "Browse" button.

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/ImportMicrosoftAzureSubscription.png?token=AFgG-BWiRVwc0bRl6mpLddVPKd-s4jPgks5UWDrWwA%3D%3D)
 

Once the File is selected press Import button.

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/ImportMicrosoftAzureSubscription2.png?token=AFgG-GhB7XOEb20tORUc0z2BrW9UiZ8Uks5UWDr1wA%3D%3D)
 
This should close the dialog, and return to the manage subscription dialog, which should show the list of subscriptions associated with your Azure Account.

Another way to load the subscription files is to Add Subscription/Sign In.

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/SignInOrgID.png?token=AFgG-M3Gacf9RkK3a-YLKlqIbaOxo4HTks5UWDsewA%3D%3D)
 
In the Manage subscription dialog select the Subscription and click Ok.

### C.	Create an Azure Mobile Service and connect it to your App  ##

If you have created already an Azure Mobile Service with the Azure Portal the Android Studio Microsoft Service wizard will prepopulate that so you can connect an existing project to your app. 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_AddMicrosoftServiceWizard.png?token=AFgG-Li9jnSV-UxFAB4CNLtqvo-qs6-_ks5UWDtwwA%3D%3D)
 
If the service does not exist click on the + button to create one without having to switch to Azure portal. 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_CreateAzureMobileService.png?token=AFgG-J8mtc2LFtG5xgRONuyPi-cuGNwPks5UWDugwA%3D%3D)
 
Once you hit Create a new service is added in the Android Studio Microsoft Service wizard

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_AddMicrosoftServiceWizard2.png?token=AFgG-JpTHm0B3d-jVYpGIByvKL41Fj6rks5UWDvMwA%3D%3D)

By clicking Next if previously you selected a Notification Hub you will be prompted to configure the Notification Hub Service. 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_ConfigureNotificationHub.png?token=AFgG-A3mQtlBYqN8ulApwvfvMfe29I5pks5UWDv4wA%3D%3D)
 
For more details about Notification Hub see [http://azure.microsoft.com/en-us/documentation/articles/notification-hubs-android-get-started/](http://azure.microsoft.com/en-us/documentation/articles/notification-hubs-android-get-started/) 

After selecting Next button you will be prompted with the summary of the changes to your project: 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_Summary.png?token=AFgG-EF8jp9Tdht3hmo0Al6PKCEOHkxtks5UWDxAwA%3D%3D)
 
The following was created to the project: helper classes for Notification Hub and Azure Mobile Services and an Azure Service Activity.  Inside the Azure Service activity you will see a starter code that obtains the mobileServiceClient object for basic operations with the tables and custom APIs and an example of how to configure the NotificationHub. 

# [MISSING IMAGE]  #
 
### Connect to an Azure Mobile Service and Notification Hub and generate the helper classes ##
### A.	Create an Azure Services Activity 
In the project tree right click on the app  

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_SelectOffice365Activity.png?token=AFgG-CFfkJJ8Eh0di_Y8Apbj9iAn4ocMks5UWD2NwA%3D%3D) 

This will generate an Android Studio Activity with the Office365 services:

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_Office365Activity.png?token=AFgG-JbeNQ4PJP3sn11t1puIpM4nOSBfks5UWD68wA%3D%3D)
 
Clicking Finish will allow developer to login and register to the Office365 services 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_Office365Wizard.png?token=AFgG-EOAV7vam4deoNEZlGVFLib4Xkttks5UWD51wA%3D%3D)
 
After selecting the appropriate permissions for the application the developer will see a screen with the summary of all the changes applied to the project.  

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_SummaryOffice365.png?token=AFgG-Gdv5_wR81CSEOW2fucs07Alw68dks5UWD4-wA%3D%3D)

The following SDKs are added to the project in the libraries:

- Outlook Services
-	FileServices
-	SharepointLists

This following java classes will be generated

- An Office365Activity with conecting to…
-	FileServicesClient
-	ListServicesClient 

![](https://raw.githubusercontent.com/MSOpenTech/msopentech-tools-for-intellij/master/docs/media/AS_Office365HelperClass.png?token=AFgG-H-mCCuyFWfHjjlVrp1gmFi__69hks5UWD3DwA%3D%3D)