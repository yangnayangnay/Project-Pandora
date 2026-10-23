package com.example.project_pandora.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Round25 缺陷修复（R25-02）：统一日期字符串本地化解析工具。
 * 后端 Jackson 以 UTC ISO 格式（yyyy-MM-dd'T'HH:mm:ss.SSS+00:00）序列化 DATETIME 字段，
 * App 端直接 substring(0,10) 会把本地 00:00~07:59 的日期截成前一天（时区差 8 小时）。
 * 本工具按偏移量换算为本地时区后再返回，兼容无时区的本地格式与短串。
 */
public final class DateCompat {

    private static final String[] ISO_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
    };

    private DateCompat() {
    }

    /** 解析为本地日期字符串 yyyy-MM-dd；入参 null/空返回 null；无法解析回退截取前 10 位。 */
    public static String toLocalDate(Object timeObj) {
        return format(timeObj, false);
    }

    /** 解析为本地日期时间字符串 yyyy-MM-dd HH:mm；入参 null/空返回 null；回退按原串长度截取。 */
    public static String toLocalDateTime(Object timeObj) {
        return format(timeObj, true);
    }

    private static String format(Object timeObj, boolean withTime) {
        if (timeObj == null) return null;
        String s = timeObj.toString().trim();
        if (s.isEmpty()) return null;
        for (String p : ISO_PATTERNS) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.US);
                Date d = in.parse(s);
                SimpleDateFormat out = new SimpleDateFormat(
                        withTime ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd", Locale.getDefault());
                return out.format(d);
            } catch (Exception ignored) {
            }
        }
        // 本地格式（无时区标识）或短串：按长度直接截取，语义与原实现一致
        if (withTime) {
            if (s.length() >= 16) return s.substring(0, 10) + " " + s.substring(11, 16);
            if (s.length() >= 10) return s.substring(0, 10);
        } else {
            if (s.length() >= 10) return s.substring(0, 10);
        }
        return s;
    }
}