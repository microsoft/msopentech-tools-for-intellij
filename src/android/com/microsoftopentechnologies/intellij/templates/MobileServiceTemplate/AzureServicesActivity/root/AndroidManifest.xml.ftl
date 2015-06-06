<#assign parameters = customParameters?eval>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" >
<#if parameters.hasMobileService || parameters.hasNotificationHub>

    <uses-permission android:name="android.permission.INTERNET"/>
</#if>
<#if parameters.hasNotificationHub>
    <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>

    <permission android:name="${packageName}.permission.C2D_MESSAGE" android:protectionLevel="signature"/>
    <uses-permission android:name="${packageName}.permission.C2D_MESSAGE"/>
</#if>

    <application>
        <activity android:name="${relativePackage}.${activityClass}"
            <#if isNewProject>
            android:label="@string/app_name"
            <#else>
            android:label="@string/title_${activityToLayout(activityClass)}"
            </#if>
            >
            <#if isLauncher>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            </#if>
        </activity>
<#if parameters.hasNotificationHub>
        <receiver android:name="com.microsoft.windowsazure.notifications.NotificationsBroadcastReceiver" android:permission="com.google.android.c2dm.permission.SEND">
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <category android:name="${packageName}" />
            </intent-filter>
        </receiver>
</#if>
    </application>
</manifest>