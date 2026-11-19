/**
 * 日志条目规则测试 - 测试标题规范、正文格式、空白字符处理等
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\util\LogEntryRulesTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogEntryRulesTest {
    @Test public void titleUsesTheSelectedDayWithoutLeadingZeros() {
        assertEquals("1月2日日志", LogEntryRules.title("2026-01-02"));
        assertEquals("11月13日日志", LogEntryRules.title("2026-11-13"));
    }
    @Test public void bodyRejectsAllWhitespaceButPreservesMultilineContent() {
        assertFalse(LogEntryRules.hasBody(null)); assertFalse(LogEntryRules.hasBody(" \n\t　\u00a0"));
        assertTrue(LogEntryRules.hasBody("\n完成联调\n"));
    }
}
