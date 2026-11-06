package com.pandora.backend.task;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class TaskTagService {
    private final JdbcTemplate db;
    public TaskTagService(JdbcTemplate db) { this.db = db; }

    public static boolean canManage(String role) { return "FOUNDER".equals(role); }

    private void requireFounder(long actor) {
        List<String> roles = db.queryForList("SELECT role FROM users WHERE id = ? AND deleted = 0 AND approval_status = 'APPROVED'", String.class, actor);
        if (roles.size() != 1 || !canManage(roles.get(0))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅创始人可管理标签，其他角色只能引用");
        }
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> tag : db.queryForList("SELECT id, name, color_index FROM task_tags WHERE deleted = 0 ORDER BY name, id")) result.add(tagFields(tag));
        return result;
    }

    @Transactional
    public Map<String, Object> save(long actor, Long id, Map<String, Object> request) {
        requireFounder(actor);
        if (!request.keySet().equals(Set.of("name", "colorIndex")) || !(request.get("name") instanceof String)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供标签名称和颜色");
        }
        String name = request.get("name").toString().trim().replaceFirst("^[#＃]+", "");
        if (name.isEmpty() || name.length() > 32 || name.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '#' || c == '＃')) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称须为1至32字，不含空白或#");
        }
        long color = integer(request.get("colorIndex"), "标签颜色无效");
        if (color < 0 || color > 7) throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择提供的八种颜色之一");
        String key = name.toLowerCase(Locale.ROOT);
        try {
            if (id == null) {
                GeneratedKeyHolder holder = new GeneratedKeyHolder();
                String finalName = name;
                db.update(connection -> {
                    var statement = connection.prepareStatement("INSERT INTO task_tags (name, name_key, color_index) VALUES (?, ?, ?)", new String[]{"id"});
                    statement.setString(1, finalName); statement.setString(2, key); statement.setLong(3, color);
                    return statement;
                }, holder);
                id = Objects.requireNonNull(holder.getKey()).longValue();
            } else if (db.update("UPDATE task_tags SET name = ?, name_key = ?, color_index = ? WHERE id = ? AND deleted = 0", name, key, color, id) != 1) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "标签已不存在，请刷新");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "同名标签已存在");
        }
        return tagFields(db.queryForMap("SELECT id, name, color_index FROM task_tags WHERE id = ?", id));
    }

    @Transactional
    public void delete(long actor, long id) {
        requireFounder(actor);
        if (db.update("UPDATE task_tags SET deleted = 1, name_key = NULL WHERE id = ? AND deleted = 0", id) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签已不存在，请刷新");
        }
        db.update("DELETE FROM task_tag_links WHERE tag_id = ?", id);
    }

    private static long integer(Object value, String message) {
        if (!(value instanceof Number)) throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        try { return new BigDecimal(value.toString()).longValueExact(); }
        catch (RuntimeException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, message); }
    }

    public List<Long> ids(Object value) {
        if (!(value instanceof List<?> values) || values.size() > 10) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每项任务最多引用10个标签，请提交有序标签列表");
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : values) {
            long id = integer(item, "标签编号无效");
            if (id <= 0 || ids.contains(id)) throw new BusinessException(ErrorCode.BAD_REQUEST, "标签编号无效或重复");
            ids.add(id);
        }
        return ids;
    }

    public void validate(Object value) { validateIds(ids(value), false); }

    private void validateIds(List<Long> ids, boolean lock) {
        if (ids.isEmpty()) return;
        List<Long> found = db.queryForList("SELECT id FROM task_tags WHERE deleted = 0 AND id IN (" + marks(ids.size()) + ")" + (lock ? " ORDER BY id FOR UPDATE" : ""), Long.class, ids.toArray());
        if (found.size() != ids.size()) throw new BusinessException(ErrorCode.BAD_REQUEST, "部分标签已删除或不存在，请重新选择标签后提交");
    }

    @Transactional
    public void replace(long taskId, Object value) {
        List<Long> ids = ids(value);
        validateIds(ids, true);
        db.update("DELETE FROM task_tag_links WHERE task_id = ?", taskId);
        for (int i = 0; i < ids.size(); i++) {
            db.update("INSERT INTO task_tag_links (task_id, tag_id, sort_order) VALUES (?, ?, ?)", taskId, ids.get(i), i);
        }
    }

    /** Batch enrichment preserves task order and avoids a separate query for each task. */
    public List<Map<String, Object>> attach(List<Map<String, Object>> tasks) {
        if (tasks.isEmpty()) return tasks;
        Map<Long, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> task : tasks) {
            Object id = task.get("id");
            if (id == null) id = task.get("ID");
            if (id instanceof Number) grouped.put(((Number) id).longValue(), new ArrayList<>());
        }
        List<Long> ids = new ArrayList<>(grouped.keySet());
        // Keep bind parameter counts bounded for large task lists.
        for (int offset = 0; offset < ids.size(); offset += 500) {
            List<Long> batch = ids.subList(offset, Math.min(offset + 500, ids.size()));
            List<Map<String, Object>> tags = db.queryForList("SELECT l.task_id, g.id, g.name, g.color_index FROM task_tag_links l JOIN task_tags g ON g.id = l.tag_id AND g.deleted = 0 WHERE l.task_id IN (" + marks(batch.size()) + ") ORDER BY l.task_id, l.sort_order, g.id", batch.toArray());
            for (Map<String, Object> raw : tags) {
                Map<String, Object> tag = tagFields(raw);
                grouped.get(((Number) raw.get("task_id")).longValue()).add(tag);
            }
        }
        for (Map<String, Object> task : tasks) {
            Object id = task.get("id"); if (id == null) id = task.get("ID");
            task.put("tags", id instanceof Number ? grouped.get(((Number) id).longValue()) : Collections.emptyList());
        }
        return tasks;
    }

    private static String marks(int size) { return String.join(",", Collections.nCopies(size, "?")); }
    private static Map<String, Object> tagFields(Map<String, Object> raw) {
        Map<String, Object> tag = new LinkedHashMap<>();
        tag.put("id", raw.get("id")); tag.put("name", raw.get("name")); tag.put("color_index", raw.get("color_index"));
        return tag;
    }
}
