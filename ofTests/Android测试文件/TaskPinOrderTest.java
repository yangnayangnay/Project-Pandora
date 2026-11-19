/**
 * 任务置顶顺序测试 - 测试任务置顶的排序逻辑
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/ui\task\TaskPinOrderTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.ui.task;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TaskPinOrderTest {
    @Test public void idsFromJsonNumbersAreCanonicalAndOrderIsStable() {
        Map<String,Object> first = Map.of("id", 1L), second = Map.of("id", 2.0), third = Map.of("ID", "3");
        List<Map<String,Object>> rows = new ArrayList<>(List.of(first, second, third));
        TaskPinOrder.sort(rows, Set.of("2", "3")); assertEquals(List.of(second, third, first), rows);
        assertTrue(TaskPinOrder.isPinned(second, Set.of("2")));
        TaskPinOrder.sort(rows, Set.of()); assertEquals(List.of(second, third, first), rows);
        assertFalse(TaskPinOrder.isPinned(Map.of("name", "无编号"), Set.of("2")));
    }
}
