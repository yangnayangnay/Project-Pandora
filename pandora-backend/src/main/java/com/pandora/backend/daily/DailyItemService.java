package com.pandora.backend.daily;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Private multi-day calendar items. Ownership always comes from the authenticated account. */
@Service
public class DailyItemService {
    private final JdbcTemplate db;
    public DailyItemService(JdbcTemplate db) { this.db = db; }

    public List<Map<String, Object>> list(long actor, String month) {
        YearMonth range;
        try {
            if (month == null || !month.matches("\\d{4}-\\d{2}")) throw new IllegalArgumentException();
            range = YearMonth.parse(month);
            if (range.getYear() < 1900) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "月份格式应为yyyy-MM，年份不早于1900年"); }
        return range(actor, Date.valueOf(range.atDay(1)), Date.valueOf(range.atEndOfMonth()));
    }

    public List<Map<String, Object>> listForDate(long actor, String day) {
        Date date = date(day);
        return range(actor, date, date);
    }

    private List<Map<String, Object>> range(long actor, Date from, Date to) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : db.queryForList(
                "SELECT id, content, item_date, start_date, end_date, completed, created_at, updated_at FROM daily_items " +
                "WHERE user_id = ? AND deleted = 0 AND start_date <= ? AND end_date >= ? ORDER BY start_date, completed, created_at, id",
                actor, to, from)) result.add(fields(row));
        return result;
    }

    @Transactional
    public Map<String, Object> create(long actor, Map<String, Object> request) {
        if (!Set.of("content", "itemDate", "startDate", "endDate").containsAll(request.keySet())
                || !request.containsKey("content") || !request.containsKey("itemDate") && !request.containsKey("startDate"))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供事项内容和起止日期");
        String content = content(request.get("content"));
        Date start = date(request.containsKey("startDate") ? request.get("startDate") : request.get("itemDate"));
        Date end = date(request.containsKey("endDate") ? request.get("endDate") : start.toString());
        requireRange(start, end);
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO daily_items (user_id, content, item_date, start_date, end_date) VALUES (?, ?, ?, ?, ?)", new String[]{"id"});
            statement.setLong(1, actor); statement.setString(2, content); statement.setDate(3, start);
            statement.setDate(4, start); statement.setDate(5, end);
            return statement;
        }, holder);
        return get(actor, Objects.requireNonNull(holder.getKey()).longValue());
    }

    @Transactional
    public Map<String, Object> update(long actor, long id, Map<String, Object> request) {
        if (request.isEmpty() || !Set.of("content", "itemDate", "startDate", "endDate", "completed").containsAll(request.keySet())) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的事项字段");
        Map<String, Object> current = get(actor, id);
        Date start = date(request.containsKey("startDate") ? request.get("startDate") : request.containsKey("itemDate") ? request.get("itemDate") : current.get("start_date"));
        Date end = date(request.containsKey("endDate") ? request.get("endDate")
                : request.containsKey("itemDate") ? start.toString() : current.get("end_date"));
        requireRange(start, end);
        StringBuilder sql = new StringBuilder("UPDATE daily_items SET updated_at = CURRENT_TIMESTAMP");
        List<Object> args = new ArrayList<>();
        if (request.containsKey("content")) { sql.append(", content = ?"); args.add(content(request.get("content"))); }
        if (request.containsKey("itemDate") || request.containsKey("startDate") || request.containsKey("endDate")) {
            sql.append(", item_date = ?, start_date = ?, end_date = ?"); args.add(start); args.add(start); args.add(end);
        }
        if (request.containsKey("completed")) {
            if (!(request.get("completed") instanceof Boolean completed)) throw new BusinessException(ErrorCode.BAD_REQUEST, "完成状态必须为布尔值");
            sql.append(", completed = ?"); args.add(completed ? 1 : 0);
        }
        sql.append(" WHERE id = ? AND user_id = ? AND deleted = 0"); args.add(id); args.add(actor);
        if (db.update(sql.toString(), args.toArray()) != 1) throw missing();
        return get(actor, id);
    }

    @Transactional
    public void delete(long actor, long id) {
        if (db.update("UPDATE daily_items SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ? AND deleted = 0", id, actor) != 1) throw missing();
    }

    private Map<String, Object> get(long actor, long id) {
        List<Map<String, Object>> rows = db.queryForList("SELECT id, content, item_date, start_date, end_date, completed, created_at, updated_at FROM daily_items WHERE id = ? AND user_id = ? AND deleted = 0", id, actor);
        if (rows.isEmpty()) throw missing();
        return fields(rows.get(0));
    }
    private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "事项不存在或不属于当前账号，请刷新"); }
    private static String content(Object raw) {
        if (!(raw instanceof String value) || value.strip().isEmpty() || value.strip().length() > 500) throw new BusinessException(ErrorCode.BAD_REQUEST, "事项内容须为1至500字");
        return value.strip();
    }
    private static Date date(Object raw) {
        try {
            if (!(raw instanceof String value) || !value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
            LocalDate date = LocalDate.parse(value);
            if (date.getYear() < 1900) throw new IllegalArgumentException();
            return Date.valueOf(date);
        } catch (RuntimeException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择有效的事项日期，年份不早于1900年"); }
    }
    private static void requireRange(Date start, Date end) {
        if (end.before(start)) throw new BusinessException(ErrorCode.BAD_REQUEST, "事项结束日期不能早于开始日期");
    }
    private static Map<String, Object> fields(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id")); result.put("content", row.get("content"));
        result.put("item_date", row.get("item_date").toString());
        result.put("start_date", row.get("start_date").toString());
        result.put("end_date", row.get("end_date").toString());
        Object done = row.get("completed");
        result.put("completed", Boolean.TRUE.equals(done) || done instanceof Number && ((Number) done).intValue() != 0);
        result.put("created_at", Objects.toString(row.get("created_at"), ""));
        result.put("updated_at", Objects.toString(row.get("updated_at"), ""));
        return result;
    }
}
