package com.example.project_pandora.core.util;

import com.example.project_pandora.model.UserRelationItem;

import java.util.Comparator;

public class PermissionLevel {

    public static int of(String role) {
        if (role == null) return -1;
        switch (role) {
            case "FOUNDER": return 4;
            case "DEPT_HEAD": return 3;
            case "TEAM_LEADER": return 2;
            case "EMPLOYEE": return 1;
            case "ADMIN": return 0;
            default: return -1;
        }
    }

    public static Comparator<UserRelationItem> descendingComparator() {
        return (a, b) -> Integer.compare(b.getPermissionLevel(), a.getPermissionLevel());
    }
}