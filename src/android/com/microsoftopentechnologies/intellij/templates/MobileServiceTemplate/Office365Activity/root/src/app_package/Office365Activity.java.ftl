<#if includeOutlookServices && !includeFileServices && !includeListServices>//fa684d69-70b3-41ec-83ff-2f8fa77aeeba</#if><#if !includeOutlookServices && includeFileServices && !includeListServices>//1073bed4-78c3-4b4a-8a4d-ad874a286d86</#if><#if !includeOutlookServices && !includeFileServices && includeListServices>//6695fd94-10cc-4274-b5df-46a3bc63a33d</#if><#if includeOutlookServices && includeFileServices && !includeListServices>//c4c2fd13-4abf-4785-a410-1887c5a1f1fc</#if><#if includeOutlookServices && !includeFileServices && includeListServices>//322e22fa-c249-4805-b057-c7b282acb605</#if><#if !includeOutlookServices && includeFileServices && includeListServices>//7193e8e2-dcec-4eb9-a3d6-02d86f88eaed</#if><#if includeOutlookServices && includeFileServices && includeListServices>//25fdea0c-8a15-457f-9b15-dacb4e7dc2b2</#if>
package ${packageName};

import android.app.Activity;
<#if includeOutlookServices || includeFileServices || includeListServices>
import android.content.Context;
</#if>
import android.os.Bundle;
<#if includeOutlookServices || includeFileServices || includeListServices>
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
</#if>
<#if includeFileServices>
import com.microsoft.fileservices.Item;
</#if>
<#if includeListServices>
import com.microsoft.listservices.Credentials;
import com.microsoft.listservices.Query;
import com.microsoft.listservices.SPList;
import com.microsoft.listservices.SharepointListsClient;
import com.microsoft.listservices.http.Request;
</#if>
<#if includeOutlookServices>
import com.microsoft.outlookservices.Message;
import com.microsoft.outlookservices.odata.OutlookClient;
</#if>
<#if includeOutlookServices || includeFileServices>
import com.microsoft.services.odata.impl.DefaultDependencyResolver;
</#if>
<#if includeFileServices>
import com.microsoft.sharepointservices.odata.SharePointClient;
</#if>
<#if includeOutlookServices || includeFileServices || includeListServices>

import java.util.List;
</#if>

public class ${activityClass} extends Activity {
    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<#if includeOutlookServices>

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
<#if includeFileServices>

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
<#if includeListServices>

        SharepointListsClient sharepointListsClient = getSharepointListsClient(this);
        ListenableFuture<List<SPList>> listsFuture = getMyLists(sharepointListsClient);

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
	}
<#if includeOutlookServices>

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
<#if includeFileServices>

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
<#if includeListServices>

    private static SharepointListsClient getSharepointListsClient(Context context) {
        Credentials credentials = new Credentials() {
            @Override
            public void prepareRequest(Request request) {
                // TODO: Implement this invoking AAD or another credentials provider
            }
        };

        String serverUrl = context.getString(R.string.ls_server_url_${activityToLayout(activityClass)});
        String siteRelativeUrl = context.getString(R.string.ls_site_relative_url_${activityToLayout(activityClass)});

        return new SharepointListsClient(serverUrl, siteRelativeUrl, credentials);
    }

    private static ListenableFuture<List<SPList>> getMyLists(SharepointListsClient client) {
        return client.getLists(new Query());
    }
</#if>
}