<#assign parameters = customParameters?eval><resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
<#if parameters.hasMobileService>
     <string name="ms_url_${activityToLayout(activityClass)}">${parameters.appUrl}</string>
     <string name="ms_key_${activityToLayout(activityClass)}">${parameters.appKey}</string>
</#if>
<#if parameters.hasNotificationHub>
    <string name="nh_sender_id_${activityToLayout(activityClass)}">${parameters.sender}</string>
    <string name="nh_conn_str_${activityToLayout(activityClass)}">${parameters.connStr?xml}</string>
    <string name="nh_name_${activityToLayout(activityClass)}">${parameters.hub}</string>
</#if>
</resources>