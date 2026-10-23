package com.example.project_pandora.ui.log;

import java.util.Map;

/**
 * REQ-23-06：日志创建人展示工具（LogAdapter / SubordinateLogAdapter 共用）。
 * 规则：本人日志隐藏创建人标记；他人日志显示 real_name > username > "未知"。
 */
public final class LogAuthorDisplay {

    private LogAuthorDisplay() {
    }

    /**
     * 判断日志是否为当前用户本人所写。
     * 读 user_id 与 currentUserId 比较（Number 统一转 long）；
     * user_id 缺失或类型非法时视为非本人（按他人处理，防御性展示）。
     */
    public static boolean isOwnLog(Map<String, Object> log, long currentUserId) {
        if (log == null) {
            return false;
        }
        Object userId = log.get("user_id");
        if (userId == null) {
            userId = log.get("USER_ID");
        }
        // Round26: 解析统一收敛至 NumberCompat（原非 Number 一律按他人处理语义保持）
        Long uid = com.example.project_pandora.core.util.NumberCompat.toLong(userId);
        return uid != null && uid == currentUserId;
    }

    /**
     * 解析日志创建人显示名：real_name 非空 > username 非空 > "未知"。
     */
    public static String resolveAuthorName(Map<String, Object> log) {
        if (log != null) {
            Object realName = log.get("real_name");
            if (realName != null && !realName.toString().trim().isEmpty()) {
                return realName.toString();
            }
            Object username = log.get("username");
            if (username != null && !username.toString().trim().isEmpty()) {
                return username.toString();
            }
        }
        return "未知";
    }
}