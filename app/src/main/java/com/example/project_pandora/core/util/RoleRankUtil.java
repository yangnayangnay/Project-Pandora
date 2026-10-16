package com.example.project_pandora.core.util;

/**
 * TASK-R18-011: RoleRank 等级映射工具
 *
 * <p>提供等级序号映射，用于人员关系树形展示的同层排序：
 * ADMIN=1 > FOUNDER=2 > DEPT_HEAD=3 > TEAM_LEADER=4 > EMPLOYEE=5，未知角色=99。</p>
 *
 * <p>独立于被 UserRelationActivity/UserRelationAdapter 现有权限展示依赖的 PermissionLevel
 * （PermissionLevel 的 ADMIN=0/FOUNDER=4/DEPT_HEAD=3/TEAM_LEADER=2/EMPLOYEE=1 语义为"权限分"，
 * 其数值最小者 ADMIN=0 若用于等级排序会让 ADMIN 排在末位，与规格 ADMIN 最高冲突）。
 * PermissionLevel 保持原样不动，本类仅供 Android 树形适配器同层排序使用。</p>
 */
public final class RoleRankUtil {

    private RoleRankUtil() {}

    /**
     * 返回角色的等级序号（数值越小等级越高）。
     * ADMIN=1, FOUNDER=2, DEPT_HEAD=3, TEAM_LEADER=4, EMPLOYEE=5, 未知角色=99
     *
     * @param role 角色字符串，可为 null
     * @return 等级序号
     */
    public static int rankOf(String role) {
        if (role == null) return 99;
        switch (role) {
            case "ADMIN": return 1;
            case "FOUNDER": return 2;
            case "DEPT_HEAD": return 3;
            case "TEAM_LEADER": return 4;
            case "EMPLOYEE": return 5;
            default: return 99;
        }
    }
}