package com.pandora.backend.draft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class DraftDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveOrUpdate(Long userId, String draftType, String content) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM drafts WHERE user_id = ? AND draft_type = ?",
                Integer.class, userId, draftType);
        if (existing != null && existing > 0) {
            jdbcTemplate.update(
                    "UPDATE drafts SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND draft_type = ?",
                    content, userId, draftType);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO drafts (user_id, draft_type, content) VALUES (?, ?, ?)",
                    userId, draftType, content);
        }
    }

    public Map<String, Object> findByUserIdAndType(Long userId, String draftType) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM drafts WHERE user_id = ? AND draft_type = ?",
                userId, draftType);
        return list.isEmpty() ? null : list.get(0);
    }

    public int deleteByUserIdAndType(Long userId, String draftType) {
        return jdbcTemplate.update(
                "DELETE FROM drafts WHERE user_id = ? AND draft_type = ?",
                userId, draftType);
    }

    public int deleteExpired(Timestamp expireBefore) {
        return jdbcTemplate.update(
                "DELETE FROM drafts WHERE updated_at < ?",
                expireBefore);
    }
}