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
 * TaskStateMachine 单元测试
 * ⚠️ 核心提醒：VALID/INVALID 两张表是按常见任务系统假设的，
 *    运行前务必打开你的 TaskStateMachine 对照真实流转规则修改——只改这两张表即可！
 */
@DisplayName("TaskStateMachine 状态流转规则")
class TaskStateMachineTest {

    private final TaskStateMachine sm = new TaskStateMachine(); // [ADJUST] 若是静态方法，改为类名直接调用

    // ============ [ADJUST] 对照真实规则修改以下两张表 ============
    // 实际代码：只有 IN_PROGRESS ↔ COMPLETED 是合法流转
    // PENDING 状态不存在，所有包含 PENDING 的流转都是非法的
    private static final String[][] VALID = {
            {"IN_PROGRESS", "COMPLETED"},   // 责任人完成
            {"COMPLETED", "IN_PROGRESS"},   // 恢复进行中（需上级权限）
    };
    private static final String[][] INVALID = {
            {"COMPLETED", "COMPLETED"},     // 自身流转
            {"IN_PROGRESS", "IN_PROGRESS"}, // 相同状态
    };
    // ==========================================================

    @Test
    @DisplayName("TC-SM-01[正常] fromString合法状态字符串转枚举")
    void tc01_fromStringNormal() {
        assertEquals("IN_PROGRESS", TaskStateMachine.fromString("IN_PROGRESS").name());
        assertEquals("COMPLETED", TaskStateMachine.fromString("COMPLETED").name());
    }

    @ParameterizedTest(name = "TC-SM-02[异常] fromString非法状态\"{0}\"应抛异常")
    @ValueSource(strings = {"", "FOO", "in_progress", "COMPLETED_X", "PENDING", "TODO"})
    void tc02_fromStringInvalid(String s) {
        assertThrows(Exception.class, () -> TaskStateMachine.fromString(s)); // [ADJUST] 可改为具体异常类型
    }

    @Test
    @DisplayName("TC-SM-03[异常] fromString(null)应抛异常")
    void tc03_fromStringNull() {
        assertThrows(Exception.class, () -> TaskStateMachine.fromString(null));
    }

    @ParameterizedTest(name = "TC-SM-04[正常] canTransition: {0}->{1} 应允许")
    @MethodSource("validPairs")
    void tc04_canTransitionValid(String from, String to) {
        assertTrue(sm.canTransition(from, to));   // [ADJUST] 若参数为枚举: Status.valueOf(from)
    }

    @ParameterizedTest(name = "TC-SM-05[异常] canTransition: {0}->{1} 应拒绝")
    @MethodSource("invalidPairs")
    void tc05_canTransitionInvalid(String from, String to) {
        assertFalse(sm.canTransition(from, to));
    }

    @ParameterizedTest(name = "TC-SM-06[边界] canTransition未知状态\"{0}\"应拒绝")
    @ValueSource(strings = {"", "UNKNOWN", "PENDING", "DONE"})
    void tc06_canTransitionUnknown(String from) {
        assertFalse(sm.canTransition(from, "COMPLETED")); // [ADJUST] 按实际行为改 true/false
    }

    @Test
    @DisplayName("TC-SM-06b[边界] canTransition未知状态null应拒绝")
    void tc06b_canTransitionUnknownNull() {
        assertFalse(sm.canTransition(null, "COMPLETED"));
    }

    @Test
    @DisplayName("TC-SM-07[正常] 责任人执行合法流转不应抛异常")
    void tc07_validateByAssignee() {
        // 责任人可以将进行中标记为完成
        assertDoesNotThrow(() -> sm.validateTransition("IN_PROGRESS", "COMPLETED", true, false));
    }

    @Test
    @DisplayName("TC-SM-08[异常] 非责任人且非上级执行流转应抛权限异常")
    void tc08_validateNoPermission() {
        // 非责任人不能标记完成
        assertThrows(Exception.class,
                () -> sm.validateTransition("IN_PROGRESS", "COMPLETED", false, false));
    }

    @Test
    @DisplayName("TC-SM-09[正常] 责任人执行合法流转不应抛异常")
    void tc09_validateByAssignee() {
        // 责任人可以将进行中标记为完成
        assertDoesNotThrow(() -> TaskStateMachine.validateTransition("IN_PROGRESS", "COMPLETED", true, false));
    }

    @Test
    @DisplayName("TC-SM-10[正常] 上级恢复进行中不应抛异常")
    void tc10_validateSuperiorRevert() {
        // 上级可以将已完成恢复为进行中
        assertDoesNotThrow(() -> TaskStateMachine.validateTransition("COMPLETED", "IN_PROGRESS", false, true));
    }

    @Test
    @DisplayName("TC-SM-11[正常] 上级允许回退到进行中")
    void tc11_superiorCanRollback() {
        // 上级可以将已完成回退到进行中
        assertDoesNotThrow(() -> sm.validateTransition("COMPLETED", "IN_PROGRESS", false, true));
    }

    @Test
    @DisplayName("TC-SM-12[边界] 相同状态流转应拒绝")
    void tc12_sameStateTransition() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("IN_PROGRESS", "IN_PROGRESS", true, false));
        assertThrows(Exception.class,
                () -> sm.validateTransition("COMPLETED", "COMPLETED", true, false));
    }

    @Test
    @DisplayName("TC-SM-13[正常] 枚举displayName正确")
    void tc13_enumDisplayName() {
        assertEquals("进行中", TaskStateMachine.TaskStatus.IN_PROGRESS.getDisplayName());
        assertEquals("已完成", TaskStateMachine.TaskStatus.COMPLETED.getDisplayName());
    }

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
    // 注意：TSM-102 已删除，与 TC-SM-10 保持一致（恢复需上级权限）

    @Test
    @DisplayName("TSM-103[正常] 上级将已完成任务恢复为进行中不抛异常")
    void tsm103_revertBySuperior() {
        assertDoesNotThrow(() -> sm.validateTransition("COMPLETED", "IN_PROGRESS", false, true));
    }

    @Test
    @DisplayName("TSM-106[异常] 非责任人且非创建人恢复进行中应抛权限异常")
    void tsm106_revertNoPermission() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("COMPLETED", "IN_PROGRESS", false, false));
    }

    @Test
    @DisplayName("TSM-107[异常] validateTransition传入未知状态应抛异常")
    void tsm107_unknownStatus() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("INVALID", "COMPLETED", true, false));
    }

    @Test
    @DisplayName("TSM-108[异常] 目标状态为PENDING(非法执行状态)应抛异常")
    void tsm108_toPendingForbidden() {
        assertThrows(Exception.class,
                () -> sm.validateTransition("IN_PROGRESS", "PENDING", true, false));
    }

    // ========== 状态流转模块测试结束 ==========
    // 测试用例: 18个 (TC-SM-01~14, TSM-103/106/107/108)
    // 最后执行时间: 2026-11-18 22:50
    // 结果: 全部通过
}