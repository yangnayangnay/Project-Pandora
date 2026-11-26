package com.pandora.backend.task;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskStateMachine 单元测试——验证任务状态流转规则。
 * 覆盖正常流转、非法流转、权限校验、枚举功能等场景。
 */
@DisplayName("TaskStateMachine 状态流转规则")
class TaskStateMachineTest {

    private final TaskStateMachine sm = new TaskStateMachine();

    // 合法流转对：IN_PROGRESS <-> COMPLETED
    private static final String[][] VALID = {
            {"IN_PROGRESS", "COMPLETED"},   // 责任人完成
            {"COMPLETED", "IN_PROGRESS"},   // 恢复进行中（需上级权限）
    };
    // 非法流转对：相同状态流转
    private static final String[][] INVALID = {
            {"COMPLETED", "COMPLETED"},     // 自身流转
            {"IN_PROGRESS", "IN_PROGRESS"}, // 相同状态
    };

    /**
     * TC-SM-01[正常] fromString 合法状态字符串转枚举。
     */
    @Test
    @DisplayName("TC-SM-01[正常] fromString合法状态字符串转枚举")
    void tc01_fromStringNormal() {
        assertEquals("IN_PROGRESS", TaskStateMachine.fromString("IN_PROGRESS").name());
        assertEquals("COMPLETED", TaskStateMachine.fromString("COMPLETED").name());
    }

    /**
     * TC-SM-02[异常] fromString 非法状态应抛异常。
     */
    @ParameterizedTest(name = "TC-SM-02[异常] fromString非法状态\"{0}\"应抛异常")
    @ValueSource(strings = {"", "FOO", "in_progress", "COMPLETED_X", "PENDING", "TODO"})
    void tc02_fromStringInvalid(String s) {
        assertThrows(Exception.class, () -> TaskStateMachine.fromString(s));
    }

    /**
     * TC-SM-03[异常] fromString(null) 应抛异常。
     */
    @Test
    @DisplayName("TC-SM-03[异常] fromString(null)应抛异常")
    void tc03_fromStringNull() {
        assertThrows(Exception.class, () -> TaskStateMachine.fromString(null));
    }

    /**
     * TC-SM-04[正常] canTransition 合法流转应返回 true。
     */
    @ParameterizedTest(name = "TC-SM-04[正常] canTransition: {0}->{1} 应允许")
    @MethodSource("validPairs")
    void tc04_canTransitionValid(String from, String to) {
        assertTrue(sm.canTransition(from, to));
    }

    /**
     * TC-SM-05[异常] canTransition 非法流转应返回 false。
     */
    @ParameterizedTest(name = "TC-SM-05[异常] canTransition: {0}->{1} 应拒绝")
    @MethodSource("invalidPairs")
    void tc05_canTransitionInvalid(String from, String to) {
        assertFalse(sm.canTransition(from, to));
    }

    /**
     * TC-SM-06[边界] canTransition 未知状态应返回 false。
     */
    @ParameterizedTest(name = "TC-SM-06[边界] canTransition未知状态\"{0}\"应拒绝")
    @ValueSource(strings = {"", "UNKNOWN", "PENDING", "DONE"})
    void tc06_canTransitionUnknown(String from) {
        assertFalse(sm.canTransition(from, "COMPLETED"));
    }

    /**
     * TC-SM-06b[边界] canTransition 未知状态 null 应返回 false。
     */
    @Test
    @DisplayName("TC-SM-06b[边界] canTransition未知状态null应拒绝")
    void tc06b_canTransitionUnknownNull() {
        assertFalse(sm.canTransition(null, "COMPLETED"));
    }

    /**
     * TC-SM-07[正常] 责任人执行合法流转不应抛异常。
     */
    @Test
    @DisplayName("TC-SM-07[正常] 责任人执行合法流转不应抛异常")
    void tc07_validateByAssignee() {
        assertDoesNotThrow(() -> sm.validateTransition("IN_PROGRESS", "COMPLETED", true, false));
    }

