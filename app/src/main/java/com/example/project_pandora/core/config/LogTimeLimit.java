package com.example.project_pandora.core.config;

import java.util.Arrays;
import java.util.List;

public enum LogTimeLimit {
    DAYS_7(7, "最近7天"),
    DAYS_30(30, "最近30天"),
    DAYS_90(90, "最近90天"),
    ALL(0, "全部");

    private final int days;
    private final String displayName;

    LogTimeLimit(int days, String displayName) {
        this.days = days;
        this.displayName = displayName;
    }

    public int getDays() {
        return days;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LogTimeLimit fromDays(int days) {
        for (LogTimeLimit limit : values()) {
            if (limit.days == days) {
                return limit;
            }
        }
        return DAYS_30;
    }

    public static List<LogTimeLimit> orderedValues() {
        return Arrays.asList(DAYS_7, DAYS_30, DAYS_90, ALL);
    }
}