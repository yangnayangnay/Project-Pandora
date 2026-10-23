package com.example.project_pandora.core.util;

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

    @Test
    public void toLong_null_returnsNull() {
        assertNull(NumberCompat.toLong(null));
    }

    @Test
    public void toLong_emptyString_returnsNull() {
        assertNull(NumberCompat.toLong(""));
        assertNull(NumberCompat.toLong("   "));
    }

    @Test
    public void toLong_doubleString_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong("5.0"));
    }

    @Test
    public void toLong_negativeDoubleString_returnsValue() {
        assertEquals(Long.valueOf(-3L), NumberCompat.toLong("-3.0"));
    }

    @Test
    public void toLong_doubleInstance_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong(Double.valueOf(5.0)));
        assertEquals(Long.valueOf(-3L), NumberCompat.toLong(Double.valueOf(-3.7)));
    }

    @Test
    public void toLong_integerInstance_returnsValue() {
        assertEquals(Integer.valueOf(5).longValue(), NumberCompat.toLong(Integer.valueOf(5)).longValue());
    }

    @Test
    public void toLong_longInstance_returnsValue() {
        assertEquals(Long.valueOf(9223372036854775807L), NumberCompat.toLong(Long.MAX_VALUE));
    }

    @Test
    public void toLong_plainDigitString_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong("5"));
        assertEquals(Long.valueOf(-3L), NumberCompat.toLong("-3"));
    }

    @Test
    public void toLong_nonNumericString_returnsNull() {
        assertNull(NumberCompat.toLong("abc"));
        assertNull(NumberCompat.toLong("12x3"));
    }

    @Test
    public void toLong_scientificNotation_returnsValue() {
        assertEquals(Long.valueOf(5L), NumberCompat.toLong("5E0"));
        assertEquals(Long.valueOf(5000L), NumberCompat.toLong("5E3"));
    }

    @Test
    public void toLong_outOfRange_returnsNull() {
        assertNull(NumberCompat.toLong("1E30"));
        assertNull(NumberCompat.toLong("-1E30"));
        assertNull(NumberCompat.toLong("Infinity"));
        assertNull(NumberCompat.toLong("NaN"));
    }

    // ---- toLongOrDefault ----

    @Test
    public void toLongOrDefault_nullInput_returnsDefault() {
        assertEquals(99L, NumberCompat.toLongOrDefault(null, 99L));
        assertEquals(99L, NumberCompat.toLongOrDefault("abc", 99L));
    }

    @Test
    public void toLongOrDefault_validInput_returnsParsed() {
        assertEquals(5L, NumberCompat.toLongOrDefault("5.0", 99L));
    }

    // ---- toInt ----

    @Test
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

    @Test
    public void toIntOrDefault_forms() {
        assertEquals(Integer.valueOf(9), NumberCompat.toIntOrDefault(null, 9));
        assertEquals(Integer.valueOf(5), NumberCompat.toIntOrDefault("5.0", 9));
    }

    // ---- toBool：三形态 ----

    @Test
    public void toBool_booleanInstance_directValue() {
        assertTrue(NumberCompat.toBool(Boolean.TRUE));
        assertEquals(false, NumberCompat.toBool(Boolean.FALSE));
    }

    @Test
    public void toBool_numberInstance_nonZeroIsTrue() {
        assertTrue(NumberCompat.toBool(1));
        assertTrue(NumberCompat.toBool(-1));
        assertTrue(NumberCompat.toBool(2.5));
        assertEquals(false, NumberCompat.toBool(0));
        assertEquals(false, NumberCompat.toBool(0.0));
    }

    @Test
    public void toBool_otherInputs_returnsFalse() {
        assertEquals(false, NumberCompat.toBool(null));
        assertEquals(false, NumberCompat.toBool("true"));
        assertEquals(false, NumberCompat.toBool("1"));
        assertEquals(false, NumberCompat.toBool(""));
    }
}