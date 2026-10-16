package com.pandora.backend.common;

public class RoleChecker {
    public static boolean isAdminOrFounder(String role) {
        return "ADMIN".equals(role) || "FOUNDER".equals(role);
    }

    /**
     * Round19 REQ-19-05/06: 管理员独占校验。
     * 用于公司十大事项写操作、账号创建、系统日志查看等仅 ADMIN 可执行的权限收口。
     *
     * @param role 当前用户角色
     * @return true 若角色为 ADMIN
     */
    public static boolean isAdminOnly(String role) {
        return "ADMIN".equals(role);
    }
}