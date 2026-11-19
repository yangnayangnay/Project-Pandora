package com.example.project_pandora.core.util;

/**
 * LogEntryRules 单元测试 - 日志条目规则模块
 * 
 * 测试目标：验证日志条目规则
 * 
 * 测试范围：
 * - 正常/边界场景：无前导零
 * - 异常场景：拒空白保换行
 * 
 * 测试用例：2个
 */


import org.junit.Test;
import static org.junit.Assert.*;

    /**
     * 测试用例：无前导零
     * 输入类别：边界（边界值分析）
     * 测试步骤：标题使用所选日期无前导零
     * 预期结果：正确显示
     */
    @Test public void titleUsesTheSelectedDayWithoutLeadingZeros() {
        assertEquals("1月2日日志", LogEntryRules.title("2026-01-02"));
        assertEquals("11月13日日志", LogEntryRules.title("2026-11-13"));
    /**
     * 测试用例：拒空白/保换行
     * 输入类别：正+异（正常+异常）
     * 测试步骤：正文拒绝所有空白但保留多行内容
     * 预期结果：正确处理
     */
    @Test public void bodyRejectsAllWhitespaceButPreservesMultilineContent() {
        assertFalse(LogEntryRules.hasBody(null)); assertFalse(LogEntryRules.hasBody(" \n\t　\u00a0"));
        assertTrue(LogEntryRules.hasBody("\n完成联调\n"));
    }
}
