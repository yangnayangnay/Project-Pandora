package com.example.project_pandora.core.util;

public class RoleDisplayUtil {
    public static String toChinese(String role) {
        if (role == null) return "\u672A\u77E5\u89D2\u8272";
        switch (role) {
            case "ADMIN": return "\u7BA1\u7406\u5458";
            case "FOUNDER": return "\u521B\u59CB\u4EBA";
            case "DEPT_HEAD": return "\u90E8\u95E8\u8D1F\u8D23\u4EBA";
            case "TEAM_LEADER": return "\u56E2\u961F\u8D1F\u8D23\u4EBA";
            case "EMPLOYEE": return "\u5458\u5DE5";
            default: return role;
        }
    }
}