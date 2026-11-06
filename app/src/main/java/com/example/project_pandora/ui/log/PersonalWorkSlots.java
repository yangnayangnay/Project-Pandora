package com.example.project_pandora.ui.log;

import com.example.project_pandora.core.util.NumberCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Preserves the saved ranks, including empty positions, in the ten-slot editor. */
public final class PersonalWorkSlots {
    private PersonalWorkSlots() {}

    public static List<Map<String, Object>> fromSaved(List<Map<String, Object>> saved) {
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int rank = 1; rank <= 10; rank++) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("rankOrder", rank);
            slot.put("content", "");
            slots.add(slot);
        }
        if (saved == null) return slots;
        boolean[] occupied = new boolean[10];
        for (Map<String, Object> raw : saved) {
            Long rank = NumberCompat.toLong(value(raw, "rankOrder", "rank_order", "RANK_ORDER"));
            int index = rank != null && rank >= 1 && rank <= 10 ? rank.intValue() - 1 : -1;
            if (index < 0 || occupied[index]) {
                index = 0;
                while (index < 10 && occupied[index]) index++;
                if (index == 10) break;
            }
            occupied[index] = true;
            Map<String, Object> slot = slots.get(index);
            slot.put("id", NumberCompat.toLong(value(raw, "id", "ID")));
            Object content = value(raw, "content", "CONTENT");
            slot.put("content", content == null ? "" : content.toString());
            slot.put("relatedTaskId", value(raw, "relatedTaskId", "related_task_id", "RELATED_TASK_ID"));
        }
        return slots;
    }

    private static Object value(Map<String, Object> row, String... keys) {
        for (String key : keys) if (row.get(key) != null) return row.get(key);
        return null;
    }
}
