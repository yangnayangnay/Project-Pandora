package com.pandora.backend.ai;

import java.util.*;

public class FreeAiApiCatalog {

    public static List<Map<String, Object>> listAll() {
        List<Map<String, Object>> catalog = new ArrayList<>();

        Map<String, Object> deepseek = new HashMap<>();
        deepseek.put("apiName", "DeepSeek");
        deepseek.put("freeQuota", "每用户500万Token");
        deepseek.put("docUrl", "https://api-doc.deepseek.com");
        deepseek.put("compatibility", "OpenAI兼容格式");
        deepseek.put("recommended", true);
        catalog.add(deepseek);

        Map<String, Object> qwen = new HashMap<>();
        qwen.put("apiName", "通义千问");
        qwen.put("freeQuota", "每月100万Token");
        qwen.put("docUrl", "https://help.aliyun.com/zh/dashscope/developer-reference/tongyi-qianwen-api");
        qwen.put("compatibility", "自定义格式");
        qwen.put("recommended", false);
        catalog.add(qwen);

        Map<String, Object> zhipu = new HashMap<>();
        zhipu.put("apiName", "智谱AI(GLM)");
        zhipu.put("freeQuota", "新用户免费250万Token");
        zhipu.put("docUrl", "https://open.bigmodel.cn/dev/api");
        zhipu.put("compatibility", "自定义格式");
        zhipu.put("recommended", false);
        catalog.add(zhipu);

        return catalog;
    }
}