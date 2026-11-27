package com.example.project_pandora.notification;

/** Transport-independent cursor rules, shared by foreground and background receivers. */
public final class NotificationDelivery {
    private NotificationDelivery() {}
    public static String preferencesName(long user) { return "notification_prefs_user_"+user; }
    public static long baseline(long cursor,long serverMax) { return cursor<0 || cursor>serverMax ? 0 : cursor; }
    public static boolean shouldAlert(boolean read,boolean enabled) { return !read && enabled; }
    public static boolean confirmedRead(long id,long readThrough) { return id>0 && id<=readThrough; }
    public static long acknowledge(long cursor,long id,boolean handled) { return handled ? Math.max(cursor,id) : cursor; }
}
