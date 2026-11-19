/**
 * 日历单元格度量测试 - 测试日历单元格的尺寸和度量
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/ui\widget\CalendarCellMetricsTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.ui.widget;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalendarCellMetricsTest {
    @Test public void holidaysFollowEntriesAndOverflowWithoutTakingTheFirstLane() {
        for (float scale : new float[]{1f, 1.3f, 2f, 3f}) {
            CalendarCellMetrics m = new CalendarCellMetrics(scale);
            assertEquals(m.dateHeight(), m.taskTop(), 0f);
            for (int count = 0; count <= 4; count++) {
                assertEquals(m.taskTop() + count * m.entryStep(), m.specialTop(count, false), .001f);
            }
            float top = m.specialTop(4, true);
            assertTrue(top > m.taskTop() + 4 * m.entryStep() + m.entryHeight());
            assertTrue(top + m.specialRowHeight() < m.rowHeight());
        }
    }
    @Test public void fourEntriesAndOverflowStayWithinTheirDateAtEveryFontScale() {
        for (float scale : new float[]{1f, 1.3f, 1.5f, 2f, 3f}) {
            CalendarCellMetrics metrics = new CalendarCellMetrics(scale);
            float priorBottom = metrics.taskTop();
            for (int lane = 0; lane <= CalendarCellMetrics.VISIBLE_ENTRIES; lane++) {
                float top = metrics.taskTop() + lane * metrics.entryStep();
                float bottom = top + metrics.entryHeight();
                assertTrue("Entries overlap at font scale " + scale, top >= priorBottom);
                assertTrue("Entry crosses the date boundary", bottom < metrics.rowHeight());
                assertTrue("One line does not fit the entry", metrics.entryHeight() >= 11 * scale * 1.3f + 4);
                priorBottom = bottom;
            }
        }
    }
    @Test public void textScalingIncreasesAvailableSpaceInsteadOfShrinkingText() {
        CalendarCellMetrics base = new CalendarCellMetrics(1f), large = new CalendarCellMetrics(2f);
        assertEquals(4, CalendarCellMetrics.VISIBLE_ENTRIES);
        assertTrue("Default calendar remains compact", base.rowHeight() < 190);
        assertTrue(large.rowHeight() > base.rowHeight());
        assertTrue(large.dateHeight() > base.dateHeight());
        assertTrue(large.weekdayHeight() > base.weekdayHeight());
    }
    @Test public void dateAndCaptionFitInsideSelectionAndDotRemainsBelow() {
        for (float scale : new float[]{1f, 1.3f, 2f, 3f}) {
            CalendarCellMetrics metrics = new CalendarCellMetrics(scale);
            assertEquals(18f, CalendarCellMetrics.DATE_TEXT_SP, 0f);
            assertTrue(4 + metrics.numberHeight() < metrics.lunarTop());
            assertTrue(metrics.lunarTop() + metrics.lunarHeight() <= 2 + metrics.selectionHeight());
            assertTrue(2 + metrics.selectionHeight() + 2 < metrics.dateHeight() - 2);
        }
    }
}
