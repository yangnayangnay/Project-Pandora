package com.pandora.backend.ai;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class ExternalAIClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalAIClient.class);

    public String call(String prompt, String analysisType, String apiUrl, String apiKey, int timeoutMs) {
        return call(prompt, analysisType, apiUrl, apiKey, timeoutMs, "deepseek-chat");
    }

    public String call(String prompt, String analysisType, String apiUrl, String apiKey, int timeoutMs, String model) {
        try {
            // Round24 T2.2: 显式设置连接/读取超时，使 timeoutMs 形参真正生效（替代默认无限等待）
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Math.min(3000, timeoutMs));
            factory.setReadTimeout(timeoutMs);
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model != null && !model.isEmpty() ? model : "deepseek-chat");
            body.put("messages", Collections.singletonList(message));
            // Round24 T2.3: max_tokens 按 analysisType 分型收敛（DIVINATION=280/SUGGESTION=500/MBTI_REPORT=700/其余=500）
            body.put("max_tokens", resolveMaxTokens(analysisType));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new BusinessException(ErrorCode.AI_QUOTA_LOW);
            }

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_AI_FORMAT_ERROR);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.EXTERNAL_AI_FORMAT_ERROR);
            }

            Map<String, Object> messageResp = (Map<String, Object>) choices.get(0).get("message");
            return (String) messageResp.get("content");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("外部AI调用失败: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                throw new BusinessException(ErrorCode.AI_QUOTA_LOW);
            }
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "外部AI调用失败: " + e.getMessage());
        }
    }

    // Round24 T2.3: max_tokens 分型映射（封闭逻辑，未知类型落 500）
    private int resolveMaxTokens(String analysisType) {
        if (analysisType == null) {
            return 500;
        }
        switch (analysisType) {
            case "DIVINATION":
                return 280;
            case "MBTI_REPORT":
                return 700;
            case "SUGGESTION":
            default:
                return 500;
        }
    }

}