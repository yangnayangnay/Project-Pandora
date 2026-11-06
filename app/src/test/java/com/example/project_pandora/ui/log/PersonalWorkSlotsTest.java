package com.example.project_pandora.ui.log;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersonalWorkSlotsTest {

    @Test
    public void nullAndEmptySavedResponsesBecomeTenBlankSlots() {
        assertTenBlankSlots(PersonalWorkSlots.fromSaved(null));
        assertTenBlankSlots(PersonalWorkSlots.fromSaved(Collections.emptyList()));
    }

    @Test
    public void snakeCaseRankAndGsonDoubleIdLandInTheMatchingSlot() {
        Map<String, Object> saved = row(
                "rank_order", 7.0,
                "id", 100.0,
                "content", "原始保存内容");
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(Collections.singletonList(saved));

        assertEquals(10, slots.size());
        Map<String, Object> slot7 = slots.get(6);
        assertEquals(7, slot7.get("rankOrder"));
        assertEquals(Long.valueOf(100L), slot7.get("id"));
        assertEquals("原始保存内容", slot7.get("content"));
    }

    @Test
    public void uppercaseAndCamelCaseFieldsAreAccepted() {
        List<Map<String, Object>> saved = Arrays.asList(
                row("RANK_ORDER", 4.0, "ID", 41.0, "CONTENT", "大写字段内容"),
                row("rankOrder", 6.0, "id", 61.0, "content", "驼峰字段内容"));
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(saved);

        assertEquals(4, slots.get(3).get("rankOrder"));
        assertEquals(Long.valueOf(41L), slots.get(3).get("id"));
        assertEquals("大写字段内容", slots.get(3).get("content"));

        assertEquals(6, slots.get(5).get("rankOrder"));
        assertEquals(Long.valueOf(61L), slots.get(5).get("id"));
        assertEquals("驼峰字段内容", slots.get(5).get("content"));
    }

    @Test
    public void sparseRanksKeepTheEmptyPositionBetweenOccupiedSlots() {
        List<Map<String, Object>> saved = Arrays.asList(
                row("rank_order", 1.0, "id", 11.0, "content", "第一个位置"),
                row("rank_order", 3.0, "id", 13.0, "content", "第三个位置"));
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(saved);

        assertEquals(1, slots.get(0).get("rankOrder"));
        assertEquals("第一个位置", slots.get(0).get("content"));
        assertEquals(2, slots.get(1).get("rankOrder"));
        assertEquals("", slots.get(1).get("content"));
        assertEquals(3, slots.get(2).get("rankOrder"));
        assertEquals(Long.valueOf(13L), slots.get(2).get("id"));
        assertEquals("第三个位置", slots.get(2).get("content"));
    }

    @Test
    public void unsortedRanksAppearAtTheirRankedSlots() {
        List<Map<String, Object>> saved = Arrays.asList(
                row("rank_order", 9.0, "id", 900.0, "content", "第九个位置"),
                row("rank_order", 2.0, "id", 200.0, "content", "第二个位置"));
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(saved);

        assertEquals(9, slots.get(8).get("rankOrder"));
        assertEquals(Long.valueOf(900L), slots.get(8).get("id"));
        assertEquals("第九个位置", slots.get(8).get("content"));

        assertEquals(2, slots.get(1).get("rankOrder"));
        assertEquals(Long.valueOf(200L), slots.get(1).get("id"));
        assertEquals("第二个位置", slots.get(1).get("content"));
    }

    @Test
    public void editingSourceMapsOrSlotsDoesNotMutateTheOtherView() {
        Map<String, Object> first = row(
                "rank_order", 2.0,
                "id", 20.0,
                "content", "原始二");
        Map<String, Object> second = row(
                "rank_order", 5.0,
                "id", 50.0,
                "content", "原始五");
        List<Map<String, Object>> saved = new ArrayList<>(Arrays.asList(first, second));
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(saved);
        Map<String, Object> slot2 = slots.get(1);
        Map<String, Object> slot5 = slots.get(4);

        first.put("content", "源内容已修改");
        first.put("id", 999.0);
        assertEquals("原始二", slot2.get("content"));
        assertEquals(Long.valueOf(20L), slot2.get("id"));

        slot2.put("content", "槽位内容已修改");
        slot2.put("id", 777.0);
        assertEquals("源内容已修改", first.get("content"));
        assertEquals(999.0, first.get("id"));
        assertEquals("原始五", second.get("content"));
        assertEquals("原始五", slot5.get("content"));
        assertEquals(Long.valueOf(50L), slot5.get("id"));
        assertEquals("", slots.get(0).get("content"));
    }

    @Test
    public void contentPreservesChinesePunctuationAndLongText() {
        String punctuation = "复盘：目标、风险与结论；同步（含下一步）。";
        String longText = "跨部门协作需对齐目标、拆解任务，并持续记录依赖、风险与结论。"
                .repeat(8);
        assertTrue(longText.length() > 100);

        List<Map<String, Object>> saved = Arrays.asList(
                row("rank_order", 1.0, "id", 1.0, "content", punctuation),
                row("rank_order", 3.0, "id", 3.0, "content", longText));
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(saved);

        assertEquals(punctuation, slots.get(0).get("content"));
        assertEquals(longText, slots.get(2).get("content"));
    }

    private static void assertTenBlankSlots(List<Map<String, Object>> slots) {
        assertEquals(10, slots.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, slots.get(i).get("rankOrder"));
            assertEquals("", slots.get(i).get("content"));
        }
    }

    private static Map<String, Object> row(Object... entries) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            row.put((String) entries[i], entries[i + 1]);
        }
        return row;
    }
}
