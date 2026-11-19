/**
 * 任务指派人测试 - 测试去重、大写列名恢复、旧格式兼容等
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\util\TaskAssigneesTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.util;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TaskAssigneesTest {
    @Test public void uppercaseSqlRowsRestoreAllAssigneesInsteadOfLegacyOne() {
        Map<String, Object> task = new HashMap<>();
        task.put("ASSIGNEES", Arrays.asList(Collections.singletonMap("ID", 11.0), Collections.singletonMap("ID", 12L)));
        task.put("ASSIGNEE_ID", 11L);
        assertEquals(Arrays.asList(11L, 12L), TaskAssignees.ids(task));
        assertEquals(11.0, TaskAssignees.people(task).get(0).get("id"));
    }
    @Test public void relationIdsAndExplicitIdsDeduplicateWithoutDroppingPeople() {
        Map<String, Object> task = new HashMap<>();
        task.put("assignees", Arrays.asList(Collections.singletonMap("user_id", "21"), Collections.singletonMap("userId", 22)));
        task.put("assigneeIds", Arrays.asList(21, "23", 0, "invalid"));
        assertEquals(Arrays.asList(21L, 22L, 23L), TaskAssignees.ids(task));
    }
    @Test public void legacySinglePersonRemainsSupportedAndInputIsNotMutated() {
        Map<String, Object> legacy = Collections.singletonMap("ASSIGNEE_ID", "31");
        assertEquals(Collections.singletonList(31L), TaskAssignees.ids(legacy));
        assertEquals(1, legacy.size());
        assertTrue(TaskAssignees.people(legacy).isEmpty());
    }
}
