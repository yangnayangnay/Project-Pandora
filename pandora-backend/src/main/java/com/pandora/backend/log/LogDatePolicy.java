package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 日志日期策略——定义可编辑日志的日期范围。
 * 规则：仅可填写或编辑前天、昨天、今天的日志。
 * 被日志创建/编辑流程调用。
 */
public final class LogDatePolicy {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private LogDatePolicy() {}

    /**
     * 校验日志日期是否可编辑。
     * 使用系统默认时钟。
     *
     * @param date 日期字符串（yyyy-MM-dd）
     * @return 校验通过的日期字符串
     */
    public static String requireWritable(String date) {
        return requireWritable(date, Clock.system(ZONE));
    }

    /**
     * 校验日志日期是否可编辑（可指定时钟，用于测试）。
     * 规则：仅可填写或编辑前天、昨天、今天的日志。
     *
     * @param date 日期字符串（yyyy-MM-dd）
     * @param clock 时钟（用于测试）
     * @return 校验通过的日期字符串
     */
    static String requireWritable(String date, Clock clock) {
        try {
            LocalDate value = LocalDate.parse(date);
            LocalDate today = LocalDate.now(clock.withZone(ZONE));
            if (value.isAfter(today) || value.isBefore(today.minusDays(2))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "仅可填写或编辑前天、昨天、今天的日志");
            }
            return value.toString();
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日志日期格式应为 yyyy-MM-dd");
        }
    }
}