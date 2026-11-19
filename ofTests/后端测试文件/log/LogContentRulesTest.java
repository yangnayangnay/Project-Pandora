package com.pandora.backend.log;

/**
 * LogContentRules 单元测试 - 日志内容规则模块
 * 
 * 测试目标：验证日志内容规则校验
 * 
 * 测试范围：
 * - 正常场景：标题规范、正文保留格式
 * - 边界场景：1月1日/12月31日/2月28日/2月29日日期边界、单字符正文
 * - 异常场景：null日期、非法日期、全角空格等空白字符
 * 
 * 测试用例：11个
 */


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

    /**
     * 测试用例：标题规范
     * 输入类别：同类（有效等价类）
     * 测试步骤：生成规范格式的标题
     * 预期结果：标题使用规范格式和所选日期
     */
    void titleIsCanonicalAndUsesSelectedDay() {
        assertEquals("1月2日日志", LogContentRules.title("2026-01-02"));
        assertEquals("11月13日日志", LogContentRules.title("2026-11-13"));
    }

    /**
     * 测试用例：正文可见内容
     * 输入类别：正+异（正常+异常）
     * 测试步骤：校验正文包含可见内容
     * 预期结果：保留用户格式
     */
    void bodyMustContainVisibleContentAndKeepsUserFormatting() {
        for (Object value : new Object[]{null, 1, "", " \n\t", " ", "\u00a0"})
            assertThrows(BusinessException.class, () -> LogContentRules.requireBody(value));
        assertEquals("\n 完成联调\n", LogContentRules.requireBody("\n 完成联调\n"));
    }

    // ==================== 新增:title 边界值 ====================

    /** 边界下界:最小月日,前导零必须去掉 */
    /**
     * 测试用例：1月1日边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：最小日期(1月1日)去掉前导零
     * 预期结果：标题正确显示
     */
    void titleDropsLeadingZerosOnSmallestMonthAndDay() {
        assertEquals("1月1日日志", LogContentRules.title("2026-01-01"));
    }

    /** 边界上界:最大月日,两位数保持原样 */
    /**
     * 测试用例：12月31日边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：最大日期(12月31日)保留两位数
     * 预期结果：标题正确显示
     */
    void titleKeepsTwoDigitMonthAndDayAtUpperBound() {
        assertEquals("12月31日日志", LogContentRules.title("2026-12-31"));
    }

    /** 特殊边界:平年二月末 2月28日 */
    /**
     * 测试用例：2月28日边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：平年2月28日
     * 预期结果：标题正确显示
     */
    void titleSupportsNonLeapFebruaryEnd() {
        assertEquals("2月28日日志", LogContentRules.title("2026-02-28"));
    }

    /** 特殊边界:闰日 2月29日 是合法选中日期 */
    /**
     * 测试用例：2月29日边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：闰年2月29日
     * 预期结果：标题正确显示
     */
    void titleSupportsLeapDay() {
        assertEquals("2月29日日志", LogContentRules.title("2028-02-29"));
    }

    // ==================== 新增:title 异常输入 ====================
    // 这两条用 Exception 兜底断言:无论内部是 LocalDate.parse 抛
    // DateTimeParseException、手工解析抛 NPE/NumberFormatException、
    // 还是包装成 BusinessException,非法日期都必须抛异常而非静默返回。

    /** 异常输入:null 日期必须抛异常 */
    /**
     * 测试用例：null日期
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入 null 日期
     * 预期结果：被拒绝
     */
    void titleRejectsNullDate() {
        assertThrows(Exception.class, () -> LogContentRules.title(null));
    }

    /** 异常输入:格式非法的日期必须抛异常 */
    /**
     * 测试用例：非法日期
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入格式错误的日期
     * 预期结果：被拒绝
     */
    void titleRejectsMalformedDate() {
        assertThrows(Exception.class, () -> LogContentRules.title("not-a-date"));
    }

    // ==================== 新增:requireBody 边界与等价类 ====================

    /** 边界:最小有效正文 = 1 个可见字符,必须被接受且原样返回 */
    /**
     * 测试用例：单字符正文边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：最小可见正文(单字符)
     * 预期结果：被接受
     */
    void bodyAcceptsMinimalSingleVisibleCharacter() {
        assertEquals("a", LogContentRules.requireBody("a"));
        assertEquals("完", LogContentRules.requireBody("完"));
    }

    /** 等价类扩展:全角空格/换页/垂直制表/回车等其余不可见内容同样拒绝 */
    /**
     * 测试用例：全角空格等空白
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入纯Unicode空白字符
     * 预期结果：被拒绝
     */
    void bodyRejectsRemainingUnicodeWhitespaceOnlyValues() {
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\u3000"));
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\f"));
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\u000b"));
        assertThrows(BusinessException.class, () -> LogContentRules.requireBody("\r"));
    }

    /** 格式保留强化:首尾空白 + 中间可见内容 → 原样返回,不做 trim */
    /**
     * 测试用例：正文保留格式
     * 输入类别：同类（有效等价类）
     * 测试步骤：正文包含首尾空白和中间可见内容
     * 预期结果：原样返回
     */
    void bodyKeepsFormattingWithBlankEdgesAndVisibleCenter() {
        assertEquals(" \n\t完成联调\t\n ", LogContentRules.requireBody(" \n\t完成联调\t\n "));
    }
}
