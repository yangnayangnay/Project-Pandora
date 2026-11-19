/**
 * 月度任务日期测试 - 测试月度任务的日期处理
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/ui\widget\MonthTaskDatesTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.ui.widget;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pure-JVM boundary tests for month-grid date coverage; no Android classes are used. */
public class MonthTaskDatesTest {
    private static final String DAY = "2026-10-27";

    @Test
    public void sameDayTimedTaskIsIncludedOnItsDay() {
        assertTrue(MonthTaskDates.covers("2026-10-27 09:00", "2026-10-27 10:00", DAY));
    }

    @Test
    public void nextDayOutsideTimedTaskIsExcluded() {
        assertFalse(MonthTaskDates.covers("2026-10-27 09:00", "2026-10-27 10:00", "2026-10-28"));
    }

    @Test
    public void crossDayTimedTaskIsIncludedOnBothCalendarDays() {
        String start = "2026-10-27 23:00";
        String end = "2026-10-28 01:00";
        assertTrue(MonthTaskDates.covers(start, end, "2026-10-27"));
        assertTrue(MonthTaskDates.covers(start, end, "2026-10-28"));
    }

    @Test
    public void timedMidnightEndIsExcludedOnEndDay() {
        String start = "2026-10-27 22:00";
        String end = "2026-10-28 00:00";
        assertTrue(MonthTaskDates.covers(start, end, "2026-10-27"));
        assertFalse(MonthTaskDates.covers(start, end, "2026-10-28"));
    }

    @Test
    public void dateOnlyEndDayIsInclusive() {
        assertTrue(MonthTaskDates.covers("2026-10-27", "2026-10-29", "2026-10-29"));
        assertFalse(MonthTaskDates.covers("2026-10-27", "2026-10-29", "2026-10-30"));
    }

    @Test
    public void missingStartFallsBackToEndDate() {
        assertTrue(MonthTaskDates.covers(null, "2026-10-27 10:00", DAY));
        assertFalse(MonthTaskDates.covers(null, "2026-10-27 10:00", "2026-10-28"));
    }

    @Test
    public void undatedTaskIsExcludedFromSelectedDayList() {
        assertFalse(MonthTaskDates.covers(null, null, DAY));
        assertFalse(MonthTaskDates.covers("", "", DAY));
    }

    @Test
    public void invertedEndIsTreatedAsStartDateOnly() {
        String start = "2026-10-27 09:00";
        String end = "2026-10-25 10:00";
        assertTrue(MonthTaskDates.covers(start, end, DAY));
        assertFalse(MonthTaskDates.covers(start, end, "2026-10-25"));
    }
}
