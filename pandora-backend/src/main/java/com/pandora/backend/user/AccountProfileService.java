package com.pandora.backend.user;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Admin-only partial maintenance; legacy fields that were not edited remain intact. */
@Service
public class AccountProfileService {
    private static final Map<String, String> COLUMNS = Map.of(
            "employeeNo", "username", "realName", "real_name", "email", "email",
            "phone", "phone", "idCard", "id_card", "birthday", "birthday");
    private final JdbcTemplate db;

    public AccountProfileService(JdbcTemplate db) { this.db = db; }

    @Transactional
    public Map<String, Object> update(long actorId, long targetId, Map<String, Object> request) {
        if (db.queryForList("SELECT id FROM users WHERE id=? AND role='ADMIN' AND deleted=0 "
                + "AND approval_status='APPROVED'", Long.class, actorId).isEmpty())
            throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!COLUMNS.keySet().containsAll(request.keySet()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的账号资料字段");
        List<Map<String, Object>> accounts = db.queryForList("SELECT id,username,real_name,email,phone,id_card,birthday "
                + "FROM users WHERE id=? AND deleted=0 FOR UPDATE", targetId);
        if (accounts.isEmpty()) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        Map<String, Object> current = accounts.get(0), changes = new LinkedHashMap<>();
        for (var entry : request.entrySet()) {
            String field = entry.getKey(), column = COLUMNS.get(field);
            Object raw = entry.getValue();
            if (raw != null && !(raw instanceof String))
                throw new BusinessException(ErrorCode.BAD_REQUEST, "账号资料格式无效");
            String value = raw == null ? "" : ((String) raw).trim();
            String original = Objects.toString(current.get(column), "");
            if (value.equals(original)) continue;
            if (value.isEmpty()) throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "已填写的账号资料不能为空");
            changes.put(column, validate(field, value, targetId));
        }
        if (!changes.isEmpty()) {
            List<String> assignments = new ArrayList<>();
            changes.keySet().forEach(column -> assignments.add(column + "=?"));
            List<Object> values = new ArrayList<>(changes.values()); values.add(targetId);
            try {
                db.update("UPDATE users SET " + String.join(",", assignments)
                        + ",updated_at=CURRENT_TIMESTAMP WHERE id=?", values.toArray());
            } catch (DuplicateKeyException error) {
                throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "工号已存在，请使用其他工号");
            }
        }
        // Never return password hashes, even to the management page.
        Map<String, Object> saved = new LinkedHashMap<>();
        db.queryForMap("SELECT id,username,real_name,email,phone,id_card,birthday,updated_at FROM users WHERE id=?", targetId)
                .forEach((key, value) -> saved.put(key.toLowerCase(Locale.ROOT), value));
        if (saved.get("birthday") != null) saved.put("birthday", saved.get("birthday").toString());
        return saved;
    }

    private Object validate(String field, String value, long id) {
        switch (field) {
            case "employeeNo" -> {
                if (!value.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$"))
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "工号须为1至64位字母、数字、点、下划线或短横线");
                if (count("SELECT COUNT(*) FROM users WHERE username=? AND id<>?", value, id) > 0)
                    throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "工号已存在，请使用其他工号");
            }
            case "realName" -> {
                if (value.length() > 64) throw new BusinessException(ErrorCode.BAD_REQUEST, "真实姓名不能超过64字");
            }
            case "email" -> {
                if (value.length() > 128 || !value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
                    throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
                if (count("SELECT COUNT(*) FROM users WHERE LOWER(email)=LOWER(?) AND id<>? AND deleted=0", value, id) > 0)
                    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            case "phone" -> {
                if (!value.matches("^1[3-9]\\d{9}$")) throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
            }
            case "idCard" -> {
                value = value.toUpperCase(Locale.ROOT);
                if (!value.matches("^\\d{17}[\\dX]$")) throw new BusinessException(ErrorCode.INVALID_ID_CARD);
                if (count("SELECT COUNT(*) FROM users WHERE UPPER(id_card)=? AND id<>? AND deleted=0", value, id) > 0)
                    throw new BusinessException(ErrorCode.ID_CARD_ALREADY_EXISTS);
            }
            case "birthday" -> {
                try {
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("format", value, 0);
                    LocalDate birthday = LocalDate.parse(value);
                    if (birthday.getYear() < 1 || birthday.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "生日不能晚于今天，且年份须大于0");
                    return birthday;
                } catch (DateTimeParseException error) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "生日须为有效的年月日");
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return value;
    }

    private int count(String query, Object... args) { return db.queryForObject(query, Integer.class, args); }
}
