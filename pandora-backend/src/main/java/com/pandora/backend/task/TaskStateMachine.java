package com.pandora.backend.task;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;

/**
 * 任务状态机——定义任务状态流转规则和权限校验。
 * 当前仅支持 IN_PROGRESS ↔ COMPLETED 双向流转。
 * 被 TaskChangeService 调用。
 */
public class TaskStateMachine {

    /**
     * 任务状态枚举。
     */
    public enum TaskStatus {
        IN_PROGRESS("进行中"),
        COMPLETED("已完成");

        private final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 用户角色枚举（用于权限判断）。
     */
    public enum UserRole {
        ADMIN, FOUNDER, DEPT_HEAD, TEAM_LEADER, EMPLOYEE
    }

    /**
     * 将字符串转换为任务状态枚举。
     *
     * @param status 状态字符串
     * @return 任务状态枚举
     */
    public static TaskStatus fromString(String status) {
        try {
            return TaskStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION, "未知状态: " + status);
        }
    }

    /**
     * 校验状态流转是否合法。
     * 规则：
     * - 仅支持 IN_PROGRESS ↔ COMPLETED 双向流转
     * - 仅责任人可标记完成
     * - 责任人或创建人可恢复为进行中
     *
     * @param fromStatus 当前状态
     * @param toStatus 目标状态
     * @param isAssignee 是否责任人
     * @param isSuperior 是否上级（创建人）
     */
    public static void validateTransition(String fromStatus, String toStatus,
                                          boolean isAssignee, boolean isSuperior) {
        TaskStatus from = fromString(fromStatus);
        TaskStatus to = fromString(toStatus);

        if (from == to || !(from == TaskStatus.IN_PROGRESS && to == TaskStatus.COMPLETED)
                && !(from == TaskStatus.COMPLETED && to == TaskStatus.IN_PROGRESS)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION,
                    String.format("非法状态跳转: %s → %s", from.getDisplayName(), to.getDisplayName()));
        }

        if (!isAssignee && !(to == TaskStatus.IN_PROGRESS && isSuperior))
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION,
                    to == TaskStatus.COMPLETED ? "仅负责人可标记完成" : "仅负责人或创建人可恢复为进行中");
    }

    /**
     * 判断状态流转是否合法（不抛异常版本）。
     *
     * @param fromStatus 当前状态
     * @param toStatus 目标状态
     * @return true 表示合法
     */
    public static boolean canTransition(String fromStatus, String toStatus) {
        try {
            TaskStatus from = fromString(fromStatus);
            TaskStatus to = fromString(toStatus);
            return from != to && (from == TaskStatus.IN_PROGRESS && to == TaskStatus.COMPLETED
                    || from == TaskStatus.COMPLETED && to == TaskStatus.IN_PROGRESS);
        } catch (Exception e) {
            return false;
        }
    }
}