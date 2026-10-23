package com.example.project_pandora.core.util;

/**
 * Round26 REQ-26-02：统一数字安全解析工具。
 * Gson 将 JSON 数字反序列化为 Double（"5.0"），Long.parseLong 直接崩溃——
 * Round 24 缺陷与 Round 25 缺陷⑫两次实际发作的根因。
 * 行为基准：TaskCreateActivity 缺陷⑫修复落点（instanceof Number 取值 + parseDouble 兜底）。
 * 任何输入均不抛出异常；null 返回值语义=解析失败。
 */
public final class NumberCompat {

    private static final double LONG_MAX_AS_DOUBLE = 9.223372036854774E18;
    private static final double LONG_MIN_AS_DOUBLE = -9.223372036854776E18;

    private NumberCompat() {
    }

    /** Number→longValue；非空数字串（含"5.0"/科学计数法）→parseDouble 后取整；null/空串/非数字串/超 Long 范围→null。 */
    public static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        Double d = parseDoubleSafely(value);
        if (d == null || !isLongRange(d)) return null;
        return d.longValue();
    }

    /** toLong 为 null 时返回 def（适配"默认值参与业务"的既有语义点）。 */
    public static long toLongOrDefault(Object value, long def) {
        Long r = toLong(value);
        return r == null ? def : r;
    }

    /** Number→intValue；非空数字串（含"5.0"/科学计数法）→parseDouble 后取整；null/空串/非数字串/超 int 范围→null。 */
    public static Integer toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        Double d = parseDoubleSafely(value);
        if (d == null || !isIntRange(d)) return null;
        return d.intValue();
    }

    /** toInt 为 null 时返回 def。 */
    public static Integer toIntOrDefault(Object value, Integer def) {
        Integer r = toInt(value);
        return r == null ? def : r;
    }

    /** Boolean 直取；Number 按 intValue≠0；其余（null/串等）false。对齐 TaskDetailActivity 既有 toBool 语义。 */
    public static boolean toBool(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return false;
    }

    private static Double parseDoubleSafely(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            double d = Double.parseDouble(s);
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isLongRange(double d) {
        return d >= LONG_MIN_AS_DOUBLE && d <= LONG_MAX_AS_DOUBLE;
    }

    private static boolean isIntRange(double d) {
        return d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE;
    }
}