<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
<#if includeMobileServices>
    <string name="ms_url_${activityToLayout(activityClass)}">$APPURL_${activityClass}</string>
    <string name="ms_key_${activityToLayout(activityClass)}">$APPKEY_${activityClass}</string>
</#if>
<#if includeNotificationHub>
    <string name="nh_sender_id_${activityToLayout(activityClass)}">$SENDERID_${activityClass}</string>
    <string name="nh_conn_str_${activityToLayout(activityClass)}">$CONNSTR_${activityClass}</string>
    <string name="nh_name_${activityToLayout(activityClass)}">$HUBNAME_${activityClass}</string>
</#if>
</resources>