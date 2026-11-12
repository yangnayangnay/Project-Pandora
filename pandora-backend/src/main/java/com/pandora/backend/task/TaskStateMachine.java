package com.pandora.backend.task;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;

public class TaskStateMachine {

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

    public enum UserRole {
        ADMIN, FOUNDER, DEPT_HEAD, TEAM_LEADER, EMPLOYEE
    }

    public static TaskStatus fromString(String status) {
        try {
            return TaskStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION, "未知状态: " + status);
        }
    }

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
