package com.pandora.backend.ai;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/ai")
public class AIConfigController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AIGatewayService aiGatewayService;

    @Autowired
    private AIChannelStateManager channelStateManager;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        checkAdmin();
        List<Map<String, Object>> configs = jdbcTemplate.queryForList("SELECT * FROM ai_config ORDER BY id LIMIT 1");
        if (configs.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        Map<String, Object> config = new HashMap<>(configs.get(0));
        Object keyObj = config.get("API_KEY");
        if (keyObj == null) keyObj = config.get("api_key");
        if (keyObj != null) {
            String key = keyObj.toString();
            if (key.length() > 4) {
                config.put("API_KEY", key.substring(0, 3) + "********" + key.substring(key.length() - 3));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<Void>> updateConfig(@RequestBody Map<String, Object> request) {
        checkAdmin();
        String apiUrl = (String) request.get("apiUrl");
        String apiKey = (String) request.get("apiKey");
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "apiUrl不能为空");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "apiKey不能为空");
        }

        Object quotaObj = request.get("quotaThreshold");
        Integer quotaThreshold = quotaObj != null ? ((Number) quotaObj).intValue() : 100;
        Object timeoutObj = request.get("timeoutMs");
        Integer timeoutMs = timeoutObj != null ? ((Number) timeoutObj).intValue() : 30000;
        Object enabledObj = request.get("enabled");
        Integer enabled = enabledObj != null ? ((Number) enabledObj).intValue() : 1;
        String model = (String) request.get("model");
        String provider = (String) request.get("provider");

        StringBuilder sql = new StringBuilder("UPDATE ai_config SET api_url = ?, api_key = ?, quota_threshold = ?, timeout_ms = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();
        params.add(apiUrl);
        params.add(apiKey);
        params.add(quotaThreshold);
        params.add(timeoutMs);
        params.add(enabled);

        if (model != null && !model.trim().isEmpty()) {
            sql.append(", model = ?");
            params.add(model.trim());
        }
        if (provider != null && !provider.trim().isEmpty()) {
            sql.append(", provider = ?");
            params.add(provider.trim());
        }

        sql.append(" WHERE id = 1");
        jdbcTemplate.update(sql.toString(), params.toArray());

        aiGatewayService.refreshConfig();
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/channel-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChannelStatus() {
        checkAdmin();
        return ResponseEntity.ok(ApiResponse.success(channelStateManager.getState()));
    }

    @GetMapping("/free-catalog")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFreeCatalog() {
        checkAdmin();
        return ResponseEntity.ok(ApiResponse.success(FreeAiApiCatalog.listAll()));
    }

    private void checkAdmin() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ?", userId);
            String role = (String) user.get("ROLE");
            if (role == null) role = (String) user.get("role");
            if (!"ADMIN".equals(role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}