package com.example.project_pandora.core.crash;

import android.content.Context;
import android.widget.Toast;

import com.example.project_pandora.core.logger.PandoraLogger;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context context;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void register(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            PandoraLogger logger = PandoraLogger.getInstance();
            logger.error(TAG, "未捕获异常: " + e.getMessage(), e);

            new Thread(() -> {
                try {
                    Thread.sleep(200);
                    Toast.makeText(context, "应用遇到问题，请稍后重试", Toast.LENGTH_LONG).show();
                } catch (InterruptedException ignored) {}
            }).start();
        } catch (Exception ignored) {
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        }
    }
}