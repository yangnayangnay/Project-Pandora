package com.example.project_pandora.core.util;

/**
 * TaskDateTimes 单元测试 - 任务日期时间处理模块
 * 
 * 测试目标：验证任务日期时间处理
 * 
 * 测试范围：
 * - 正常场景：跨天区间、时区固定、格式化
 * - 边界场景：相等拒绝/下一分钟接受、仅日期→当日始末、24点非法
 * - 异常场景：起止倒置、null回退、非法日历日期、尾随字符、过短串
 * 
 * 测试用例：11个
 */


import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Contract tests for task picker datetime text: minute precision, strict parsing and draft restore. */
public class TaskDateTimesTest {

    /**
     * 测试用例：保留时分
     * 输入类别：同类（有效等价类）
     * 测试步骤：格式化日期时间
     * 预期结果：保留小时和分钟
     */
    public void formatKeepsHourAndMinute() {
        Date morning = TaskDateTimes.parse("2024-05-06 07:05");
        Date lateNight = TaskDateTimes.parse("2024-05-06 23:59");

        assertNotNull(morning);
        assertNotNull(lateNight);
        assertEquals("2024-05-06 07:05", TaskDateTimes.format(morning));
        assertEquals("2024-05-06 23:59", TaskDateTimes.format(lateNight));
    }

    /**
     * 测试用例：非法日历日期
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入"2024-02-30"等非法日期
     * 预期结果：返回 null
     */
    public void parseRejectsIllegalCalendarDates() {
        assertNull(TaskDateTimes.parse("2024-02-30 10:00"));
        assertNull(TaskDateTimes.parse("2024-13-01 10:00"));
        assertNull(TaskDateTimes.parse("2024-04-31 10:00"));
    }

    /**
     * 测试用例：24点非法边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：传入"2024-01-01 24:00"
     * 预期结果：返回 null
     */
    public void parseRejects24Hour() {
        assertNull(TaskDateTimes.parse("2024-01-01 24:00"));
    }

    /**
     * 测试用例：过短串
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入过短字符串
     * 预期结果：返回 null
     */
    public void parseRejectsShortStrings() {
        assertNull(TaskDateTimes.parse("2024-01-01"));
        assertNull(TaskDateTimes.parse("2024-01-01 1:00"));
        assertNull(TaskDateTimes.parse("2024-1-01 10:00"));
    }

    /**
     * 测试用例：尾随字符
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入带尾随字符的字符串
     * 预期结果：返回 null
     */
    public void parseRejectsTrailingCharacters() {
        assertNull(TaskDateTimes.parse("2024-01-01 10:00x"));
        assertNull(TaskDateTimes.parse("2024-01-01 10:00:00"));
        assertNull(TaskDateTimes.parse("2024-01-01 10:00 "));
    }

    /**
     * 测试用例：起止倒置
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：结束时间早于开始时间
     * 预期结果：返回 false
     */
    public void validRangeRejectsReversedStartAndEnd() {
        assertFalse(TaskDateTimes.validRange("2024-01-02 10:00", "2024-01-02 09:00"));
    }

    /**
     * 测试用例：跨天区间
     * 输入类别：同类（有效等价类）
     * 测试步骤：起止时间跨天
     * 预期结果：返回 true
     */
    public void validRangeAcceptsCrossDayRange() {
        assertTrue(TaskDateTimes.validRange("2024-12-31 22:00", "2025-01-01 02:00"));
    }

    /**
     * 测试用例：相等拒绝/下一分钟接受
     * 输入类别：边界（边界值分析）
     * 测试步骤：起止相等和下一分钟分别测试
     * 预期结果：相等拒绝，下一分钟接受
     */
    public void validRangeRejectsEqualStartAndEndAndAcceptsNextMinute() {
        assertFalse(TaskDateTimes.validRange("2024-03-01 08:30", "2024-03-01 08:30"));
        assertTrue(TaskDateTimes.validRange("2024-03-01 08:30", "2024-03-01 08:31"));
    }

    /**
     * 测试用例：仅日期→当日始末
     * 输入类别：边界（边界值分析）
     * 测试步骤：仅日期草稿恢复
     * 预期结果：转为当日始末
     */
    public void restoreDateOnlyDraftUsesStartAndEndOfDay() {
        assertEquals("2024-05-06 00:00", TaskDateTimes.restore("2024-05-06", false));
        assertEquals("2024-05-06 23:59", TaskDateTimes.restore("2024-05-06", true));
    }

    /**
     * 测试用例：null回退
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入 null
     * 预期结果：返回 null
     */
    public void restoreNullReturnsNull() {
        assertNull(TaskDateTimes.restore(null, false));
        assertNull(TaskDateTimes.restore(null, true));
        assertNull(TaskDateTimes.restore("", false));
    }

    /**
     * 测试用例：时区固定
     * 输入类别：同类（有效等价类）
     * 测试步骤：ISO解析使用固定上海时区
     * 预期结果：正确转换
     */
    public void restoreIsoWithOffsetUsesFixedShanghaiTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));

            assertEquals("2024-05-06 08:30",
                    TaskDateTimes.restore("2024-05-06T00:30:00.000+00:00", false));
            assertEquals("2024-05-06 04:30",
                    TaskDateTimes.restore("2024-05-05T20:30:00+00:00", false));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
