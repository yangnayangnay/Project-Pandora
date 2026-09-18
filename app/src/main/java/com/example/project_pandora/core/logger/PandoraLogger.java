package com.example.project_pandora.core.logger;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PandoraLogger {

    private static final String TAG = "PandoraLogger";
    private static final String LOG_DIR = "pandora_logs";
    private static final long MAX_TOTAL_SIZE = 50 * 1024 * 1024L;
    private static final long MAX_FILE_AGE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private static final List<String> SENSITIVE_PATTERNS = Arrays.asList(
            "password", "token", "secret", "authorization", "手机号"
    );

    private static PandoraLogger instance;
    private final File logDir;

    private PandoraLogger(Context context) {
        logDir = new File(context.getFilesDir(), LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new PandoraLogger(context);
            instance.cleanupOldLogs();
        }
    }

    public static PandoraLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PandoraLogger not initialized. Call init() first.");
        }
        return instance;
    }

    public void debug(String tag, String message) {
        log("DEBUG", tag, message);
    }

    public void info(String tag, String message) {
        log("INFO", tag, message);
    }

    public void warn(String tag, String message) {
        log("WARN", tag, message);
    }

    public void error(String tag, String message, Throwable throwable) {
        log("ERROR", tag, message + " | Exception: " + throwable.getMessage());
    }

    public void error(String tag, String message) {
        log("ERROR", tag, message);
    }

    private void log(String level, String tag, String message) {
        String filteredMessage = filterSensitiveInfo(message);
        String logEntry = String.format(Locale.getDefault(), "[%s] [%s] [%s] [%s] %s",
                TIME_FORMAT.format(new Date()), level, Thread.currentThread().getName(), tag, filteredMessage);

        Log.println(mapLevelToAndroid(level), tag, filteredMessage);

        Completable.fromAction(() -> writeToFile(logEntry))
              .subscribeOn(Schedulers.io())
              .subscribe(
                      () -> {},
                      e -> Log.e(TAG, "Failed to write log to file", e)
              );
    }

    private void writeToFile(String logEntry) {
        String fileName = "pandora_" + DATE_FORMAT.format(new Date()) + ".log";
        File logFile = new File(logDir, fileName);
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log file", e);
        }
    }

    private String filterSensitiveInfo(String message) {
        String filtered = message;
        for (String pattern : SENSITIVE_PATTERNS) {
            filtered = filtered.replaceAll("(?i)" + pattern + ".*?[:=]\\s*\\S+", pattern + "=***FILTERED***");
        }
        return filtered;
    }

    private void cleanupOldLogs() {
        Completable.fromAction(() -> {
            File[] files = logDir.listFiles();
            if (files == null) return;

            long totalSize = 0;
            List<File> fileList = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (File file : files) {
                if (now - file.lastModified() > MAX_FILE_AGE_MS) {
                    file.delete();
                } else {
                    totalSize += file.length();
                    fileList.add(file);
                }
            }

            if (totalSize > MAX_TOTAL_SIZE) {
                fileList.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                for (File file : fileList) {
                    if (totalSize <= MAX_TOTAL_SIZE) break;
                    totalSize -= file.length();
                    file.delete();
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
                () -> Log.d(TAG, "Log cleanup completed"),
                e -> Log.e(TAG, "Log cleanup failed", e)
        );
    }

    private int mapLevelToAndroid(String level) {
        switch (level) {
            case "DEBUG": return Log.DEBUG;
            case "INFO": return Log.INFO;
            case "WARN": return Log.WARN;
            case "ERROR": return Log.ERROR;
            default: return Log.INFO;
        }
    }
}