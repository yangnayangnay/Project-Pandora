package com.example.project_pandora.core.util;

/**
 * NumberCompat 单元测试 - 数字兼容性转换模块
 * 
 * 测试目标：验证数字类型兼容性转换
 * 
 * 测试范围：
 * - 正常场景：纯数字串、负数、小数、各种实例类型转换
 * - 边界场景：科学计数法、超范围溢出、0/非0判定
 * - 异常场景：null回退、空串、非数字、其他类型
 * 
 * 测试用例：18个
 */


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Round26 REQ-26-02：NumberCompat 数字安全解析单测。
 * 覆盖 spec 4.2-2 全部输入形态：null、空串、"5.0"、"-3.0"、Double、Integer、Long、
 * 纯数字串、非数字串、科学计数法、超范围大数；toBool 的 Boolean/Number/其他三形态。
 * 任何输入组合均不抛异常（异常即用例失败）。
 */
public class NumberCompatTest {

    // ---- toLong：11 形态 ----

    /**
     * 测试用例：null
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入 null
     * 预期结果：返回 null
     */
    public void toLong_null_returnsNull() {
        assertNull(NumberCompat.toLong(null));
    }

    /**
     * 测试用例：空串
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入空串或空白
     * 预期结果：返回 null
     */
    public void toLong_emptyString_returnsNull() {
        assertNull(NumberCompat.toLong(""));
        assertNull(NumberCompat.toLong("   "));
    }

    /**
     * 测试用例：小数串
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入"5.0"
     * 预期结果：返回 5L
     */
    public void toLong_doubleString_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong("5.0"));
    }

    /**
     * 测试用例：负数串
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入"-3.0"
     * 预期结果：返回 -3L
     */
    public void toLong_negativeDoubleString_returnsValue() {
        assertEquals(Long.valueOf(-3L), NumberCompat.toLong("-3.0"));
    }

    /**
     * 测试用例：Double实例
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入 Double 实例
     * 预期结果：转换成功
     */
    public void toLong_doubleInstance_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong(Double.valueOf(5.0)));
        assertEquals(Long.valueOf(-3L), NumberCompat.toLong(Double.valueOf(-3.7)));
    }

    /**
     * 测试用例：Integer实例
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入 Integer 实例
     * 预期结果：转换成功
     */
    public void toLong_integerInstance_returnsValue() {
        assertEquals(Integer.valueOf(5).longValue(), NumberCompat.toLong(Integer.valueOf(5)).longValue());
    }

    /**
     * 测试用例：Long实例
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入 Long.MAX_VALUE
     * 预期结果：返回 Long.MAX_VALUE
     */
    public void toLong_longInstance_returnsValue() {
        assertEquals(Long.valueOf(9223372036854775807L), NumberCompat.toLong(Long.MAX_VALUE));
    }

    /**
     * 测试用例：纯数字串
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入"5"或"-3"
     * 预期结果：返回 5L 或 -3L
     */
    public void toLong_plainDigitString_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong("5"));
        assertEquals(Long.valueOf(-3L), NumberCompat.toLong("-3"));
    }

    /**
     * 测试用例：非数字
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入"abc"或"12x3"
     * 预期结果：返回 null
     */
    public void toLong_nonNumericString_returnsNull() {
        assertNull(NumberCompat.toLong("abc"));
        assertNull(NumberCompat.toLong("12x3"));
    }

    /**
     * 测试用例：科学计数法边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：传入"5E0"或"5E3"
     * 预期结果：返回 5L 或 5000L
     */
    public void toLong_scientificNotation_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong("5E0"));
        assertEquals(Long.valueOf(5000L), NumberCompat.toLong("5E3"));
    }

    /**
     * 测试用例：超范围溢出边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：传入"1E30"或"Infinity"或"NaN"
     * 预期结果：返回 null
     */
    public void toLong_outOfRange_returnsNull() {
        assertNull(NumberCompat.toLong("1E30"));
        assertNull(NumberCompat.toLong("-1E30"));
        assertNull(NumberCompat.toLong("Infinity"));
        assertNull(NumberCompat.toLong("NaN"));
    }

    // ---- toLongOrDefault ----

    /**
     * 测试用例：null回退默认
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入 null 或无效值
     * 预期结果：返回默认值
     */
    public void toLongOrDefault_nullInput_returnsDefault() {
        assertEquals(99L, NumberCompat.toLongOrDefault(null, 99L));
        assertEquals(99L, NumberCompat.toLongOrDefault("abc", 99L));
    }

    /**
     * 测试用例：有效输入回退
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入"5.0"
     * 预期结果：返回 5L
     */
    public void toLongOrDefault_validInput_returnsParsed() {
        assertEquals(5L, NumberCompat.toLongOrDefault("5.0", 99L));
    }

    // ---- toInt ----

    /**
     * 测试用例：toInt各种形式
     * 输入类别：同类（有效等价类）
     * 测试步骤：测试各种输入形式
     * 预期结果：正确转换
     */
    public void toInt_forms() {
        assertNull(NumberCompat.toInt(null));
        assertNull(NumberCompat.toInt(""));
        assertNull(NumberCompat.toInt("abc"));
        assertEquals(Integer.valueOf(5), NumberCompat.toInt("5.0"));
        assertEquals(Integer.valueOf(-3), NumberCompat.toInt(-3.9));
        assertEquals(Integer.valueOf(7), NumberCompat.toInt(Integer.valueOf(7)));
        assertEquals(Integer.valueOf(5000), NumberCompat.toInt("5E3"));
        assertNull(NumberCompat.toInt("1E30"));
    }

    /**
     * 测试用例：toIntOrDefault各种形式
     * 输入类别：同类（有效等价类）
     * 测试步骤：测试各种输入形式
     * 预期结果：正确转换
     */
    public void toIntOrDefault_forms() {
        assertEquals(Integer.valueOf(9), NumberCompat.toIntOrDefault(null, 9));
        assertEquals(Integer.valueOf(5), NumberCompat.toIntOrDefault("5.0", 9));
    }

    // ---- toBool：三形态 ----

    /**
     * 测试用例：Boolean实例
     * 输入类别：同类（有效等价类）
     * 测试步骤：传入 Boolean.TRUE/FALSE
     * 预期结果：返回 true/false
     */
    public void toBool_booleanInstance_directValue() {
        assertTrue(NumberCompat.toBool(Boolean.TRUE));
        assertEquals(false, NumberCompat.toBool(Boolean.FALSE));
    }

    /**
     * 测试用例：0/非0判定边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：传入 1/-1/2.5/0/0.0
     * 预期结果：非0为真，0为假
     */
    public void toBool_numberInstance_nonZeroIsTrue() {
        assertTrue(NumberCompat.toBool(1));
        assertTrue(NumberCompat.toBool(-1));
        assertTrue(NumberCompat.toBool(2.5));
        assertEquals(false, NumberCompat.toBool(0));
        assertEquals(false, NumberCompat.toBool(0.0));
    }

    /**
     * 测试用例：其他类型回退
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入 null/"true"/"1"/""
     * 预期结果：返回 false
     */
    public void toBool_otherInputs_returnsFalse() {
        assertEquals(false, NumberCompat.toBool(null));
        assertEquals(false, NumberCompat.toBool("true"));
        assertEquals(false, NumberCompat.toBool("1"));
        assertEquals(false, NumberCompat.toBool(""));
    }
}