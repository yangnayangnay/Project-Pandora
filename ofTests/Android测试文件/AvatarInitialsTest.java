/**
 * 头像首字母测试 - 测试头像首字母的生成逻辑
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/ui\mine\AvatarInitialsTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.ui.mine;

import org.junit.Test;
import static org.junit.Assert.*;

public class AvatarInitialsTest {
    @Test public void keepsLastTwoNameCharactersAndSupportsShortNames() {
        assertEquals("小明", AvatarInitials.fromName("王小明"));
        assertEquals("娜娜", AvatarInitials.fromName(" 欧阳娜娜 "));
        assertEquals("王", AvatarInitials.fromName("王"));
        assertEquals("01", AvatarInitials.fromName("000001"));
    }
    @Test public void emptyNamesHaveAFallbackAndUnicodeCharactersAreNotSplit() {
        assertEquals("用户", AvatarInitials.fromName(null));
        assertEquals("用户", AvatarInitials.fromName("  "));
        assertEquals("𠮷明", AvatarInitials.fromName("王𠮷明"));
    }
}
