<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
<#if includeOutlookServices || includeFileServices || includeListServices>
    <string name="o365_app_id_${activityToLayout(activityClass)}">$O365_APP_ID_${activityClass}</string>
    <string name="o365_name_${activityToLayout(activityClass)}">$O365_NAME_${activityClass}</string>
</#if>
<#if includeOutlookServices>
    <string name="os_url_${activityToLayout(activityClass)}">https://outlook.com/ews/odata/</string>
</#if>
<#if includeFileServices>
    <string name="fs_url_${activityToLayout(activityClass)}">https://mytenant.sharepoint.com/_api/v1.0</string>
</#if>
<#if includeListServices>
    <string name="ls_server_url_${activityToLayout(activityClass)}">https://mytenant.sharepoint.com/_api/v1.0</string>
    <string name="ls_site_relative_url_${activityToLayout(activityClass)}">/</string>
</#if>
<#if includeOneNoteServices>
    <string name="o365_clientId_${activityToLayout(activityClass)}">$O365_CLIENTID_${activityClass}</string>
</#if>
</resources>