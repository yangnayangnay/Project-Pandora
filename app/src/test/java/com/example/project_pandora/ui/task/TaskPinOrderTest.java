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
