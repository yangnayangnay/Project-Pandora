package com.example.project_pandora.notification;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.project_pandora.core.network.*;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.core.util.TaskTags;
import com.example.project_pandora.data.remote.NotificationApi;
import java.util.*;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;

/** Durable delivery acknowledgement; network waits never hold the processing lock. */
public final class NotificationPollEngine {
    private static final Object DELIVERY_LOCK=new Object();
    private NotificationPollEngine() {}
    public static void confirmReadThrough(Context context,long user,long boundary) {
        synchronized(DELIVERY_LOCK) {
            var prefs=NotificationState.preferences(context,user);
            prefs.edit().putLong("read_through_id",Math.max(boundary,prefs.getLong("read_through_id",0)))
                    .putLong("last_notification_id",Math.max(boundary,prefs.getLong("last_notification_id",0))).commit();
            android.app.NotificationManager manager=context.getSystemService(android.app.NotificationManager.class);
            if(manager!=null) for(var notification:manager.getActiveNotifications()) {
                if(("work_user_"+user).equals(notification.getTag()) && notification.getId()<=boundary)
                    manager.cancel(notification.getTag(),notification.getId());
            }
        }
    }
    public static boolean pollOnce(Context context) { return pollOnce(context,1,null); }
    public interface ActiveCall { void set(Call<?> call); }
    public static boolean pollOnce(Context context,int seconds,ActiveCall active) {
        Context app=context.getApplicationContext(); TokenManager session=TokenManager.getInstance(app);
        long user=session.getUserId(); String token=session.getToken();
        if(user<0 || token==null) return false;
        SharedPreferences prefs=NotificationState.preferences(app,user);
        NotificationApi api=NetworkClient.getInstance(app).createService(NotificationApi.class);
        long requestedAfter=prefs.getLong("last_notification_id",0);
        Call<ApiResponse<List<Map<String,Object>>>> call=api.waitForNotifications(
                requestedAfter,prefs.getLong("unread_count",-1),seconds);
        if(active!=null) active.set(call);
        android.os.PowerManager.WakeLock processing=null;
        try {
            var response=call.execute();
            if(!response.isSuccessful() || response.body()==null || !response.body().isSuccess()) return false;
            ApiResponse<List<Map<String,Object>>> body=response.body();
            if(!token.equals(session.getToken()) || session.getUserId()!=user) return false;
            processing=app.getSystemService(android.os.PowerManager.class).newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,"pandora:notification-delivery");
            processing.acquire(15000);
            Map<String,Object> enabled=preferences(api,prefs);
            synchronized(DELIVERY_LOCK) {
                if(!token.equals(session.getToken()) || session.getUserId()!=user) return false;
                if(requestedAfter<prefs.getLong("read_through_id",0)) NotificationState.refresh(app);
                else if(body.getUnreadCount()!=null) NotificationState.publish(app,user,body.getUnreadCount());
                long cursor=prefs.getLong("last_notification_id",0);
                if(body.getMaxNotificationId()!=null && cursor==requestedAfter && cursor>body.getMaxNotificationId()) {
                    cursor=NotificationDelivery.baseline(cursor,body.getMaxNotificationId());
                    prefs.edit().remove("read_through_id").commit();
                }
                List<Map<String,Object>> rows=body.getData()==null ? new ArrayList<>() : new ArrayList<>(body.getData());
                rows.sort(Comparator.comparingLong(row -> NumberCompat.toLongOrDefault(TaskTags.value(row,"id"),0)));
                for(var row:rows) {
                    long id=NumberCompat.toLongOrDefault(TaskTags.value(row,"id"),0);
                    if(id<=cursor) continue;
                    String type=Objects.toString(TaskTags.value(row,"type"),"");
                    boolean read=NumberCompat.toBool(TaskTags.value(row,"is_read"))
                            || NotificationDelivery.confirmedRead(id,prefs.getLong("read_through_id",0));
                    boolean alerts=NumberCompat.toBool(enabled.getOrDefault(type,true));
                    boolean handled=!NotificationDelivery.shouldAlert(read,alerts) || TaskNotificationService.postNotification(
                            app,id,type,"通知",Objects.toString(TaskTags.value(row,"content"),""),
                            NumberCompat.toLong(TaskTags.value(row,"task_id")),NotificationContent.logId(row));
                    cursor=NotificationDelivery.acknowledge(cursor,id,handled);
                    // Failed posting is retried; never acknowledge a notification that was silently dropped.
                    prefs.edit().putLong("last_notification_id",cursor).commit();
                    if(!handled) return false;
                }
                prefs.edit().putLong("last_notification_id",cursor).commit();
            }
            return true;
        } catch(Exception error) {
            android.util.Log.w("NotificationReceiver","Receive failed; cursor retained",error);
            return false;
        } finally {
            if(processing!=null && processing.isHeld()) processing.release();
            if(active!=null) active.set(null);
        }
    }

    private static Map<String,Object> preferences(NotificationApi api,SharedPreferences prefs) {
        Map<String,Object> values=new HashMap<>();
        for(String type:NotificationContent.TYPES) values.put(type,prefs.getBoolean("pref_type_"+type,true));
        if(System.currentTimeMillis()-prefs.getLong("pref_timestamp",0)<60000) return values;
        try {
            var response=api.getPreferences().timeout(10,TimeUnit.SECONDS).blockingFirst();
            if(response.isSuccess() && response.getData()!=null) {
                values.putAll(response.getData());
                var editor=prefs.edit();
                for(String type:NotificationContent.TYPES) editor.putBoolean("pref_type_"+type,NumberCompat.toBool(values.get(type)));
                editor.putLong("pref_timestamp",System.currentTimeMillis()).apply();
            }
        } catch(Exception error) { android.util.Log.w("NotificationReceiver","Using cached alert preferences",error); }
        return values;
    }
}
