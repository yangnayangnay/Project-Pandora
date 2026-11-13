package com.example.project_pandora.core.util;


public final class LogEntryRules {
    private LogEntryRules() {}

    public static String title(String date) {
        String[] parts = date.split("-");
        return Integer.parseInt(parts[1]) + "月" + Integer.parseInt(parts[2]) + "日日志";
    }

    public static boolean hasBody(String body) {
        return body != null && body.codePoints().anyMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c));
    }
}
