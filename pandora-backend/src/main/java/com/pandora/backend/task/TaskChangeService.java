package com.pandora.backend.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.hierarchy.HierarchyService;
import com.pandora.backend.notification.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Editorial changes are proposals until the current responsible superior approves them. */
@Service
public class TaskChangeService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final HierarchyService hierarchy;
    private final NotificationService notifications;
    private final TaskTagService tags;

    public TaskChangeService(JdbcTemplate db, ObjectMapper json, HierarchyService hierarchy, NotificationService notifications, TaskTagService tags) {
        this.db = db; this.json = json; this.hierarchy = hierarchy; this.notifications = notifications;
        this.tags = tags;
    }

    public static boolean exempt(String role) { return "ADMIN".equals(role) || "FOUNDER".equals(role); }

    private String role(long id) {
        List<String> roles = db.queryForList("SELECT role FROM users WHERE id = ? AND deleted = 0 AND approval_status = 'APPROVED'", String.class, id);
        if (roles.isEmpty()) throw new BusinessException(ErrorCode.FORBIDDEN, "账号不存在、已停用或尚未批准");
        return roles.get(0);
    }

    public Long approver(long actor) {
        String role = role(actor);
        if (exempt(role)) return null;
        String expected = switch (role) {
            case "EMPLOYEE" -> "TEAM_LEADER";
            case "TEAM_LEADER" -> "DEPT_HEAD";
            case "DEPT_HEAD" -> "FOUNDER";
            default -> throw new BusinessException(ErrorCode.FORBIDDEN, "未知职级，无法确定审核人");
        };
        return hierarchy.getSuperiors(actor).stream()
                .filter(u -> expected.equals(u.get("role")))
                .sorted(Comparator.comparingInt(u -> ((Number) u.get("depth")).intValue()))
                .map(u -> ((Number) u.get("id")).longValue())
                .filter(id -> !db.queryForList("SELECT id FROM users WHERE id = ? AND deleted = 0 AND approval_status = 'APPROVED'", Long.class, id).isEmpty())
                .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "尚未配置对应职级的上级，请联系管理员维护人员关系"));
    }

    private Map<String, Object> task(long id, boolean lock) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM tasks WHERE id = ? AND deleted = 0" + (lock ? " FOR UPDATE" : ""), id);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        return rows.get(0);
    }

    private void checkActor(long actor, Map<String, Object> task, String operation) {
        if (exempt(role(actor))) return;
        boolean dispatcher = task.get("dispatcher_id") instanceof Number && actor == ((Number) task.get("dispatcher_id")).longValue();
        boolean assignee = isAssignee(((Number) task.get("id")).longValue(), actor, task);
        if (!dispatcher && !("UPDATE".equals(operation) && assignee)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅创建人可申请删除；创建人或责任人可申请编辑");
        }
    }

    private void validate(Map<String, Object> data, boolean create) {
        Set<String> allowed = Set.of("name", "content", "assigneeId", "assigneeIds", "isUrgent", "startTime", "endTime", "progressNote", "tagIds");
        for (String key : data.keySet()) if (!allowed.contains(key)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的任务编辑字段：" + key + "，执行状态请使用状态流转操作");
        }
        if (data.containsKey("tagIds")) tags.validate(data.get("tagIds"));
        for (String key : List.of("name", "content")) {
            if (create || data.containsKey(key)) {
                String value = Objects.toString(data.get(key), "").trim();
                if (value.isEmpty() || value.length() > ("name".equals(key) ? 256 : 5000)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "任务名称和正文必填，名称最多256字、正文最多5000字");
                }
            }
        }
        if (create || data.containsKey("assigneeId") || data.containsKey("assigneeIds")) assigneeIds(data, true);
        if (data.containsKey("isUrgent") && !(data.get("isUrgent") instanceof Boolean))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "紧急程度必须为布尔值");
        for (String dateKey : List.of("startTime", "endTime")) if (data.get(dateKey) != null) {
            taskDateTime(data.get(dateKey));
        }
    }

    private java.time.LocalDateTime taskDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof java.time.LocalDateTime local) return local;
        try {
            String text = value.toString().trim().replace(' ', 'T');
            return text.length() == 10 ? java.time.LocalDate.parse(text).atStartOfDay() : java.time.LocalDateTime.parse(text);
        } catch (RuntimeException error) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务开始或截止时间格式错误");
        }
    }

    private void validateSchedule(Map<String, Object> data, Map<String, Object> before, boolean create) {
        if (!create && !data.containsKey("startTime") && !data.containsKey("endTime")) return;
        Object defaultStart = create ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))
                : before == null ? null : before.get("start_time");
        Object defaultEnd = before == null ? null : before.get("end_time");
        java.time.LocalDateTime start = taskDateTime(data.getOrDefault("startTime", defaultStart));
        java.time.LocalDateTime end = taskDateTime(data.getOrDefault("endTime", defaultEnd));
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "截止时间必须晚于开始时间");
        }
    }

    @Transactional
    public Map<String, Object> submit(long actor, String operation, Long taskId, Map<String, Object> data) {
        Map<String, Object> before = taskId == null ? null : task(taskId, true);
        if (before != null) checkActor(actor, before, operation);
        if (!"DELETE".equals(operation)) {
            validate(data, "CREATE".equals(operation));
            validateSchedule(data, before, "CREATE".equals(operation));
        }
        if (exempt(role(actor))) {
            Long id = apply(actor, operation, taskId, data);
            return result(id, null, "APPROVED", "操作成功，当前职级免审");
        }
        Long reviewer = approver(actor);
        if (taskId != null && !db.queryForList("SELECT id FROM task_change_requests WHERE task_id = ? AND status = 'PENDING'", Long.class, taskId).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该任务已有待审核申请，请先处理已有申请");
        }
        String payload;
        try { payload = json.writeValueAsString(data); }
        catch (Exception e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "任务内容格式错误"); }
        long revision = before == null ? 0 : ((Number) before.get("revision")).longValue();
        long requestId = insert("INSERT INTO task_change_requests (task_id, operation, requester_id, approver_id, payload, base_revision, status) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')",
                taskId, operation, actor, reviewer, payload, revision);
        return result(taskId, requestId, "PENDING", "申请已提交，审核通过后生效");
    }

    private Map<String, Object> result(Long id, Long requestId, String status, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id); result.put("requestId", requestId); result.put("approvalStatus", status); result.put("message", message);
        return result;
    }

    public List<Map<String, Object>> requests(long viewer, boolean mine) {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT r.*, CASE WHEN u.role='ADMIN' THEN '系统' ELSE COALESCE(u.real_name,u.username) END AS requester_name, t.name AS task_name " +
                "FROM task_change_requests r JOIN users u ON u.id = r.requester_id LEFT JOIN tasks t ON t.id = r.task_id " +
                (mine ? "WHERE r.requester_id = ? " : "WHERE r.status = 'PENDING' ") + "ORDER BY r.created_at DESC", mine ? new Object[]{viewer} : new Object[]{});
        List<Map<String, Object>> visible = new ArrayList<>();
        Map<Long, String> tagNames = new HashMap<>();
        for (Map<String, Object> tag : tags.list()) tagNames.put(((Number) tag.get("id")).longValue(), "#" + tag.get("name"));
        for (Map<String, Object> raw : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            raw.forEach((k, v) -> row.put(k.toLowerCase(Locale.ROOT), v));
            try { row.put("payload", json.readValue(payloadText(row.get("payload")), Map.class)); }
            catch (Exception e) { row.put("payload", Collections.emptyMap()); }
            Map<?, ?> payload = (Map<?, ?>) row.get("payload");
            if (payload.containsKey("tagIds")) {
                List<String> names = new ArrayList<>();
                for (Long id : tags.ids(payload.get("tagIds"))) names.add(tagNames.getOrDefault(id, "已删除标签（" + id + "）"));
                row.put("tag_names", names.isEmpty() ? "无标签" : String.join("  ", names));
            }
            long requester = ((Number) row.get("requester_id")).longValue();
            try {
                Long current = approver(requester);
                row.put("current_approver_id", current == null ? requester : current);
                if (mine || Objects.equals(row.get("current_approver_id"), viewer)) visible.add(row);
            } catch (BusinessException e) {
                if (mine) { row.put("routing_error", e.getMessage()); visible.add(row); }
            }
        }
        return visible;
    }

    @Transactional
    public Map<String, Object> decide(long reviewer, long requestId, boolean approved, String reason) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM task_change_requests WHERE id = ? FOR UPDATE", requestId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "审核申请不存在");
        Map<String, Object> request = rows.get(0);
        if (!"PENDING".equals(request.get("status"))) throw new BusinessException(ErrorCode.BAD_REQUEST, "该申请已处理，请刷新");
        long actor = ((Number) request.get("requester_id")).longValue();
        Long expected = approver(actor);
        if (reviewer != (expected == null ? actor : expected)) throw new BusinessException(ErrorCode.FORBIDDEN, "仅当前对应上级可审核该申请");
        if (!approved && (reason == null || reason.trim().isEmpty())) throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写退回原因");
        if (reason != null && reason.length() > 500) throw new BusinessException(ErrorCode.BAD_REQUEST, "审核说明最多500字");
        Long taskId = request.get("task_id") == null ? null : ((Number) request.get("task_id")).longValue();
        String operation = request.get("operation").toString();
        if (approved) {
            Map<String, Object> before = null;
            if (taskId != null) {
                before = task(taskId, true);
                checkActor(actor, before, operation);
                if (((Number) before.get("revision")).longValue() != ((Number) request.get("base_revision")).longValue()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已变更，请退回此申请后重新提交，避免覆盖最新数据");
                }
            }
            Map<String, Object> data;
            try { data = json.readValue(payloadText(request.get("payload")), new TypeReference<Map<String, Object>>() {}); }
            catch (Exception e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "申请内容无法读取"); }
            if (!"DELETE".equals(operation)) {
                validate(data, "CREATE".equals(operation));
                validateSchedule(data, before, "CREATE".equals(operation));
            }
            taskId = apply(actor, operation, taskId, data);
        }
        db.update("UPDATE task_change_requests SET status = ?, task_id = ?, approver_id = ?, reason = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?",
                approved ? "APPROVED" : "REJECTED", taskId, reviewer, reason, requestId);
        return result(taskId, requestId, approved ? "APPROVED" : "REJECTED", approved ? "审核通过，操作已生效" : "申请已退回");
    }

    private Long apply(long actor, String operation, Long taskId, Map<String, Object> data) {
        if ("CREATE".equals(operation)) {
            List<Long> assignees = assigneeIds(data, true);
            long assignee = assignees.get(0);
            taskId = insert("INSERT INTO tasks (name, content, status, assignee_id, dispatcher_id, is_urgent, start_time, end_time) VALUES (?, ?, 'IN_PROGRESS', ?, ?, ?, ?, ?)",
                    data.get("name"), data.get("content"), assignee, actor,
                    bool(data.get("isUrgent")), data.getOrDefault("startTime", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString()), data.get("endTime"));
            replaceAssignees(taskId, assignees);
            if (data.containsKey("tagIds")) tags.replace(taskId, data.get("tagIds"));
            for (Long id : assignees) notifications.detectNewTask(taskId, id, actor);
            return taskId;
        }
        if ("DELETE".equals(operation)) {
            db.update("UPDATE tasks SET deleted = 1, revision = revision + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", taskId);
            return taskId;
        }
        Map<String, String> columns = Map.of("name", "name", "content", "content", "isUrgent", "is_urgent", "startTime", "start_time", "endTime", "end_time", "progressNote", "progress_note");
        StringBuilder sql = new StringBuilder("UPDATE tasks SET revision = revision + 1, updated_at = CURRENT_TIMESTAMP");
        List<Object> args = new ArrayList<>();
        Set<Long> previousAssignees = new LinkedHashSet<>();
        List<Long> nextAssignees = null;
        data.forEach((key, value) -> { if (columns.containsKey(key)) { sql.append(", ").append(columns.get(key)).append(" = ?"); args.add(key.startsWith("is") ? bool(value) : value); } });
        if (data.containsKey("assigneeId") || data.containsKey("assigneeIds")) {
            previousAssignees.addAll(db.queryForList("SELECT user_id FROM task_assignees WHERE task_id=?", Long.class, taskId));
            if (previousAssignees.isEmpty()) {
                Long legacy = db.queryForObject("SELECT assignee_id FROM tasks WHERE id=?", Long.class, taskId);
                if (legacy != null) previousAssignees.add(legacy);
            }
            nextAssignees = assigneeIds(data, true);
            sql.append(", assignee_id = ?"); args.add(nextAssignees.get(0));
            replaceAssignees(taskId, nextAssignees);
        }
        sql.append(" WHERE id = ?"); args.add(taskId); db.update(sql.toString(), args.toArray());
        if (nextAssignees != null) {
            Long dispatcher = db.queryForObject("SELECT dispatcher_id FROM tasks WHERE id=?", Long.class, taskId);
            for (Long id : nextAssignees) if (!previousAssignees.contains(id)) notifications.detectNewTask(taskId, id, dispatcher);
        }
        if (data.containsKey("tagIds")) tags.replace(taskId, data.get("tagIds"));
        return taskId;
    }

    private String payloadText(Object value) throws java.sql.SQLException {
        if (value instanceof java.sql.Clob clob) return clob.getSubString(1, Math.toIntExact(clob.length()));
        return Objects.toString(value, "{}");
    }

    @Transactional
    public void updateExecution(long actor, long taskId, String status, String progressNote) {
        Map<String, Object> before = task(taskId, true);
        boolean assignee = isAssignee(taskId, actor, before);
        boolean creator = before.get("dispatcher_id") instanceof Number && actor == ((Number) before.get("dispatcher_id")).longValue();
        if (progressNote != null && !assignee) throw new BusinessException(ErrorCode.FORBIDDEN, "仅责任人可更新进度");
        if (status != null) TaskStateMachine.validateTransition(before.get("status").toString(), status, assignee, creator);
        if (status != null || progressNote != null) db.update("UPDATE tasks SET status = ?, progress_note = ?, revision = revision + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status == null ? before.get("status") : status, progressNote == null ? before.get("progress_note") : progressNote, taskId);
    }

    private int bool(Object value) { return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(Objects.toString(value)) || "1".equals(Objects.toString(value)) ? 1 : 0; }

    private List<Long> assigneeIds(Map<String, Object> data, boolean required) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Object raw = data.get("assigneeIds");
        if (raw instanceof Collection<?> values) for (Object value : values) {
            try { ids.add(Long.parseLong(Objects.toString(value))); }
            catch (NumberFormatException error) { throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择有效负责人"); }
        }
        if (ids.isEmpty() && data.get("assigneeId") != null) {
            try { ids.add(Long.parseLong(data.get("assigneeId").toString())); }
            catch (NumberFormatException error) { throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择有效负责人"); }
        }
        if (required && ids.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少选择一位负责人");
        for (Long id : ids) if ("ADMIN".equals(role(id)))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "管理员不能作为任务负责人");
        return new ArrayList<>(ids);
    }

    private void replaceAssignees(long taskId, List<Long> ids) {
        db.update("DELETE FROM task_assignees WHERE task_id = ?", taskId);
        for (Long id : ids) db.update("INSERT INTO task_assignees(task_id,user_id) VALUES(?,?)", taskId, id);
    }

    private boolean isAssignee(long taskId, long actor, Map<String, Object> task) {
        if (!db.queryForList("SELECT user_id FROM task_assignees WHERE task_id=? AND user_id=?", Long.class, taskId, actor).isEmpty()) return true;
        return task.get("assignee_id") instanceof Number && actor == ((Number) task.get("assignee_id")).longValue();
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
}
