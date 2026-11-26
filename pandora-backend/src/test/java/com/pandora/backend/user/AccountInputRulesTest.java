package com.pandora.backend.user;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 账号输入规则测试——验证真实姓名、密码、身份证号的格式校验。
 * 覆盖正常值、边界值、异常值三种测试场景。
 */
class AccountInputRulesTest {

    /**
     * 生成有效身份证号（计算校验位）。
     * 使用 GB 11643-1999 标准的加权算法。
     */
    static String identity(String prefix) {
        int[] weights = {7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2};
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += (prefix.charAt(i) - '0') * weights[i];
        return prefix + "10X98765432".charAt(sum % 11);
    }

    /**
     * 测试姓名和密码的精确边界。
     * 验证：
     * - 姓名：1-15 个汉字
     * - 密码：6-16 个 Unicode 码点
     */
    @Test
    void namesAndPasswordsHaveExactCharacterBoundaries() {
        // 正常值：有效姓名
        for (String name : new String[]{"吴六", "汉".repeat(15), "𠮷野"})
            assertTrue(AccountInputRules.realName(name));
        // 异常值：无效姓名
        for (String name : new String[]{"", "吴 六", "Alice", "陈十1", "汉".repeat(16), "吴·六"})
            assertFalse(AccountInputRules.realName(name));
        // 正常值：有效密码
        for (String value : new String[]{"123456", "a".repeat(16), "密".repeat(16), "😀".repeat(16)})
            assertTrue(AccountInputRules.password(value));
        // 异常值：无效密码
        for (String value : new String[]{"", "12345", "a".repeat(17), "😀".repeat(17)})
            assertFalse(AccountInputRules.password(value));
    }

    /**
     * 测试身份证号的每一段校验和精确退休日期。
     * 验证：
     * - 地区码：前6位必须在有效列表中
     * - 出生日期：8位有效日期
     * - 年龄限制：60岁以内
     * - 校验位：加权求和模11
     */
    @Test
    void identityChecksEverySectionAndExactRetirementDay() {
        var today = LocalDate.of(2026, 11, 23);
        // 正常值：有效身份证号
        for (String prefix : new String[]{"11010119900101001", "11010319900101001", "11011519900101001", "11010119661124001", "11010120000229001"})
            assertTrue(AccountInputRules.idCard(identity(prefix), today), prefix);
        // 异常值：无效身份证号
        for (String prefix : new String[]{"11999919900101001", "11000019900101001", "11010119661123001", "11010119661122001", "11010120271123001", "11010120010229001", "11010119900101000"})
            assertFalse(AccountInputRules.idCard(identity(prefix), today), prefix);
        // 异常值：校验位错误
        assertFalse(AccountInputRules.idCard("11010119900101001X", today));
        // 正常值：小写 x 也可接受
        assertTrue(AccountInputRules.idCard(identity("11010119900203001").toLowerCase(), today));
    }
}