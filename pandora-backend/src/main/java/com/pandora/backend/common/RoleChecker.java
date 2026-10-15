package com.pandora.backend.common;

public class RoleChecker {
    public static boolean isAdminOrFounder(String role) {
        return "ADMIN".equals(role) || "FOUNDER".equals(role);
    }
}