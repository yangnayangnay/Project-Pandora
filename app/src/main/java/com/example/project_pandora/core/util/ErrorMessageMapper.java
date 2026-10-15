package com.example.project_pandora.core.util;

/**
 * 错误码到用户友好文案映射器。
 * 根据后端返回的 code 映射精确提示文案，未匹配时回退到通用提示。
 */
public final class ErrorMessageMapper {

    private ErrorMessageMapper() {}

    /**
     * 根据错误码映射精确提示文案。
     *
     * @param errorCode     后端返回的错误码
     * @param serverMessage 后端返回的原始消息（用于回退）
     * @return 用户友好的提示文案
     */
    public static String map(int errorCode, String serverMessage) {
        switch (errorCode) {
            case 4040:
                return "十大事项数量已满（10/10），请先删除已有事项再添加";
            case 4041:
                return "内容不能为空，请输入后重试";
            case 4042:
                return "所选任务不存在或已被删除，请刷新任务列表后重试";
            case 4047:
                return "该任务已在十大派发中，不可重复选择";
            case 4048:
                return "该事项内容已存在，请勿重复添加";
            case 4049:
                return "排名序号必须在1-10之间";
            case 4050:
                return "保存失败，请重试。若多次失败请联系管理员";
            case 4060:
                return "人员关系存在循环引用，已自动截断";
            case 4061:
                return "存在孤儿节点（上级已删除），已单独展示";
            case 403:
                return "权限不足，该操作需要管理员或创始人权限";
            case 404:
                return "未找到该记录，可能已被删除，请刷新列表后重试";
            default:
                return "操作失败（错误码：" + errorCode + "），请联系管理员或重试";
        }
    }
}