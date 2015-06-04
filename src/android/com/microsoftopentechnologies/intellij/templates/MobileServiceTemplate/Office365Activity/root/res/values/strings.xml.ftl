<#assign parameters = customParameters?eval><resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
<#if parameters.isOutlookServices || parameters.isFileServices || parameters.isSharepointLists>
    <string name="o365_app_id_${activityToLayout(activityClass)}">${parameters.appId}</string>
    <string name="o365_name_${activityToLayout(activityClass)}">${parameters.appName}</string>
</#if>
<#if parameters.isOutlookServices>
    <string name="os_url_${activityToLayout(activityClass)}">https://outlook.com/ews/odata/</string>
</#if>
<#if parameters.isFileServices>
    <string name="fs_url_${activityToLayout(activityClass)}">https://mytenant.sharepoint.com/_api/v1.0</string>
</#if>
<#if parameters.isSharepointLists>
    <string name="ls_server_url_${activityToLayout(activityClass)}">https://mytenant.sharepoint.com/_api/v1.0</string>
    <string name="ls_site_relative_url_${activityToLayout(activityClass)}">/</string>
</#if>
<#if parameters.isOneNote>
    <string name="o365_clientId_${activityToLayout(activityClass)}">${parameters.clientId}</string>
</#if>
</resources>