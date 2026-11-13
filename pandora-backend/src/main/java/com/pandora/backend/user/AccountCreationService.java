package com.pandora.backend.user;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/** Creates an approved personnel account and its required contact/organization data together. */
@Service
public class AccountCreationService {
    private final JdbcTemplate db;
    private final BCryptPasswordEncoder encoder;

    public AccountCreationService(JdbcTemplate db, BCryptPasswordEncoder encoder) {
        this.db = db; this.encoder = encoder;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String employeeNo = required(request, request.containsKey("employeeNo") ? "employeeNo" : "username", "工号");
        if (request.containsKey("employeeNo") && request.containsKey("username")
                && !employeeNo.equals(required(request, "username", "工号"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工号与兼容账号字段不一致");
        }
        if (!employeeNo.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$"))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工号须为1至64位字母、数字、点、下划线或短横线");
        Object passwordValue = request.get("password");
        if (!(passwordValue instanceof String) || ((String) passwordValue).length() < 6)
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        String password = (String) passwordValue;
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能超过72个UTF-8字节");
        String role = required(request, "role", "角色");
        if (!OrganizationRules.isBusinessRole(role))
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        String realName = required(request, "realName", "真实姓名");
        if (realName.length() > 64) throw new BusinessException(ErrorCode.BAD_REQUEST, "真实姓名不能超过64字");
        String email = required(request, "email", "邮箱");
        if (email.length() > 128 || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        String phone = required(request, "phone", "手机号");
        if (!phone.matches("^1[3-9]\\d{9}$")) throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        String idCard = required(request, "idCard", "身份证号").toUpperCase(Locale.ROOT);
        if (!idCard.matches("^\\d{17}[\\dX]$")) throw new BusinessException(ErrorCode.INVALID_ID_CARD);
        String birthdayValue = required(request, "birthday", "生日");
        LocalDate birthday;
        try { birthday = LocalDate.parse(birthdayValue); }
        catch (DateTimeParseException error) { throw new BusinessException(ErrorCode.BAD_REQUEST, "生日须为有效的年月日"); }
        if (birthday.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "生日不能晚于今天");
        Long departmentId = OrganizationRules.hasDepartment(role) ? requiredId(request, "departmentId", "部门") : null;
        Long teamId = OrganizationRules.hasTeam(role) ? requiredId(request, "teamId", "团队") : null;
        if (departmentId != null && db.queryForList("SELECT id FROM departments WHERE id=? AND deleted=0", Long.class, departmentId).isEmpty())
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所选部门不存在或已停用");
        if (teamId != null && db.queryForList("SELECT id FROM teams WHERE id=? AND department_id=? AND deleted=0", Long.class, teamId, departmentId).isEmpty())
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所选团队不存在、已停用或不属于所选部门");
        if (db.queryForObject("SELECT COUNT(*) FROM users WHERE username=?", Integer.class, employeeNo) > 0)
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "工号已存在，请使用其他工号");
        if (db.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(email)=LOWER(?) AND deleted=0", Integer.class, email) > 0)
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        if (db.queryForObject("SELECT COUNT(*) FROM users WHERE UPPER(id_card)=? AND deleted=0", Integer.class, idCard) > 0)
            throw new BusinessException(ErrorCode.ID_CARD_ALREADY_EXISTS);

        var relationships = new OrganizationHierarchy(db);
        var before = relationships.snapshot();
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        Object[] values = {employeeNo, encoder.encode(password), role, realName, email, phone, idCard,
                birthday, departmentId, teamId};
        try {
            db.update(connection -> {
                var statement = connection.prepareStatement("INSERT INTO users " +
                        "(username,password_hash,role,real_name,email,phone,id_card,birthday,department_id,team_id,approval_status,deleted) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,'APPROVED',0)", new String[]{"id"});
                for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                return statement;
            }, key);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "工号已存在，请使用其他工号");
        }
        long createdId = key.getKey().longValue();
        if (OrganizationRules.isBusinessRole(role)) relationships.synchronize(before, createdId);
        return Map.of("id", createdId, "employeeNo", employeeNo, "username", employeeNo);
    }

    private static String required(Map<String, Object> request, String key, String label) {
        Object value = request.get(key);
        if (value == null || value instanceof String && ((String) value).isBlank())
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, label + "不能为空");
        if (!(value instanceof String)) throw new BusinessException(ErrorCode.BAD_REQUEST, label + "格式无效");
        return ((String) value).trim();
    }
    private static long requiredId(Map<String, Object> request, String key, String label) {
        Object value = request.get(key);
        if (value == null || value instanceof String && ((String) value).isBlank())
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "请选择" + label);
        try {
            long id = new BigDecimal(value.toString()).longValueExact();
            if (id <= 0) throw new ArithmeticException();
            return id;
        } catch (NumberFormatException | ArithmeticException error) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "编号无效");
        }
    }
}
