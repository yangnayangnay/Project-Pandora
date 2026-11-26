package com.pandora.backend.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;

public class AccountCreationServiceTest {
    private JdbcTemplate jdbc;
    private BCryptPasswordEncoder encoder;
    private AccountCreationService svc;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(JdbcTemplate.class);
        encoder = Mockito.mock(BCryptPasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hashed");
        svc = new AccountCreationService(jdbc, encoder);
        setupDefaultMocks();
    }

    @SuppressWarnings("unchecked")
    private void setupDefaultMocks() {
        // 1. 部门查询 - 返回 List<Long> 包含有效 ID
        List<Long> deptList = List.of(1L);
        doReturn(deptList).when(jdbc).queryForList(
                eq("SELECT id FROM departments WHERE id=? AND deleted=0"),
                eq(Long.class),
                anyLong());
        
        // 2. 团队查询 - 返回 List<Long> 包含有效 ID
        List<Long> teamList = List.of(1L);
        doReturn(teamList).when(jdbc).queryForList(
                eq("SELECT id FROM teams WHERE id=? AND department_id=? AND deleted=0"),
                eq(Long.class),
                anyLong(),
                anyLong());
        
        // 3. COUNT(*) 查询默认返回 0（不存在）
        doReturn(0).when(jdbc).queryForObject(anyString(), eq(Integer.class), any());
        
        // 4. 插入操作 - 模拟返回生成的 key
        doAnswer(invocation -> {
            GeneratedKeyHolder keyHolder = invocation.getArgument(1);
            var field = GeneratedKeyHolder.class.getDeclaredField("keyList");
            field.setAccessible(true);
            List<Map<String, Object>> keyList = List.of(Map.of("", 100L));
            field.set(keyHolder, keyList);
            return 1;
        }).when(jdbc).update(any(), any(GeneratedKeyHolder.class));
        
        // 5. ADMIN 查询
        doReturn(List.of()).when(jdbc).queryForList(
                eq("SELECT id FROM users WHERE role='ADMIN' AND deleted=0 ORDER BY id FOR UPDATE"),
                eq(Long.class));
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> r = new HashMap<>();
        r.put("employeeNo", "EMP001");
        r.put("username", "EMP001");
        r.put("password", "Pass123");
        r.put("role", "EMPLOYEE");
        r.put("realName", "张三");
        r.put("email", "zhangsan@test.com");
        r.put("phone", "13800138000");
        r.put("idCard", "110101199001011237"); // 有效的身份证测试号（校验码正确）
        r.put("birthday", "1990-01-01");
        r.put("departmentId", 1);
        r.put("teamId", 1);
        return r;
    }

    private BusinessException fail(Map<String, Object> r) {
        return assertThrows(BusinessException.class, () -> svc.create(r));
    }

    // ============== 测试用例 ==============

    @Test
    void 人员创建_正常创建员工_预期成功() {
        Map<String, Object> out = svc.create(validRequest());
        assertEquals("EMP001", out.get("employeeNo"));
        assertEquals("EMP001", out.get("username"));
        assertNotNull(out.get("id"));
    }

    @Test
    void 人员创建_工号最小长度1_预期成功() {
        Map<String, Object> r = validRequest();
        r.put("employeeNo", "A");
        r.put("username", "A");
        assertNotNull(svc.create(r));
    }

    @Test
    void 人员创建_工号最大长度64_预期成功() {
        Map<String, Object> r = validRequest();
        r.put("employeeNo", "a".repeat(64));
        r.put("username", "a".repeat(64));
        assertNotNull(svc.create(r));
    }

    @Test
    void 人员创建_密码最小长度6_预期成功() {
        Map<String, Object> r = validRequest();
        r.put("password", "123456");
        assertNotNull(svc.create(r));
    }

    @Test
    void 人员创建_密码最大长度16_预期成功() {
        Map<String, Object> r = validRequest();
        r.put("password", "a".repeat(16));
        assertNotNull(svc.create(r));
    }

