package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import java.time.LocalDate;

/**
 * 日志内容规则校验工具类。
 * 提供日志标题生成和正文校验方法。
 * 被日志创建/编辑流程调用。
 */
public final class LogContentRules {
    private LogContentRules() {}

    /**
     * 生成日志标题。
     * 格式：X月X日日志
     *
     * @param date 日期字符串（yyyy-MM-dd）
     * @return 格式化的日志标题
     */
    public static String title(String date) {
        LocalDate day = LocalDate.parse(date);
        return day.getMonthValue() + "月" + day.getDayOfMonth() + "日日志";
    }

    /**
     * 校验日志正文。
     * 规则：不能为空（至少包含一个非空白字符）。
     *
     * @param content 日志正文
     * @return 校验通过的正文
     */
    public static String requireBody(Object content) {
        if (!(content instanceof String) || ((String) content).codePoints()
                .noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "日志正文不能为空");
        }
        return (String) content;
    }
}