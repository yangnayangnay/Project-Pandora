/**
 * 日志日期窗口测试 - 测试前两天至今的日期窗口、月界/年界边界、午夜切换、非法日期等
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\util\LogDateWindowTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.util;

import org.junit.Test;
import java.time.Instant;
import java.util.Date;
import static org.junit.Assert.*;

public class LogDateWindowTest {
    private void window(String instant, String first, String yesterday, String today, String rejectedBefore, String tomorrow) {
        Date now = Date.from(Instant.parse(instant));
        assertEquals(first, LogDateWindow.daysAgo(2, now));
        assertEquals(yesterday, LogDateWindow.daysAgo(1, now));
        assertEquals(today, LogDateWindow.daysAgo(0, now));
        for (String date : new String[]{first, yesterday, today}) assertTrue(date, LogDateWindow.isWritable(date, now));
        assertFalse(LogDateWindow.isWritable(rejectedBefore, now));
        assertFalse(LogDateWindow.isWritable(tomorrow, now));
    }
    @Test public void acceptsPreviousTwoDaysThroughToday() {
        window("2026-10-27T16:01:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    }
    @Test public void beforeShanghaiMidnightUsesPreviousBusinessDay() {
        window("2026-10-27T15:59:59Z", "2026-10-25", "2026-10-26", "2026-10-27", "2026-10-24", "2026-10-28");
    }
    @Test public void switchesAtShanghaiMidnight() {
        window("2026-10-27T16:00:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    }
    @Test public void handlesMonthBoundary() {
        window("2026-10-31T16:01:00Z", "2026-10-30", "2026-10-31", "2026-11-01", "2026-10-29", "2026-11-02");
    }
    @Test public void handlesYearBoundary() {
        window("2026-12-31T16:01:00Z", "2026-12-30", "2026-12-31", "2027-01-01", "2026-12-29", "2027-01-02");
    }
    @Test public void rejectsMalformedDates() {
        Date now = Date.from(Instant.parse("2026-10-27T16:01:00Z"));
        for (String date : new String[]{null, "", "2026/10/28", "2026-1-1", "20261028", "2026-02-30"})
            assertFalse(LogDateWindow.isWritable(date, now));
    }

    // ============ V2.0 增量用例（闰日边界） ============
    @Test public void handlesLeapDayBoundary() {
        // 2028年是闰年，2月29日有效
        window("2028-03-01T16:01:00Z", "2028-02-29", "2028-03-01", "2028-03-02", "2028-02-28", "2028-03-03");
    }

    @Test public void handlesNonLeapFebruary() {
        // 2027年不是闰年，2月28日是最后一天
        window("2027-03-01T16:01:00Z", "2027-02-28", "2027-03-01", "2027-03-02", "2027-02-27", "2027-03-03");
    }
}
