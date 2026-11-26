package com.pandora.backend.user;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账号输入规则校验工具类。
 * 提供实名、密码、身份证号的格式校验方法。
 * 身份证校验包含：地区码、出生日期、校验位三重验证。
 * 被注册流程和账号创建流程调用。
 */
public final class AccountInputRules {
    private AccountInputRules() {}
    // 身份证加权因子（GB 11643-1999 标准）
    private static final int[] WEIGHTS = {7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2};
    // 有效地区码集合（从资源文件加载）
    private static final Set<String> REGIONS;
    static {
        try (var input = AccountInputRules.class.getResourceAsStream("/identity/region-codes.txt")) {
            if (input == null) throw new IllegalStateException("Missing identity region codes");
            REGIONS = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .lines().filter(line -> line.matches("[0-9]{6}")).collect(Collectors.toUnmodifiableSet());
        } catch (java.io.IOException error) { throw new ExceptionInInitializerError(error); }
    }

    /**
     * 校验真实姓名。
     * 规则：1-15 个汉字（Unicode 汉字范围）。
     *
     * @param value 真实姓名
     * @return true 表示有效
     */
    public static boolean realName(String value) {
        return value != null && value.matches("\\p{IsHan}{1,15}");
    }

    /**
     * 校验密码。
     * 规则：6-16 个 Unicode 码点（支持中文等多字节字符）。
     * 学习备注: 使用 codePointCount 而非 length()，正确处理 Unicode 代理对。
     *
     * @param value 密码
     * @return true 表示有效
     */
    public static boolean password(String value) {
        if (value == null) return false;
        int length = value.codePointCount(0, value.length());
        return length >= 6 && length <= 16;
    }

    /**
     * 校验身份证号（使用当前日期）。
     * 包含：地区码、出生日期有效性、年龄限制（60岁以内）、校验位验证。
     *
     * @param value 身份证号
     * @return true 表示有效
     */
    public static boolean idCard(String value) {
        return idCard(value, LocalDate.now(ZoneId.of("Asia/Shanghai")));
    }

    /**
     * 校验身份证号（可指定日期，用于测试）。
     * 验证规则：
     * 1. 格式：18位，末位可为 X/x
     * 2. 地区码：前6位必须在有效地区码列表中
     * 3. 出生日期：15-17位不能为 000（顺序码非全零）
     * 4. 年龄限制：出生日期不能晚于今天，且60岁以内
     * 5. 校验位：加权求和模11验证
     *
     * @param value 身份证号
     * @param today 当前日期（用于测试）
     * @return true 表示有效
     */
    static boolean idCard(String value, LocalDate today) {
        if (value == null || !value.matches("[0-9]{17}[0-9Xx]")
                || !REGIONS.contains(value.substring(0, 6)) || "000".equals(value.substring(14, 17))) return false;
        try {
            LocalDate birth = LocalDate.parse(value.substring(6, 14), DateTimeFormatter.BASIC_ISO_DATE);
            if (birth.isAfter(today) || !birth.plusYears(60).isAfter(today)) return false;
        } catch (java.time.DateTimeException error) { return false; }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) sum += (value.charAt(i) - '0') * WEIGHTS[i];
        return Character.toUpperCase(value.charAt(17)) == "10X98765432".charAt(sum % 11);
    }
}