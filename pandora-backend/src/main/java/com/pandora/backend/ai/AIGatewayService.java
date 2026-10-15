package com.pandora.backend.ai;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AIGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AIGatewayService.class);

    @Autowired
    private ExternalAIClient externalAIClient;

    @Autowired
    private LocalFallbackAIClient localFallbackAIClient;

    @Autowired
    private AIChannelStateManager channelStateManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> invoke(String prompt, String analysisType) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> config = getConfig();
        if (config == null) {
            log.warn("AI配置不存在，使用本地兜底");
            result.put("result", localFallbackAIClient.call(prompt, analysisType));
            result.put("source", "LOCAL_FALLBACK");
            return result;
        }

        String apiUrl = getStr(config, "API_URL", "api_url");
        String apiKey = getStr(config, "API_KEY", "api_key");
        String model = getStr(config, "MODEL", "model");
        int timeoutMs = getInt(config, "TIMEOUT_MS", "timeout_ms", 30000);
        int enabled = getInt(config, "ENABLED", "enabled", 1);

        if (channelStateManager.getCurrentChannel() == AIChannelStateManager.Channel.EXTERNAL_AVAILABLE && enabled == 1) {
            try {
                String externalResult = externalAIClient.call(prompt, analysisType, apiUrl, apiKey, timeoutMs, model);
                result.put("result", externalResult);
                result.put("source", "EXTERNAL");
                return result;
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.AI_QUOTA_LOW) {
                    log.warn("外部AI额度不足，切换至本地兜底");
                    recordQuotaEvent(getStr(config, "API_NAME", "api_name"), 0, getInt(config, "QUOTA_THRESHOLD", "quota_threshold", 100));
                    channelStateManager.switchToLocal("额度不足");
                } else {
                    log.warn("外部AI调用失败，切换至本地兜底: {}", e.getMessage());
                    channelStateManager.switchToLocal("调用异常: " + e.getMessage());
                }
            } catch (Exception e) {
                log.warn("外部AI调用异常，切换至本地兜底: {}", e.getMessage());
                channelStateManager.switchToLocal("调用异常");
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

    public void refreshConfig() {
        log.info("AI配置已热刷新");
    }

    private Map<String, Object> getConfig() {
        try {
            List<Map<String, Object>> configs = jdbcTemplate.queryForList(
                    "SELECT * FROM ai_config WHERE enabled = 1 ORDER BY id LIMIT 1");
            return configs.isEmpty() ? null : configs.get(0);
        } catch (Exception e) {
            log.warn("读取AI配置失败: {}", e.getMessage());
            return null;
        }
    }

    private void recordQuotaEvent(String apiName, long remainingQuota, int threshold) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ai_quota_events (api_name, remaining_quota, trigger_threshold, action_taken) VALUES (?, ?, ?, 'SWITCH_TO_FALLBACK')",
                    apiName, remainingQuota, threshold);
        } catch (Exception e) {
            log.warn("记录额度事件失败: {}", e.getMessage());
        }
    }

    private String getStr(Map<String, Object> map, String upper, String lower) {
        Object val = map.get(upper);
        if (val == null) val = map.get(lower);
        return val != null ? val.toString() : "";
    }

    private int getInt(Map<String, Object> map, String upper, String lower, int defaultVal) {
        Object val = map.get(upper);
        if (val == null) val = map.get(lower);
        if (val == null) return defaultVal;
        return ((Number) val).intValue();
    }
}