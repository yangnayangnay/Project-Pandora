package com.pandora.backend.template;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TemplateService {

    @Autowired
    private TemplateDao templateDao;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> saveTemplate(Long userId, String templateType, String name, Map<String, Object> content) {
        validateTemplateType(templateType);
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "模板名称不能为空");
        }
        String contentJson = toJson(content);
        Long templateId = templateDao.save(userId, templateType, name.trim(), contentJson);
        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        return result;
    }

    public List<Map<String, Object>> listTemplates(Long userId, String templateType) {
        validateTemplateType(templateType);
        List<Map<String, Object>> rows = templateDao.findByUserIdAndType(userId, templateType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("templateId", row.get("TEMPLATE_ID"));
            item.put("templateType", row.get("TEMPLATE_TYPE"));
            item.put("name", row.get("NAME"));
            String contentStr = (String) row.get("CONTENT");
            if (contentStr != null) {
                try {
                    item.put("content", objectMapper.readValue(contentStr, Map.class));
                } catch (Exception e) {
                    item.put("content", contentStr);
                }
            }
            item.put("updatedAt", row.get("UPDATED_AT"));
            result.add(item);
        }
        return result;
    }

    public void updateTemplate(Long userId, Long templateId, String name, Map<String, Object> content) {
        Map<String, Object> existing = templateDao.findById(templateId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        Long ownerId = ((Number) existing.get("USER_ID")).longValue();
        if (!ownerId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑他人模板");
        }
        String contentJson = toJson(content);
        String finalName = name != null ? name.trim() : (String) existing.get("NAME");
        int updated = templateDao.update(templateId, finalName, contentJson);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        }
    }

    public void deleteTemplate(Long userId, Long templateId) {
        Map<String, Object> existing = templateDao.findById(templateId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        Long ownerId = ((Number) existing.get("USER_ID")).longValue();
        if (!ownerId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人模板");
        }
        templateDao.logicalDelete(templateId);
    }

    public Map<String, Object> migrateTemplates(Long userId, String templateType, List<Map<String, Object>> templates) {
        validateTemplateType(templateType);
        int migratedCount = 0;
        int skippedCount = 0;
        for (Map<String, Object> tpl : templates) {
            String name = (String) tpl.get("name");
            if (name == null || name.trim().isEmpty()) {
                skippedCount++;
                continue;
            }
            if (templateDao.existsByUserIdAndNameAndType(userId, name.trim(), templateType)) {
                skippedCount++;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) tpl.get("content");
            String contentJson = toJson(content);
            templateDao.save(userId, templateType, name.trim(), contentJson);
            migratedCount++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("migratedCount", migratedCount);
        result.put("skippedCount", skippedCount);
        return result;
    }

    private void validateTemplateType(String templateType) {
        try {
            TemplateType.valueOf(templateType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法模板类型: " + templateType);
        }
    }

    private String toJson(Map<String, Object> content) {
        if (content == null) return "{}";
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板内容序列化失败");
        }
    }
}