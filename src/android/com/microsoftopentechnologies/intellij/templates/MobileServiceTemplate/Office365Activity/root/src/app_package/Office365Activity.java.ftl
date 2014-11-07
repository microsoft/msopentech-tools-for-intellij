<#if includeOutlookServices && !includeFileServices && !includeListServices>//fa684d69-70b3-41ec-83ff-2f8fa77aeeba</#if><#if !includeOutlookServices && includeFileServices && !includeListServices>//1073bed4-78c3-4b4a-8a4d-ad874a286d86</#if><#if !includeOutlookServices && !includeFileServices && includeListServices>//6695fd94-10cc-4274-b5df-46a3bc63a33d</#if><#if includeOutlookServices && includeFileServices && !includeListServices>//c4c2fd13-4abf-4785-a410-1887c5a1f1fc</#if><#if includeOutlookServices && !includeFileServices && includeListServices>//322e22fa-c249-4805-b057-c7b282acb605</#if><#if !includeOutlookServices && includeFileServices && includeListServices>//7193e8e2-dcec-4eb9-a3d6-02d86f88eaed</#if><#if includeOutlookServices && includeFileServices && includeListServices>//25fdea0c-8a15-457f-9b15-dacb4e7dc2b2</#if>
package ${packageName};

import android.app.Activity;
import android.os.Bundle;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

<#if includeOutlookServices>
// Outlook Services imports
import com.microsoftopentechnologies.intellij.OutlookServicesClient;
import com.microsoft.outlookservices.Message;

</#if>
<#if includeFileServices>
// File Services imports
import com.microsoftopentechnologies.intellij.FileServicesClient;
import com.microsoft.fileservices.Item;

</#if>
<#if includeListServices>
// List Services imports
import com.microsoftopentechnologies.intellij.ListServicesClient;
import com.microsoft.listservices.Query;
import com.microsoft.listservices.SPList;

</#if>
public class ${activityClass} extends Activity {

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<#if includeOutlookServices>

        Futures.addCallback(getInboxMessages(), new FutureCallback<List<Message>>() {
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

        Futures.addCallback(getMyFiles(), new FutureCallback<List<Item>>() {
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

        Futures.addCallback(getMyLists(), new FutureCallback<List<SPList>>() {
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
	
    public ListenableFuture<List<Message>> getInboxMessages() {
        OutlookServicesClient client = new OutlookServicesClient();
        return client.getMe().getFolders().getById("Inbox").getMessages().read();
    }
</#if>
<#if includeFileServices>

   public ListenableFuture<List<SPList>> getMyFiles() {
        FileServicesClient client = new FileServicesClient();
        return client.getfiles();
    }
</#if>
<#if includeListServices>

    public ListenableFuture<List<SPList>> getMyLists() {
        ListServicesClient client = new ListServicesClient();
        return client.getLists(new Query());
    }
</#if>
}