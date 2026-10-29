package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/** The server, UI and offline replay use the same yesterday/today/tomorrow window. */
public final class LogDatePolicy {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private LogDatePolicy() {}

    public static String requireWritable(String date) {
        return requireWritable(date, Clock.system(ZONE));
    }

    static String requireWritable(String date, Clock clock) {
        try {
            LocalDate value = LocalDate.parse(date);
            LocalDate today = LocalDate.now(clock.withZone(ZONE));
            if (value.isAfter(today.plusDays(1)) || value.isBefore(today.minusDays(1))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "仅可填写或编辑昨天、今天、明天的日志");
            }
            return value.toString();
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日志日期格式应为 yyyy-MM-dd");
        }
    }
}
