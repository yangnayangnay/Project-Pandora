package com.example.project_pandora.ui.mindmap;

/**
 * Round24 REQ-24-04: 个人十大信息条目类型标签纯函数映射。
 * sourceNote 以"账号信息-"开头按后缀映射九种账号字段简称；
 * workType 映射三种业务类型标签；未知输入返回 null（省略标签，不显示错误文本）。
 * 映射集合为封闭白名单，不含密码/身份证等敏感字段路径。
 */
public final class Top10TypeLabelMapper {

    private static final String ACCOUNT_INFO_PREFIX = "\u8D26\u53F7\u4FE1\u606F-";

    private Top10TypeLabelMapper() {
    }

    public static String resolveLabel(String workType, String sourceNote) {
        if (sourceNote != null && sourceNote.startsWith(ACCOUNT_INFO_PREFIX)) {
            String suffix = sourceNote.substring(ACCOUNT_INFO_PREFIX.length());
            switch (suffix) {
                case "\u771F\u5B9E\u59D3\u540D": return "\u59D3\u540D";
                case "\u7528\u6237\u540D": return "\u7528\u6237\u540D";
                case "\u90E8\u95E8": return "\u90E8\u95E8";
                case "\u56E2\u961F": return "\u56E2\u961F";
                case "\u89D2\u8272": return "\u89D2\u8272";
                case "\u804C\u4F4D": return "\u804C\u4F4D";
                case "MBTI": return "MBTI";
                case "\u624B\u673A\u53F7": return "\u624B\u673A";
                case "\u90AE\u7BB1": return "\u90AE\u7BB1";
                default: return null;
            }
        }
        if (workType != null) {
            switch (workType) {
                case "BIRTHDAY_WISH": return "\u751F\u65E5\u795D\u798F";
                case "PERSONAL_IMPORTANT": return "\u4E2A\u4EBA\u4E8B\u9879";
                case "AI_ANALYSIS": return "AI \u5206\u6790";
                default: return null;
            }
        }
        return null;
    }
}