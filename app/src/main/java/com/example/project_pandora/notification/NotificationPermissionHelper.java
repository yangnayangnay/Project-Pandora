package com.example.project_pandora.notification;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Round24 REQ-24-09: POST_NOTIFICATIONS 运行时申请（API 33+）。
 * sessionRequested 为进程级内存标志（不持久化）——系统自身记录"不再询问"状态，
 * 二次拒绝后 requestPermissions 仅回调 DENIED 无害；结果回调不做业务分支
 * （通知弹出已有 try-catch 静默语义，权限拒绝时应用内轮询/业务全正常）。
 */
public final class NotificationPermissionHelper {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 20240901;

    private static boolean sessionRequested = false;

    private NotificationPermissionHelper() {
    }

    public static void requestOnce(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT < 33) return;
        if (sessionRequested) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        sessionRequested = true;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_CODE_POST_NOTIFICATIONS);
    }
}