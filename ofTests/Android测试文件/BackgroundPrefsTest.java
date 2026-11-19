/**
 * 背景偏好设置测试 - 测试背景偏好的保存和读取
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\config\BackgroundPrefsTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackgroundPrefsTest {
    @Test public void existingBackgroundChoicesKeepTheirStoredMeaningAndCuteIsAThirdChoice() {
        assertEquals(BackgroundPrefs.SOLID, BackgroundPrefs.normalizeStyle(0));
        assertEquals(BackgroundPrefs.COMPANY, BackgroundPrefs.normalizeStyle(1));
        assertEquals(BackgroundPrefs.CUTE, BackgroundPrefs.normalizeStyle(2));
        assertEquals(BackgroundPrefs.COMPANY, BackgroundPrefs.normalizeStyle(-1));
        assertEquals(BackgroundPrefs.COMPANY, BackgroundPrefs.normalizeStyle(99));
    }
}
