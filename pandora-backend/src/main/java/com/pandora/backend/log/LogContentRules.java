package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import java.time.LocalDate;

/** Shared by direct writes and offline synchronization. Drafts remain editable and may be empty. */
public final class LogContentRules {
    private LogContentRules() {}

    public static String title(String date) {
        LocalDate day = LocalDate.parse(date);
        return day.getMonthValue() + "月" + day.getDayOfMonth() + "日日志";
    }

    public static String requireBody(Object content) {
        if (!(content instanceof String) || ((String) content).codePoints()
                .noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "日志正文不能为空");
        }
        return (String) content;
    }
}
