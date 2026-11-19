/**
 * 可读文本颜色测试 - 测试文本颜色的可读性
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\util\ReadableTextColorsTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReadableTextColorsTest {
    @Test public void lightTextOnLightTaggedCardIsReplaced() {
        int ink = ReadableTextColors.foreground(0xffDED9AF, 0xff24211E, 0xffF1EAE1);
        assertEquals(0xff000000, ink);
        assertTrue(ReadableTextColors.contrast(ink, 0xffDED9AF) >= 4.5);
    }
    @Test public void darkUntaggedCardRetainsReadableThemeText() {
        assertEquals(0xffF1EAE1, ReadableTextColors.foreground(0xff24211E, 0xff24211E, 0xffF1EAE1));
        assertEquals(0xff2C2823, ReadableTextColors.foreground(0xffffffff, 0xffffffff, 0xff2C2823));
    }
    @Test public void translucentFillUsesComposedColor() {
        assertEquals(0xff000000, ReadableTextColors.foreground(0x80ffffff, 0xff000000, 0xffffffff));
        assertEquals(0xffffffff, ReadableTextColors.foreground(0x00000000, 0xff000000, 0xff000000));
    }
    @Test public void everyActualDayAndNightTagPaletteMeetsTextContrast() throws Exception {
        for (String mode : new String[]{"values", "values-night"}) {
            org.w3c.dom.Document palette = xml(mode, "month_task_palette.xml");
            org.w3c.dom.Document colors = xml(mode, "colors.xml");
            int surface = namedColor(colors, "color_surface");
            for (String token : new String[]{"color_text_primary", "color_text_secondary"}) {
                int preferred = namedColor(colors, token);
                org.w3c.dom.NodeList items = palette.getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    int fill = color(items.item(i).getTextContent());
                    int ink = ReadableTextColors.foreground(fill, surface, preferred);
                    assertTrue(mode + " " + token + " " + i, ReadableTextColors.contrast(ink, fill) >= 4.5);
                }
            }
        }
    }
    private org.w3c.dom.Document xml(String mode, String file) throws Exception {
        return javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.File("src/main/res/" + mode + "/" + file));
    }
    private int namedColor(org.w3c.dom.Document document, String name) {
        org.w3c.dom.NodeList nodes = document.getElementsByTagName("color");
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
            if (name.equals(element.getAttribute("name"))) return color(element.getTextContent());
        }
        throw new AssertionError(name);
    }
    private int color(String value) { return (int) (0xff000000L | Long.parseLong(value.trim().substring(1), 16)); }
}
