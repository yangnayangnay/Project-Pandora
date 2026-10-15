package com.pandora.backend.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TemplateDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Long save(Long userId, String templateType, String name, String content) {
        jdbcTemplate.update(
                "INSERT INTO templates (user_id, template_type, name, content) VALUES (?, ?, ?, ?)",
                userId, templateType, name, content);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Map<String, Object>> findByUserIdAndType(Long userId, String templateType) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM templates WHERE user_id = ? AND template_type = ? AND deleted = 0 ORDER BY updated_at DESC",
                userId, templateType);
    }

    public int update(Long templateId, String name, String content) {
        return jdbcTemplate.update(
                "UPDATE templates SET name = ?, content = ?, updated_at = CURRENT_TIMESTAMP WHERE template_id = ? AND deleted = 0",
                name, content, templateId);
    }

    public int logicalDelete(Long templateId) {
        return jdbcTemplate.update(
                "UPDATE templates SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE template_id = ? AND deleted = 0",
                templateId);
    }

    public Map<String, Object> findById(Long templateId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM templates WHERE template_id = ? AND deleted = 0", templateId);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existsByUserIdAndNameAndType(Long userId, String name, String templateType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM templates WHERE user_id = ? AND name = ? AND template_type = ? AND deleted = 0",
                Integer.class, userId, name, templateType);
        return count != null && count > 0;
    }
}