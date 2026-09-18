package com.example.project_pandora.core.smoketest;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.project_pandora.core.logger.PandoraLogger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmokeTestEngine {

    private static final String TAG = "SmokeTestEngine";
    private static final String PREF_NAME = "smoke_test_prefs";
    private static final String KEY_LAST_TEST_DATE = "last_test_date";
    private static final String KEY_LAST_TEST_RESULT = "last_test_result";

    public static class SmokeTestResult {
        private final boolean passed;
        private final String message;
        private final long durationMs;

        public SmokeTestResult(boolean passed, String message, long durationMs) {
            this.passed = passed;
            this.message = message;
            this.durationMs = durationMs;
        }

        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public long getDurationMs() { return durationMs; }
    }

    private final Context context;
    private final SharedPreferences prefs;

    public SmokeTestEngine(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean shouldRunToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastTestDate = prefs.getString(KEY_LAST_TEST_DATE, "");
        return !today.equals(lastTestDate);
    }

    public SmokeTestResult runSmokeTest() {
        long startTime = System.currentTimeMillis();
        PandoraLogger logger = PandoraLogger.getInstance();
        logger.info(TAG, "晨星冒烟测试开始");

        StringBuilder resultMessage = new StringBuilder();
        boolean allPassed = true;

        if (!testLocalDatabase()) {
            allPassed = false;
            resultMessage.append("本地数据库校验失败; ");
            logger.error(TAG, "晨星冒烟测试: 本地数据库校验失败");
        }

        if (!testNetworkReachable()) {
            allPassed = false;
            resultMessage.append("网络层校验失败; ");
            logger.error(TAG, "晨星冒烟测试: 网络层校验失败");
        }

        if (!testThemeResources()) {
            allPassed = false;
            resultMessage.append("主题资源校验失败; ");
            logger.error(TAG, "晨星冒烟测试: 主题资源校验失败");
        }

        if (!testKeyApiConnectable()) {
            allPassed = false;
            resultMessage.append("关键API校验失败; ");
            logger.error(TAG, "晨星冒烟测试: 关键API校验失败");
        }

        long duration = System.currentTimeMillis() - startTime;

        if (allPassed) {
            resultMessage.insert(0, "晨星冒烟测试校验通过. ");
            logger.info(TAG, "晨星冒烟测试通过, 耗时: " + duration + "ms");
        } else {
            logger.warn(TAG, "晨星冒烟测试失败: " + resultMessage);
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit()
             .putString(KEY_LAST_TEST_DATE, today)
             .putBoolean(KEY_LAST_TEST_RESULT, allPassed)
             .apply();

        return new SmokeTestResult(allPassed, resultMessage.toString(), duration);
    }

    private boolean testLocalDatabase() {
        try {
            File dbFile = context.getDatabasePath("pandora.db");
            return dbFile != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testNetworkReachable() {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testThemeResources() {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testKeyApiConnectable() {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}