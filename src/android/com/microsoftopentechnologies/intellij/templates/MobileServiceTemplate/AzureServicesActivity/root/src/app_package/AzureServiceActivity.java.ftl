package ${packageName};
<#assign parameters = customParameters?eval>

import android.app.Activity;
import android.app.AlertDialog;
<#if parameters.hasMobileService || parameters.hasNotificationHub>
import android.content.Context;
</#if>
import android.os.Bundle;
<#if parameters.hasMobileService>
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
</#if>
<#if parameters.hasNotificationHub>
import com.microsoft.windowsazure.messaging.NotificationHub;
</#if>
<#if parameters.hasMobileService>
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceJsonTable;
</#if>
<#if parameters.hasNotificationHub>
import com.microsoft.windowsazure.notifications.NotificationsHandler;
import com.microsoft.windowsazure.notifications.NotificationsManager;
</#if>
<#if parameters.hasMobileService>

import java.net.MalformedURLException;
</#if>

public class ${activityClass} extends Activity {
<#if parameters.hasNotificationHub>
    public static class NotificationHubsHelper extends NotificationsHandler {
        @Override
        public void onRegistered(Context context, String gcmRegistrationId) {
            try {
                String hubName = context.getString(R.string.nh_name_${activityToLayout(activityClass)});
                String connStr = context.getString(R.string.nh_conn_str_${activityToLayout(activityClass)});

                NotificationHub hub = new NotificationHub(hubName, connStr, context);
                hub.register(gcmRegistrationId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnregistered(Context context, String gcmRegistrationId) {
            try {
                String hubName = context.getString(R.string.nh_name_${activityToLayout(activityClass)});
                String connStr = context.getString(R.string.nh_conn_str_${activityToLayout(activityClass)});

                NotificationHub hub = new NotificationHub(hubName, connStr, context);
                hub.unregister();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReceive(Context context, Bundle bundle) {
            super.onReceive(context, bundle);
            //YOU CAN OVERRIDE THE DEFAULT IMPLEMENTATION HERE
        }
    }

</#if>
    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<#if parameters.hasMobileService>

        try {
            //Obtain the MobileServiceClient object to query your mobile service
            MobileServiceClient mobileServiceClient = getMobileServiceClient(this);

            //Query or update your table's data using MobileServiceJsonTable
            MobileServiceJsonTable table = mobileServiceClient.getTable("TABLE_NAME");
            ListenableFuture<JsonElement> tableFuture = table.execute();

            Futures.addCallback(tableFuture, new FutureCallback<JsonElement>() {
                @Override
                public void onSuccess(JsonElement element) {
                    // handle success

                }

                @Override
                public void onFailure(Throwable throwable) {
                    // handle failure

                }
            });

            //Run your custom APIs
            ListenableFuture<JsonElement> apiFuture = mobileServiceClient.invokeApi("API_NAME");

            Futures.addCallback(apiFuture, new FutureCallback<JsonElement>() {
                @Override
                public void onSuccess(JsonElement element) {
                    // handle success

                }

                @Override
                public void onFailure(Throwable throwable) {
                    // handle failure

                }
            });
        } catch (MalformedURLException e) {
            createAndShowDialog(e, "Error trying to get mobile service. Invalid URL");
        } catch (MobileServiceException e) {
            createAndShowDialog(e, "Error trying to query mobile service table. Invalid URL");
        }
</#if>
<#if parameters.hasNotificationHub>

		handleNotifications(this);
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
<#if parameters.hasMobileService>

    /**
     * Creates a new MobileServiceClient instance with preconfigured credentials
     *
     * @param context
     *            The context being associated to the MobileServiceClient
     * @return
     *            The Mobile Service client
     */
	private static MobileServiceClient getMobileServiceClient(Context context) throws MalformedURLException {
		String appUrl = context.getString(R.string.ms_url_${activityToLayout(activityClass)});
        String appKey = context.getString(R.string.ms_key_${activityToLayout(activityClass)});

        return new MobileServiceClient(appUrl, appKey, context);
	}
</#if>
<#if parameters.hasNotificationHub>

    /**
     * Enable notifications handling using the NotificationHubsHelper class
     *
     * @param context
     *            The context being used to handle notifications
     */
	private static void handleNotifications(Context context) {
        String gcmAppId = context.getString(R.string.nh_sender_id_${activityToLayout(activityClass)});
        NotificationsManager.handleNotifications(context, gcmAppId, NotificationHubsHelper.class);
	}
</#if>
}