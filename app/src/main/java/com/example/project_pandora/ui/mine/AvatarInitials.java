package com.example.project_pandora.ui.mine;

/** The last two Unicode characters of a display name, without splitting surrogate pairs. */
public final class AvatarInitials {
    private AvatarInitials() {}
    public static String fromName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) return "用户";
        int count = value.codePointCount(0, value.length());
        return value.substring(value.offsetByCodePoints(0, Math.max(0, count - 2)));
    }
}
