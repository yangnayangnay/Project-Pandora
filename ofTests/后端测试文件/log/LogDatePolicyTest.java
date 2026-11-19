package com.pandora.backend.log;

/**
 * LogDatePolicy 单元测试 - 日志日期策略模块
 * 
 * 测试目标：验证日志日期窗口策略
 * 
 * 测试范围：
 * - 正常场景：前两天至今的窗口定义
 * - 边界场景：月界、年界、午夜切换、午夜前
 * - 异常场景：非法日期格式
 * 
 * 前置条件：使用 Clock.fixed 固定测试时间
 * 测试用例：6个
 */


import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LogDatePolicyTest {
    private void window(String instant, String first, String yesterday, String today, String rejectedBefore, String tomorrow) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        for (String date : new String[]{first, yesterday, today}) assertEquals(date, LogDatePolicy.requireWritable(date, clock));
        BusinessException error = assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(rejectedBefore, clock));
        assertEquals("仅可填写或编辑前天、昨天、今天的日志", error.getMessage());
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(tomorrow, clock));
    /**
     * 测试用例：窗口定义
     * 输入类别：同类（有效等价类）
     * 测试步骤：验证前两天至今的日期窗口
     * 预期结果：窗口内日期可写
     */
    @Test void acceptsPreviousTwoDaysThroughToday() {
        window("2026-10-27T16:01:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    /**
     * 测试用例：午夜前边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：上海时区午夜前验证
     * 预期结果：使用前一个工作日
     */
    @Test void beforeShanghaiMidnightUsesPreviousBusinessDay() {
        window("2026-10-27T15:59:59Z", "2026-10-25", "2026-10-26", "2026-10-27", "2026-10-24", "2026-10-28");
    /**
     * 测试用例：午夜切换边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：上海时区午夜切换点验证
     * 预期结果：正确切换
     */
    @Test void switchesAtShanghaiMidnight() {
        window("2026-10-27T16:00:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    /**
     * 测试用例：月界边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：处理月份边界
     * 预期结果：正确处理
     */
    @Test void handlesMonthBoundary() {
        window("2026-10-31T16:01:00Z", "2026-10-30", "2026-10-31", "2026-11-01", "2026-10-29", "2026-11-02");
    /**
     * 测试用例：年界边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：处理年份边界
     * 预期结果：正确处理
     */
    @Test void handlesYearBoundary() {
        window("2026-12-31T16:01:00Z", "2026-12-30", "2026-12-31", "2027-01-01", "2026-12-29", "2027-01-02");
    /**
     * 测试用例：非法日期
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入格式错误的日期
     * 预期结果：被拒绝
     */
    @Test void rejectsMalformedDates() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T16:01:00Z"), ZoneOffset.UTC);
        for (String date : new String[]{null, "", "2026/10/28", "2026-1-1", "20261028", "2026-02-30"})
            assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(date, clock));
    }
}
