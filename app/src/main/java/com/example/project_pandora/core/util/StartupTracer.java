package com.example.project_pandora.core.util;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class StartupTracer {

    private static final String TAG = "Startup";
    private static final ConcurrentHashMap<String, Long> stages = new ConcurrentHashMap<>();

    public static void begin(String stage) {
        stages.put(stage, System.currentTimeMillis());
    }

    public static void end(String stage) {
        Long start = stages.remove(stage);
        if (start != null) {
            long cost = System.currentTimeMillis() - start;
            Log.i(TAG, "[" + stage + "] cost=" + cost + "ms");
        }
    }
}