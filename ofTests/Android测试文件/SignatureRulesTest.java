/**
 * 签名规则测试 - 测试换行符一致性、空行与尾行计数等
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\util\SignatureRulesTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class SignatureRulesTest {
    @Test public void countsBlankAndTrailingLines() {
        assertEquals(1, SignatureRules.explicitLines(null));
        assertEquals(4, SignatureRules.explicitLines("一\n二\n三\n四"));
        assertEquals(4, SignatureRules.explicitLines("\n\n\n"));
        assertEquals(5, SignatureRules.explicitLines("一\n\n\n\n"));
    }
    @Test public void pastedWindowsAndUnicodeBreaksHaveConsistentMeaning() {
        assertEquals("一\n二\n三\n四", SignatureRules.normalize("一\r\n二\r三\u2028四"));
        assertEquals(5, SignatureRules.explicitLines("一\u2029二\u0085三\r四\n五"));
    }
}