    @Test
    void 人员创建_真实姓名最大长度15字_预期成功() {
        Map<String, Object> r = validRequest();
        r.put("realName", "张".repeat(15));
        assertNotNull(svc.create(r));
    }

    @Test
    void 人员创建_工号为空_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("employeeNo", "");
        assertEquals(ErrorCode.MISSING_REQUIRED_FIELD, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_工号含特殊字符_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("employeeNo", "EMP@001");
        r.put("username", "EMP@001");
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_工号与username不一致_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("username", "EMP002");
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_密码过短_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("password", "123");
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_密码超长UTF8_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("password", "a".repeat(73));
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_角色无效_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("role", "INVALID");
        assertEquals(ErrorCode.INVALID_ROLE, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_角色为ADMIN_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("role", "ADMIN");
        assertEquals(ErrorCode.INVALID_ROLE, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_真实姓名为空_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("realName", "");
        assertEquals(ErrorCode.MISSING_REQUIRED_FIELD, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_真实姓名超长_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("realName", "张".repeat(16));
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_邮箱格式错误_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("email", "invalid-email");
        assertEquals(ErrorCode.INVALID_EMAIL_FORMAT, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_邮箱超长_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("email", "a".repeat(129) + "@test.com");
        assertEquals(ErrorCode.INVALID_EMAIL_FORMAT, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_手机号格式错误_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("phone", "1234567890");
        assertEquals(ErrorCode.INVALID_PHONE_FORMAT, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_身份证格式错误_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("idCard", "12345678901234567A"); // 17位+A，A不是X也不是数字
        assertEquals(ErrorCode.INVALID_ID_CARD, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_生日格式错误_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("birthday", "1990/01/01");
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_生日晚于今天_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("birthday", "2099-12-31");
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_部门不存在_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("departmentId", 99999);
        // 覆盖部门查询返回空列表
        doReturn(List.of()).when(jdbc).queryForList(
                eq("SELECT id FROM departments WHERE id=? AND deleted=0"),
                eq(Long.class),
                anyLong());
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_团队不属于部门_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("departmentId", 1);
        r.put("teamId", 999);
        // 覆盖团队查询返回空列表
        doReturn(List.of()).when(jdbc).queryForList(
                eq("SELECT id FROM teams WHERE id=? AND department_id=? AND deleted=0"),
                eq(Long.class),
                anyLong(),
                anyLong());
        assertEquals(ErrorCode.BAD_REQUEST, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_工号已存在_预期拒绝() {
        Map<String, Object> r = validRequest();
        // 覆盖 username 的 COUNT(*) 查询返回 1
        doReturn(1).when(jdbc).queryForObject(
                argThat(sql -> sql.contains("username")),
                eq(Integer.class),
                any());
        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_邮箱已存在_预期拒绝() {
        Map<String, Object> r = validRequest();
        // 覆盖 email 的 COUNT(*) 查询返回 1
        doReturn(1).when(jdbc).queryForObject(
                argThat(sql -> sql.contains("email")),
                eq(Integer.class),
                any());
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_身份证已存在_预期拒绝() {
        Map<String, Object> r = validRequest();
        // 覆盖 id_card 的 COUNT(*) 查询返回 1
        doReturn(1).when(jdbc).queryForObject(
                argThat(sql -> sql.contains("id_card")),
                eq(Integer.class),
                any());
        assertEquals(ErrorCode.ID_CARD_ALREADY_EXISTS, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_缺少部门ID_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("role", "DEPT_HEAD");
        r.remove("departmentId");
        r.remove("teamId");
        assertEquals(ErrorCode.MISSING_REQUIRED_FIELD, fail(r).getErrorCode());
    }

    @Test
    void 人员创建_缺少团队ID_预期拒绝() {
        Map<String, Object> r = validRequest();
        r.put("role", "TEAM_LEADER");
        r.remove("teamId");
        assertEquals(ErrorCode.MISSING_REQUIRED_FIELD, fail(r).getErrorCode());
    }
}