    /**
     * TC-SM-08[异常] 非责任人且非上级执行流转应抛权限异常。
     */
    @Test
    @DisplayName("TC-SM-08[异常] 非责任人且非上级执行流转应抛权限异常")
    void tc08_validateNoPermission() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("IN_PROGRESS", "COMPLETED", false, false));
    }

    /**
     * TC-SM-09[正常] 责任人执行合法流转不应抛异常（静态方法调用）。
     */
    @Test
    @DisplayName("TC-SM-09[正常] 责任人执行合法流转不应抛异常")
    void tc09_validateByAssignee() {
        assertDoesNotThrow(() -> TaskStateMachine.validateTransition("IN_PROGRESS", "COMPLETED", true, false));
    }

    /**
     * TC-SM-10[正常] 上级恢复进行中不应抛异常。
     */
    @Test
    @DisplayName("TC-SM-10[正常] 上级恢复进行中不应抛异常")
    void tc10_validateSuperiorRevert() {
        assertDoesNotThrow(() -> TaskStateMachine.validateTransition("COMPLETED", "IN_PROGRESS", false, true));
    }

    /**
     * TC-SM-11[正常] 上级允许回退到进行中。
     */
    @Test
    @DisplayName("TC-SM-11[正常] 上级允许回退到进行中")
    void tc11_superiorCanRollback() {
        assertDoesNotThrow(() -> sm.validateTransition("COMPLETED", "IN_PROGRESS", false, true));
    }

    /**
     * TC-SM-12[边界] 相同状态流转应拒绝。
     */
    @Test
    @DisplayName("TC-SM-12[边界] 相同状态流转应拒绝")
    void tc12_sameStateTransition() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("IN_PROGRESS", "IN_PROGRESS", true, false));
        assertThrows(Exception.class,
                () -> sm.validateTransition("COMPLETED", "COMPLETED", true, false));
    }

    /**
     * TC-SM-13[正常] 枚举 displayName 正确。
     */
    @Test
    @DisplayName("TC-SM-13[正常] 枚举displayName正确")
    void tc13_enumDisplayName() {
        assertEquals("进行中", TaskStateMachine.TaskStatus.IN_PROGRESS.getDisplayName());
        assertEquals("已完成", TaskStateMachine.TaskStatus.COMPLETED.getDisplayName());
    }

    /**
     * TC-SM-14[边界] 所有枚举值可遍历。
     */
    @Test
    @DisplayName("TC-SM-14[边界] 所有枚举值可遍历")
    void tc14_enumValues() {
        TaskStateMachine.TaskStatus[] values = TaskStateMachine.TaskStatus.values();
        assertEquals(2, values.length);
        assertTrue(Arrays.asList(values).containsAll(Arrays.asList(
                TaskStateMachine.TaskStatus.IN_PROGRESS,
                TaskStateMachine.TaskStatus.COMPLETED
        )));
    }

    static Stream<Arguments> validPairs() {
        return Arrays.stream(VALID).map(a -> Arguments.of(a[0], a[1]));
    }

    static Stream<Arguments> invalidPairs() {
        return Arrays.stream(INVALID).map(a -> Arguments.of(a[0], a[1]));
    }

    // ============ V2.0 增量用例 ============

    /**
     * TSM-103[正常] 上级将已完成任务恢复为进行中不抛异常。
     */
    @Test
    @DisplayName("TSM-103[正常] 上级将已完成任务恢复为进行中不抛异常")
    void tsm103_revertBySuperior() {
        assertDoesNotThrow(() -> sm.validateTransition("COMPLETED", "IN_PROGRESS", false, true));
    }

    /**
     * TSM-106[异常] 非责任人且非创建人恢复进行中应抛权限异常。
     */
    @Test
    @DisplayName("TSM-106[异常] 非责任人且非创建人恢复进行中应抛权限异常")
    void tsm106_revertNoPermission() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("COMPLETED", "IN_PROGRESS", false, false));
    }

    /**
     * TSM-107[异常] validateTransition 传入未知状态应抛异常。
     */
    @Test
    @DisplayName("TSM-107[异常] validateTransition传入未知状态应抛异常")
    void tsm107_unknownStatus() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("INVALID", "COMPLETED", true, false));
    }

    /**
     * TSM-108[异常] 目标状态为 PENDING（非法执行状态）应抛异常。
     */
    @Test
    @DisplayName("TSM-108[异常] 目标状态为PENDING(非法执行状态)应抛异常")
    void tsm108_toPendingForbidden() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("IN_PROGRESS", "PENDING", true, false));
    }
}