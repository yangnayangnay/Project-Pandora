package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志内容规则 [V2.0 D-05] —— 原 2 条保留,新增 9 条,共 11 条
 *
 * 用例设计方法:
 *  - 等价类划分(requireBody):
 *      有效类 = 含至少一个可见字符的字符串(保留用户原始格式,不 trim)
 *      无效类 = null / 非字符串类型 / 纯不可见内容
 *             (空格、换行、制表符、不间断空格 \u00a0、全角空格 \u3000、换页 \f 等)
 *  - 边界值分析:
 *      title       = 1月1日(月日下界) / 12月31日(月日上界) / 2月28日(平年二月末) / 2月29日(闰日)
 *      requireBody = 最小有效正文 = 1 个可见字符
 */
class LogContentRulesTest {

    // ==================== 原有用例(保留) ====================

    @Test
    void titleIsCanonicalAndUsesSelectedDay() {
        assertEquals("1月2日日志", LogContentRules.title("2026-01-02"));
        assertEquals("11月13日日志", LogContentRules.title("2026-11-13"));
    }

    @Test
    void bodyMustContainVisibleContentAndKeepsUserFormatting() {
        for (Object value : new Object[]{null, 1, "", " \n\t", " ", "\u00a0"})
            assertThrows(BusinessException.class, () -> LogContentRules.requireBody(value));
        assertEquals("\n 完成联调\n", LogContentRules.requireBody("\n 完成联调\n"));
    }

    // ==================== 新增:title 边界值 ====================

    /** 边界下界:最小月日,前导零必须去掉 */
    @Test
    void titleDropsLeadingZerosOnSmallestMonthAndDay() {
        assertEquals("1月1日日志", LogContentRules.title("2026-01-01"));
    }

    /** 边界上界:最大月日,两位数保持原样 */
    @Test
    void titleKeepsTwoDigitMonthAndDayAtUpperBound() {
        assertEquals("12月31日日志", LogContentRules.title("2026-12-31"));
    }

    /** 特殊边界:平年二月末 2月28日 */
    @Test
    void titleSupportsNonLeapFebruaryEnd() {
        assertEquals("2月28日日志", LogContentRules.title("2026-02-28"));
    }

    /** 特殊边界:闰日 2月29日 是合法选中日期 */
    @Test
    void titleSupportsLeapDay() {
        assertEquals("2月29日日志", LogContentRules.title("2028-02-29"));
    }

    // ==================== 新增:title 异常输入 ====================
    // 这两条用 Exception 兜底断言:无论内部是 LocalDate.parse 抛
    // DateTimeParseException、手工解析抛 NPE/NumberFormatException、
    // 还是包装成 BusinessException,非法日期都必须抛异常而非静默返回。

    /** 异常输入:null 日期必须抛异常 */
    @Test
    void titleRejectsNullDate() {
        assertThrows(Exception.class, () -> LogContentRules.title(null));
    }

    /** 异常输入:格式非法的日期必须抛异常 */
    @Test
    void titleRejectsMalformedDate() {
        assertThrows(Exception.class, () -> LogContentRules.title("not-a-date"));
    }

    // ==================== 新增:requireBody 边界与等价类 ====================

    /** 边界:最小有效正文 = 1 个可见字符,必须被接受且原样返回 */
    @Test
    void bodyAcceptsMinimalSingleVisibleCharacter() {
        assertEquals("a", LogContentRules.requireBody("a"));
        assertEquals("完", LogContentRules.requireBody("完"));
    }

    /** 等价类扩展:全角空格/换页/垂直制表/回车等其余不可见内容同样拒绝 */
    @Test
    void bodyRejectsRemainingUnicodeWhitespaceOnlyValues() {
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\u3000"));
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\f"));
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\u000b"));
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\r"));
    }

    /** 格式保留强化:首尾空白 + 中间可见内容 → 原样返回,不做 trim */
    @Test
    void bodyKeepsFormattingWithBlankEdgesAndVisibleCenter() {
        assertEquals(" \n\t完成联调\t\n ", LogContentRules.requireBody(" \n\t完成联调\t\n "));
    }
}
