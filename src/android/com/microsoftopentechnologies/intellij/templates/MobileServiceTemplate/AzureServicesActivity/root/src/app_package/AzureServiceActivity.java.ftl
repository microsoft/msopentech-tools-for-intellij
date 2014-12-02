<#if includeMobileServices && !includeNotificationHub>//010fa0c4-5af1-4f81-95c1-720d9fab8d96</#if><#if !includeMobileServices && includeNotificationHub>//46cca6b7-ff7d-4e05-9ef2-d7eb4798222e</#if><#if includeMobileServices && includeNotificationHub>//657555dc-6167-466a-9536-071307770d46</#if>
package ${packageName};

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

<#if includeMobileServices>
import com.google.gson.JsonElement;
import com.microsoftopentechnologies.intellij.MobileService;
import com.microsoft.windowsazure.mobileservices.ApiJsonOperationCallback;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceJsonTable;
import com.microsoft.windowsazure.mobileservices.MobileServiceQuery;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.TableJsonQueryCallback;
</#if>

<#if includeNotificationHub>
import com.microsoftopentechnologies.intellij.NotificationHubsHelper;
import com.microsoft.windowsazure.messaging.NotificationHub;
</#if>

import java.net.MalformedURLException;

public class ${activityClass} extends Activity {

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        <#if includeMobileServices>
        try {

            //Obtain the MobileServiceClient object to query your mobile service
            MobileServiceClient mobileServiceClient = MobileService.getInstance(this);

            //Query or update your table's data using MobileServiceJsonTable
            MobileServiceJsonTable table_name = mobileServiceClient.getTable("TABLE_NAME");
            table_name.execute(new MobileServiceQuery<Object>(), new TableJsonQueryCallback() {
                @Override
                public void onCompleted(JsonElement jsonElement, int i, Exception e, ServiceFilterResponse serviceFilterResponse) {

                }
            });

            //Run your custom APIs
            mobileServiceClient.invokeApi("API_NAME", new ApiJsonOperationCallback() {
                @Override
                public void onCompleted(JsonElement jsonElement, Exception e, ServiceFilterResponse serviceFilterResponse) {

                }
            });
        } catch (MalformedURLException e) {
            createAndShowDialog(e, "Error trying to get mobile service. Invalid URL");
        }
        </#if>

        <#if includeNotificationHub>
        try {
            //Use the NotificationHub object to register to GCM by getting the registration ID and using the register method
            NotificationHub notificationHub = NotificationHubsHelper.getNotificationHub(this);
            notificationHub.register("GCM_REGISTRATION_ID");
        } catch (Exception e) {
            createAndShowDialog(e, "Error registering notification hub");
        }
        </#if>
    }

    /**
     * Creates a dialog and shows it
     * 
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;

        if(exception.getCause() != null){
            ex = exception.getCause();
        }

        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     * 
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }
}