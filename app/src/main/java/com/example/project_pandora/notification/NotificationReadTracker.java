package com.example.project_pandora.notification;

import android.content.Context;
import android.content.Intent;
import com.example.project_pandora.core.network.*;
import com.example.project_pandora.data.remote.NotificationApi;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;

/** Opening a system notification marks that record, never the entire inbox, as read. */
public final class NotificationReadTracker {
    private NotificationReadTracker() {}
    public static void opened(Context context,Intent intent) {
        if(intent==null || !intent.hasExtra("notificationId")) return;
        Context app=context.getApplicationContext(); TokenManager session=TokenManager.getInstance(app);
        long user=session.getUserId(), id=intent.getLongExtra("notificationId",-1);
        String token=session.getToken();
        if(token==null || id<=0 || intent.getLongExtra("notificationUserId",-1)!=user) return;
        NetworkClient.getInstance(app).createService(NotificationApi.class).markAsRead(id)
                .subscribeOn(Schedulers.io()).timeout(15,TimeUnit.SECONDS).subscribe(response -> {
                    if(response.isSuccess() && token.equals(session.getToken()) && session.getUserId()==user) {
                        intent.removeExtra("notificationId");
                        NotificationState.refresh(app);
                    }
                },error -> android.util.Log.w("NotificationRead","Mark read failed; retry on return",error));
    }
}
