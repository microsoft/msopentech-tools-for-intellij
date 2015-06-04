<#assign parameters = customParameters?eval>package ${packageName};

import android.app.Activity;
<#if parameters.isOutlookServices || parameters.isFileServices || parameters.isSharepointLists>
import android.content.Context;
</#if>
import android.os.Bundle;
<#if parameters.isOneNote>
import android.util.Log;
</#if>
<#if parameters.isOutlookServices || parameters.isFileServices || parameters.isSharepointLists || parameters.isOneNote>
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
</#if>
<#if parameters.isOutlookServices || parameters.isFileServices || parameters.isSharepointLists>
import com.google.common.util.concurrent.ListenableFuture;
</#if>
<#if parameters.isFileServices>
import com.microsoft.fileservices.Item;
import com.microsoft.fileservices.odata.SharePointClient;
</#if>
<#if parameters.isOneNote>
import com.microsoft.live.LiveAuthClient;
import com.microsoft.onenote.api.Notebook;
import com.microsoft.onenote.api.odata.OneNoteApiClient;
</#if>
<#if parameters.isOutlookServices>
import com.microsoft.outlookservices.Message;
import com.microsoft.outlookservices.odata.OutlookClient;
</#if>
<#if parameters.isOutlookServices || parameters.isFileServices>
import com.microsoft.services.odata.impl.DefaultDependencyResolver;
</#if>
<#if parameters.isOneNote>
import com.microsoft.services.odata.impl.LiveAuthDependencyResolver;
import com.microsoft.services.odata.interfaces.LogLevel;
</#if>
<#if parameters.isSharepointLists>
import com.microsoft.sharepointservices.Credentials;
import com.microsoft.sharepointservices.ListClient;
import com.microsoft.sharepointservices.Query;
import com.microsoft.sharepointservices.SPList;
import com.microsoft.sharepointservices.http.Request;
</#if>

<#if parameters.isOneNote>
import java.util.Arrays;
</#if>
<#if parameters.isOutlookServices || parameters.isFileServices || parameters.isSharepointLists || parameters.isOneNote>
import java.util.List;
</#if>

public class ${activityClass} extends Activity {
<#if parameters.isOutlookServices || parameters.isFileServices || parameters.isSharepointLists>
    private final String o365AppId = this.getString(R.string.o365_app_id_${activityToLayout(activityClass)});
    private final String o365Name = this.getString(R.string.o365_name_${activityToLayout(activityClass)});
</#if>
<#if parameters.isOneNote>
    private final String CLIENT_ID = this.getString(R.string.o365_clientId_${activityToLayout(activityClass)});
    final static public String[] SCOPES = {
            "wl.signin",
            "wl.basic",
            "wl.offline_access",
            "wl.skydrive_update",
            "wl.contacts_create",
            "office.onenote_create"
    };
    final static public String ONENOTE_API_ROOT = "https://www.onenote.com/api/v1.0";

    private LiveAuthDependencyResolver dependencyResolver;
    private List<Notebook> notebookList;
    private OneNoteApiClient oneNoteClient;
</#if>

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<#if parameters.isOutlookServices>

        OutlookClient outlookClient = getOutlookClient(this);
        ListenableFuture<List<Message>> inboxFuture = getInboxMessages(outlookClient);

        Futures.addCallback(inboxFuture, new FutureCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                // handle success

            }

            @Override
            public void onFailure(Throwable throwable) {
                // handle failure

            }
        });
</#if>
<#if parameters.isFileServices>

        SharePointClient sharePointClient = getSharePointClient(this);
        ListenableFuture<List<Item>> filesFuture = getMyFiles(sharePointClient);

        Futures.addCallback(filesFuture, new FutureCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> messages) {
                // handle success

            }

            @Override
            public void onFailure(Throwable throwable) {
                // handle failure

            }
        });
</#if>
<#if parameters.isSharepointLists>

        ListClient listClient = getListClient(this);
        ListenableFuture<List<SPList>> listsFuture = getMyLists(listClient);

        Futures.addCallback(listsFuture, new FutureCallback<List<SPList>>() {
            @Override
            public void onSuccess(List<SPList> messages) {
                // handle success

            }

            @Override
            public void onFailure(Throwable throwable) {
                // handle failure

            }
        });
</#if>

<#if parameters.isOneNote>
        try {
            Futures.addCallback(this.getDependencyResolver().interactiveInitialize(this), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Futures.addCallback(getOneNoteClient().getnotebooks().read(), new FutureCallback<List<Notebook>>() {
                        @Override
                        public void onSuccess(List<Notebook> notebooks) {
                            notebookList = notebooks;


                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.e("OneNoteSampleActivity", t.getMessage());
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    getDependencyResolver().getLogger().log(t.getMessage(), LogLevel.ERROR);
                }
            });
        } catch (Exception e) {
            Log.e("OneNoteSampleActivity", e.getMessage());
        }
</#if>
	}
<#if parameters.isOutlookServices>

    private static OutlookClient getOutlookClient(Context context) {
        // TODO: Implement this invoking AAD or another credentials provider
        String token = "someAADtoken";
        DefaultDependencyResolver resolver = new DefaultDependencyResolver(token);
		String url = context.getString(R.string.os_url_${activityToLayout(activityClass)});

        return new OutlookClient(url, resolver);
    }

    private static ListenableFuture<List<Message>> getInboxMessages(OutlookClient client) {
        return client.getMe().getFolders().getById("Inbox").getMessages().read();
    }
</#if>
<#if parameters.isFileServices>

   private static SharePointClient getSharePointClient(Context context) {
        // TODO: Implement this invoking AAD or another credentials provider
        String token = "someAADtoken";
        DefaultDependencyResolver resolver = new DefaultDependencyResolver(token);
		String url = context.getString(R.string.fs_url_${activityToLayout(activityClass)});

        return new SharePointClient(url, resolver);
    }

    private static ListenableFuture<List<Item>> getMyFiles(SharePointClient client) {
        return client.getfiles().read();
    }
</#if>
<#if parameters.isSharepointLists>

    private static ListClient getListClient(Context context) {
        Credentials credentials = new Credentials() {
            @Override
            public void prepareRequest(Request request) {
                // TODO: Implement this invoking AAD or another credentials provider
            }
        };

        String serverUrl = context.getString(R.string.ls_server_url_${activityToLayout(activityClass)});
        String siteRelativeUrl = context.getString(R.string.ls_site_relative_url_${activityToLayout(activityClass)});

        return new ListClient(serverUrl, siteRelativeUrl, credentials);
    }

    private static ListenableFuture<List<SPList>> getMyLists(ListClient client) {
        return client.getLists(new Query());
    }
</#if>
<#if parameters.isOneNote>
    protected OneNoteApiClient getOneNoteClient() {
        if (oneNoteClient == null) {
            oneNoteClient = new OneNoteApiClient(ONENOTE_API_ROOT, getDependencyResolver());
        }
        return oneNoteClient;
    }

    protected LiveAuthDependencyResolver getDependencyResolver() {

        if (dependencyResolver == null) {
            LiveAuthClient theAuthClient = new LiveAuthClient(getApplicationContext(), CLIENT_ID,
                    Arrays.asList(SCOPES));

            dependencyResolver = new LiveAuthDependencyResolver(theAuthClient);

            dependencyResolver.getLogger().setEnabled(true);
            dependencyResolver.getLogger().setLogLevel(LogLevel.VERBOSE);
        }

        return dependencyResolver;
    }
</#if>
}