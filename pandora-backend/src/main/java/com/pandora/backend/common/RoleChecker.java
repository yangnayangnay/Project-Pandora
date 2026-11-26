package com.pandora.backend.common;

/**
 * 角色校验工具类。
 * 提供管理员/创始人角色的判断方法，用于权限控制。
 * 被 Controller 和 Service 层调用。
 */
public class RoleChecker {
    /**
     * 判断是否为管理员或创始人。
     * 创始人拥有最高权限，管理员拥有系统管理权限。
     *
     * @param role 用户角色字符串
     * @return true 若角色为 ADMIN 或 FOUNDER
     */
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