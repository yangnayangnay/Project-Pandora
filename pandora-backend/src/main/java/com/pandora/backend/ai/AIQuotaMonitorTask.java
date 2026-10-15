package com.pandora.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AIQuotaMonitorTask {

    private static final Logger log = LoggerFactory.getLogger(AIQuotaMonitorTask.class);

    @Autowired
    private ExternalAIClient externalAIClient;

    @Autowired
    private AIChannelStateManager channelStateManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelayString = "${pandora.ai.monitor-interval-ms:300000}")
    public void monitorAndRecover() {
        if (channelStateManager.getCurrentChannel() != AIChannelStateManager.Channel.LOCAL_FALLBACK) {
            return;
        }

        try {
            List<Map<String, Object>> configs = jdbcTemplate.queryForList(
                    "SELECT * FROM ai_config WHERE enabled = 1 ORDER BY id LIMIT 1");
            if (configs.isEmpty()) {
                return;
            }

            Map<String, Object> config = configs.get(0);
            String apiUrl = getStr(config, "API_URL", "api_url");
            String apiKey = getStr(config, "API_KEY", "api_key");

            if (externalAIClient.probe(apiUrl, apiKey)) {
                log.info("外部AI探测成功，回切至EXTERNAL_AVAILABLE");
                channelStateManager.switchToExternal();
            } else {
                log.debug("外部AI探测失败，保持LOCAL_FALLBACK");
            }
        } catch (Exception e) {
            log.warn("额度监控探测异常: {}", e.getMessage());
        }
    }

    private String getStr(Map<String, Object> map, String upper, String lower) {
        Object val = map.get(upper);
        if (val == null) val = map.get(lower);
        return val != null ? val.toString() : "";
    }
}