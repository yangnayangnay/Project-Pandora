package com.pandora.backend.task;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaskStateMachine {

    public enum TaskStatus {
        PENDING("待接收"),
        IN_PROGRESS("进行中"),
        COMPLETED("已完成"),
        CONFIRMED("已确认"),
        REJECTED("已退回");

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

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(TaskStatus.PENDING, new HashSet<>(java.util.Arrays.asList(
                TaskStatus.IN_PROGRESS, TaskStatus.REJECTED)));
        TRANSITIONS.put(TaskStatus.IN_PROGRESS, new HashSet<>(java.util.Arrays.asList(
                TaskStatus.COMPLETED, TaskStatus.REJECTED)));
        TRANSITIONS.put(TaskStatus.COMPLETED, new HashSet<>(java.util.Arrays.asList(
                TaskStatus.CONFIRMED, TaskStatus.REJECTED, TaskStatus.IN_PROGRESS)));
        TRANSITIONS.put(TaskStatus.CONFIRMED, new HashSet<>());
        TRANSITIONS.put(TaskStatus.REJECTED, new HashSet<>(java.util.Arrays.asList(
                TaskStatus.PENDING, TaskStatus.IN_PROGRESS)));
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

        if (!TRANSITIONS.containsKey(from) || !TRANSITIONS.get(from).contains(to)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION,
                    String.format("非法状态跳转: %s → %s", from.getDisplayName(), to.getDisplayName()));
        }

        switch (to) {
            case IN_PROGRESS:
                if (!isAssignee) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "仅责任人可开始任务");
                }
                break;
            case COMPLETED:
                if (!isAssignee) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "仅责任人可标记完成");
                }
                break;
            case CONFIRMED:
            case REJECTED:
                if (!isSuperior) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "仅上级可确认/退回任务");
                }
                break;
            default:
                break;
        }
    }

    public static boolean canTransition(String fromStatus, String toStatus) {
        try {
            TaskStatus from = fromString(fromStatus);
            TaskStatus to = fromString(toStatus);
            return TRANSITIONS.containsKey(from) && TRANSITIONS.get(from).contains(to);
        } catch (Exception e) {
            return false;
        }
    }
}