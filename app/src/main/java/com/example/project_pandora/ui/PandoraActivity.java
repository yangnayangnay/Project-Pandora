package com.example.project_pandora.ui;

import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project_pandora.core.config.FontSizePrefs;

/** Keeps native sp text and calendar measurements on the same user-selected scale. */
public abstract class PandoraActivity extends AppCompatActivity {
    private float appliedFontSize;
    private final android.os.Handler notificationHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable notificationRefresh=new Runnable() {
        @Override public void run() {
            var session=com.example.project_pandora.core.network.TokenManager.getInstance(PandoraActivity.this);
            if(session.isLoggedIn() && session.getUserId()>0 && !"ADMIN".equals(session.getRole())) {
                com.example.project_pandora.notification.NotificationPollWorker.enqueueOneTimeWork(PandoraActivity.this);
                com.example.project_pandora.notification.NotificationState.refresh(PandoraActivity.this);
            }
            notificationHandler.postDelayed(this,15000);
        }
    };
    @Override protected void attachBaseContext(Context base) {
        appliedFontSize = FontSizePrefs.get(base);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.fontScale *= appliedFontSize;
        super.attachBaseContext(base.createConfigurationContext(configuration));
    }
    @Override protected void onResume() {
        super.onResume();
        com.example.project_pandora.notification.NotificationReadTracker.opened(this,getIntent());
        notificationHandler.removeCallbacks(notificationRefresh);
        notificationHandler.post(notificationRefresh);
        if (Float.compare(appliedFontSize, FontSizePrefs.get(this)) != 0) recreate();
    }
    @Override protected void onPause() {
        notificationHandler.removeCallbacks(notificationRefresh);
        super.onPause();
    }
}
