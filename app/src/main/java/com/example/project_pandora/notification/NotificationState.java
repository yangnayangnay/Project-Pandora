package com.example.project_pandora.notification;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.NotificationApi;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;

/** Unread state is independent from notification permission and alert preferences. */
public final class NotificationState {
    public static final class Count {
        public final long userId, unread;
        Count(long userId,long unread) { this.userId=userId; this.unread=unread; }
    }
    private static final Subject<Count> counts=BehaviorSubject.<Count>create().toSerialized();
    private static final java.util.concurrent.ConcurrentHashMap<Long,java.util.concurrent.atomic.AtomicLong> revisions=new java.util.concurrent.ConcurrentHashMap<>();
    private static java.util.concurrent.atomic.AtomicLong revision(long user) {
        return revisions.computeIfAbsent(user,key -> new java.util.concurrent.atomic.AtomicLong());
    }
    private NotificationState() {}
    public static SharedPreferences preferences(Context context,long user) {
        return context.getSharedPreferences(NotificationDelivery.preferencesName(user),Context.MODE_PRIVATE);
    }
    public static Observable<Count> changes() { return counts.hide(); }
    public static void publish(Context context,long user,long count) {
        if (TokenManager.getInstance(context).getUserId()!=user) return;
        long safe=Math.max(0,count);
        revision(user).incrementAndGet();
        preferences(context,user).edit().putLong("unread_count",safe).apply();
        counts.onNext(new Count(user,safe));
    }
    public static void refresh(Context context) {
        Context app=context.getApplicationContext();
        TokenManager session=TokenManager.getInstance(app);
        long user=session.getUserId(); String token=session.getToken();
        if (user<0 || token==null) return;
        long ticket=revision(user).incrementAndGet();
        NetworkClient.getInstance(app).createService(NotificationApi.class).unreadCount()
                .subscribeOn(Schedulers.io()).timeout(15,TimeUnit.SECONDS).subscribe(response -> {
                    if (ticket==revision(user).get() && token.equals(session.getToken()) && response.isSuccess() && response.getData()!=null)
                        publish(app,user,response.getData());
                }, error -> android.util.Log.w("NotificationState","Unread refresh failed",error));
    }
}
