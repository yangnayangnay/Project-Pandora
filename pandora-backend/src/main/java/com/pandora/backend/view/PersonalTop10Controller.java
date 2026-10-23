package com.pandora.backend.view;

import com.pandora.backend.audit.Auditable;
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
@RequestMapping("/view/personal-top10")
public class PersonalTop10Controller {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/inject")
    @Auditable(operationType = "PERSONAL_TOP10_INJECT", targetType = "PERSONAL_TOP10")
    public ResponseEntity<ApiResponse<Map<String, Object>>> inject(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();

        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.TOP10_CONTENT_EMPTY);
        }
        if (content.length() > 512) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "内容长度不能超过512");
        }

        Object rankObj = request.get("rankOrder");
        if (rankObj == null) rankObj = request.get("rank_order");
        Integer rankOrder = rankObj != null ? ((Number) rankObj).intValue() : 1;
        if (rankOrder < 1 || rankOrder > 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM top10_works WHERE user_id = ? AND work_type = 'PERSONAL_IMPORTANT' AND deleted = 0",
                Integer.class, userId);
        if (count != null && count >= 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        // Round23 REQ-23-01: 可选 category 入参——PERSONAL_INFO(个人信息类，进入个人十大信息象限) /
        // WORK_ITEM(工作事项类，默认，不展示)。
        // 非法值处理策略：按缺省 WORK_ITEM 处理（宽容策略，旧客户端不带该参数时行为与旧版完全一致）
        Object categoryObj = request.get("category");
        String category = categoryObj != null ? categoryObj.toString().trim() : "";
        if (!"PERSONAL_INFO".equals(category) && !"WORK_ITEM".equals(category)) {
            category = "WORK_ITEM";
        }

        jdbcTemplate.update(
                "INSERT INTO top10_works (user_id, work_type, rank_order, content, is_auto_filled, category) VALUES (?, 'PERSONAL_IMPORTANT', ?, ?, 0, ?)",
                userId, rankOrder, content.trim(), category);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/inject/{id}")
    @Auditable(operationType = "PERSONAL_TOP10_UPDATE", targetType = "PERSONAL_TOP10")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE id = ? AND work_type = 'PERSONAL_IMPORTANT' AND deleted = 0", id);
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "条目不存在");
        }

        Long ownerId = ((Number) existing.get(0).get("USER_ID")).longValue();
        if (!ownerId.equals(userId)) {
            throw new BusinessException(ErrorCode.PERSONAL_TOP10_INJECT_FORBIDDEN);
        }

        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.TOP10_CONTENT_EMPTY);
        }

        jdbcTemplate.update(
                "UPDATE top10_works SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                content.trim(), id);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/inject/{id}")
    @Auditable(operationType = "PERSONAL_TOP10_DELETE", targetType = "PERSONAL_TOP10")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE id = ? AND work_type = 'PERSONAL_IMPORTANT' AND deleted = 0", id);
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "条目不存在");
        }

        Long ownerId = ((Number) existing.get(0).get("USER_ID")).longValue();
        if (!ownerId.equals(userId)) {
            throw new BusinessException(ErrorCode.PERSONAL_TOP10_INJECT_FORBIDDEN);
        }

        jdbcTemplate.update(
                "UPDATE top10_works SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}