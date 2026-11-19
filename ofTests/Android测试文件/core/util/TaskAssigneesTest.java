package com.example.project_pandora.core.util;

/**
 * TaskAssignees 单元测试 - 任务指派人处理模块
 * 
 * 测试目标：验证任务指派人处理
 * 
 * 测试范围：
 * - 正常场景：去重不丢人
 * - 边界场景：大写列名恢复、旧格式兼容
 * 
 * 测试用例：3个
 */


import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

    /**
     * 测试用例：大写列名恢复
     * 输入类别：边界（边界值分析）
     * 测试步骤：大写SQL行恢复所有指派人
     * 预期结果：正确恢复所有指派人
     */
    @Test public void uppercaseSqlRowsRestoreAllAssigneesInsteadOfLegacyOne() {
        Map<String, Object> task = new HashMap<>();
        task.put("ASSIGNEES", Arrays.asList(Collections.singletonMap("ID", 11.0), Collections.singletonMap("ID", 12L)));
        task.put("ASSIGNEE_ID", 11L);
        assertEquals(Arrays.asList(11L, 12L), TaskAssignees.ids(task));
        assertEquals(11.0, TaskAssignees.people(task).get(0).get("id"));
    /**
     * 测试用例：去重不丢人
     * 输入类别：同类（有效等价类）
     * 测试步骤：关系ID和显式ID去重
     * 预期结果：去重且不丢人
     */
    @Test public void relationIdsAndExplicitIdsDeduplicateWithoutDroppingPeople() {
        Map<String, Object> task = new HashMap<>();
        task.put("assignees", Arrays.asList(Collections.singletonMap("user_id", "21"), Collections.singletonMap("userId", 22)));
        task.put("assigneeIds", Arrays.asList(21, "23", 0, "invalid"));
        assertEquals(Arrays.asList(21L, 22L, 23L), TaskAssignees.ids(task));
    /**
     * 测试用例：旧格式兼容
     * 输入类别：边界（边界值分析）
     * 测试步骤：旧单人格式仍被支持
     * 预期结果：输入不变异
     */
    @Test public void legacySinglePersonRemainsSupportedAndInputIsNotMutated() {
        Map<String, Object> legacy = Collections.singletonMap("ASSIGNEE_ID", "31");
        assertEquals(Collections.singletonList(31L), TaskAssignees.ids(legacy));
        assertEquals(1, legacy.size());
        assertTrue(TaskAssignees.people(legacy).isEmpty());
    }
}
