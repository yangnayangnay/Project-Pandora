package com.pandora.backend.draft;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Map;

@Service
public class DraftService {

    @Autowired
    private DraftDao draftDao;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> saveDraft(Long userId, String draftType, Map<String, Object> content) {
        validateDraftType(draftType);
        if (content == null || content.isEmpty() || isAllEmpty(content)) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "草稿内容不能为空");
        }
        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿内容序列化失败");
        }
        draftDao.saveOrUpdate(userId, draftType, contentJson);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("savedAt", new Timestamp(System.currentTimeMillis()));
        return result;
    }

    public Map<String, Object> getDraft(Long userId, String draftType) {
        validateDraftType(draftType);
        Map<String, Object> row = draftDao.findByUserIdAndType(userId, draftType);
        if (row == null) {
            return null;
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("draftId", row.get("DRAFT_ID"));
        result.put("draftType", row.get("DRAFT_TYPE"));
        String contentStr = (String) row.get("CONTENT");
        if (contentStr != null) {
            try {
                result.put("content", objectMapper.readValue(contentStr, Map.class));
            } catch (Exception e) {
                result.put("content", contentStr);
            }
        }
        result.put("updatedAt", row.get("UPDATED_AT"));
        return result;
    }

    public void deleteDraft(Long userId, String draftType) {
        validateDraftType(draftType);
        draftDao.deleteByUserIdAndType(userId, draftType);
    }

    public int cleanupExpired() {
        long sevenDaysAgoMillis = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        Timestamp expireBefore = new Timestamp(sevenDaysAgoMillis);
        return draftDao.deleteExpired(expireBefore);
    }

    private void validateDraftType(String draftType) {
        try {
            DraftType.valueOf(draftType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法草稿类型: " + draftType);
        }
    }

    private boolean isAllEmpty(Map<String, Object> content) {
        return content.values().stream().allMatch(v ->
                v == null || (v instanceof String && ((String) v).trim().isEmpty()));
    }
}