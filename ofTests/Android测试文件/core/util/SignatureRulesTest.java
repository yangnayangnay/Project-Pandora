package com.example.project_pandora.core.util;

/**
 * SignatureRules 单元测试 - 签名规则模块
 * 
 * 测试目标：验证签名规则
 * 
 * 测试范围：
 * - 正常场景：换行符一致性
 * - 边界场景：空行与尾行计数
 * 
 * 测试用例：2个
 */


import org.junit.Test;
import static org.junit.Assert.*;

    /**
     * 测试用例：空行与尾行计数
     * 输入类别：边界（边界值分析）
     * 测试步骤：空行和尾行计数
     * 预期结果：正确计数
     */
    @Test public void countsBlankAndTrailingLines() {
        assertEquals(1, SignatureRules.explicitLines(null));
        assertEquals(4, SignatureRules.explicitLines("一\n二\n三\n四"));
        assertEquals(4, SignatureRules.explicitLines("\n\n\n"));
        assertEquals(5, SignatureRules.explicitLines("一\n\n\n\n"));
    /**
     * 测试用例：换行符一致性
     * 输入类别：同类（有效等价类）
     * 测试步骤：粘贴的Windows和Unicode换行符
     * 预期结果：统一含义
     */
    @Test public void pastedWindowsAndUnicodeBreaksHaveConsistentMeaning() {
        assertEquals("一\n二\n三\n四", SignatureRules.normalize("一\r\n二\r三\u2028四"));
        assertEquals(5, SignatureRules.explicitLines("一\u2029二\u0085三\r四\n五"));
    }
}
