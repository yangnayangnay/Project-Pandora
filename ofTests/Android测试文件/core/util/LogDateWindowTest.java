package com.example.project_pandora.core.util;

/**
 * LogDateWindow 单元测试 - 日志日期窗口模块
 * 
 * 测试目标：验证日志日期窗口（与后端 LogDatePolicy 镜像）
 * 
 * 测试范围：
 * - 正常场景：前两天至今的窗口定义
 * - 边界场景：月界、年界、平年2月、闰日、午夜切换
 * - 异常场景：非法日期
 * 
 * 测试用例：8个
 */


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
    /**
     * 测试用例：窗口定义
     * 输入类别：同类（有效等价类）
     * 测试步骤：验证前两天至今的日期窗口
     * 预期结果：窗口内日期可写
     */
    @Test public void acceptsPreviousTwoDaysThroughToday() {
        window("2026-10-27T16:01:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    /**
     * 测试用例：午夜前
     * 输入类别：边界（边界值分析）
     * 测试步骤：午夜前使用前一个工作日
     * 预期结果：正确处理
     */
    @Test public void beforeShanghaiMidnightUsesPreviousBusinessDay() {
        window("2026-10-27T15:59:59Z", "2026-10-25", "2026-10-26", "2026-10-27", "2026-10-24", "2026-10-28");
    /**
     * 测试用例：午夜切换
     * 输入类别：边界（边界值分析）
     * 测试步骤：上海时区午夜切换
     * 预期结果：正确切换
     */
    @Test public void switchesAtShanghaiMidnight() {
        window("2026-10-27T16:00:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    /**
     * 测试用例：月界边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：处理月份边界
     * 预期结果：正确处理
     */
    @Test public void handlesMonthBoundary() {
        window("2026-10-31T16:01:00Z", "2026-10-30", "2026-10-31", "2026-11-01", "2026-10-29", "2026-11-02");
    /**
     * 测试用例：年界边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：处理年份边界
     * 预期结果：正确处理
     */
    @Test public void handlesYearBoundary() {
        window("2026-12-31T16:01:00Z", "2026-12-30", "2026-12-31", "2027-01-01", "2026-12-29", "2027-01-02");
    /**
     * 测试用例：非法日期
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入格式错误的日期
     * 预期结果：被拒绝
     */
    @Test public void rejectsMalformedDates() {
        Date now = Date.from(Instant.parse("2026-10-27T16:01:00Z"));
        for (String date : new String[]{null, "", "2026/10/28", "2026-1-1", "20261028", "2026-02-30"})
            assertFalse(LogDateWindow.isWritable(date, now));
    }

    /**
     * 测试用例：闰日边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：闰日边界
     * 预期结果：正确处理
     */
    @Test public void handlesLeapDayBoundary() {
        // 2028年是闰年，2月29日有效
        window("2028-03-01T16:01:00Z", "2028-02-29", "2028-03-01", "2028-03-02", "2028-02-28", "2028-03-03");
    }
    /**
     * 测试用例：平年2月边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：平年2月
     * 预期结果：正确处理
     */
    @Test public void handlesNonLeapFebruary() {
        // 2027年不是闰年，2月28日是最后一天
        window("2027-03-01T16:01:00Z", "2027-02-28", "2027-03-01", "2027-03-02", "2027-02-27", "2027-03-03");
    }
}
