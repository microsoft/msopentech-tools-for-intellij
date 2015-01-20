<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
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
</resources>