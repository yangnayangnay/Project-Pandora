package com.pandora.backend.ai;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Round28 REQ-28-02: AI 网关改为请求级透传判定。
 * 不再读取 ai_config 表、不再维护进程级通道状态机：
 * 每次调用依据 AiTransmitConfig（客户端随请求透传）独立判定——
 * 配置可用 → 调用客户端指定端点（source=EXTERNAL）；无效/失败/超时 → 本地兜底（source=LOCAL_FALLBACK）。
 * 降级语义与 Round24 口径一致：请求正常返回 200、不抛 AI_UNAVAILABLE、result/source 键名不变。
 */
@Service
public class AIGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AIGatewayService.class);

    /** Round28: 外部调用限时固定 15 秒（与各调用点 CompletableFuture 15s 限时对齐） */
    private static final int EXTERNAL_TIMEOUT_MS = 15000;

    @Autowired
    private ExternalAIClient externalAIClient;

    @Autowired
    private LocalFallbackAIClient localFallbackAIClient;

    public Map<String, Object> invoke(String prompt, String analysisType, AiTransmitConfig config) {
        Map<String, Object> result = new HashMap<>();

        if (config != null && config.isUsable()) {
            try {
                String externalResult = externalAIClient.call(
                        prompt, analysisType, config.getApiUrl(), config.getApiKey(), EXTERNAL_TIMEOUT_MS, config.getModel());
                result.put("result", externalResult);
                result.put("source", "EXTERNAL");
                return result;
            } catch (Exception e) {
                // 禁止记录 API Key 明文，仅记原因与端点/模型
                log.warn("透传外部AI调用失败，降级本地兜底: url={}, model={}, reason={}",
                        config.getApiUrl(), config.getModel(), e.getMessage());
            }
        }

        try {
            String fallbackResult = localFallbackAIClient.call(prompt, analysisType);
            result.put("result", fallbackResult);
            result.put("source", "LOCAL_FALLBACK");
            return result;
        } catch (Exception e) {
            log.error("本地兜底AI也不可用: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
        }
    }
}