package com.example.project_pandora.core.config;

import java.util.Arrays;
import java.util.List;

public enum LogCountLimit {
    COUNT_20(20, "20条"),
    COUNT_50(50, "50条"),
    COUNT_100(100, "100条"),
    UNLIMITED(0, "不限");

    private final int count;
    private final String displayName;

    LogCountLimit(int count, String displayName) {
        this.count = count;
        this.displayName = displayName;
    }

    public int getCount() {
        return count;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LogCountLimit fromCount(int count) {
        for (LogCountLimit limit : values()) {
            if (limit.count == count) {
                return limit;
            }
        }
        return COUNT_50;
    }

    public static List<LogCountLimit> orderedValues() {
        return Arrays.asList(COUNT_20, COUNT_50, COUNT_100, UNLIMITED);
    }
}