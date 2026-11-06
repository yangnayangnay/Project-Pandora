package com.pandora.backend.user;

import com.pandora.backend.auth.AuthController;
import com.pandora.backend.auth.JwtUtil;
import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountCreationTest {
    private JdbcTemplate db;
    private AccountCreationService accounts;
    private UserController users;
    private AuthController auth;
    private MockMvc http;
    private BCryptPasswordEncoder encoder;

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:employee-accounts-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source);
        db.update("DELETE FROM users");
        encoder = new BCryptPasswordEncoder();
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status,email) VALUES (1,'admin-existing',?,'ADMIN','APPROVED','admin@example.test')", encoder.encode("admin-password"));
        db.update("INSERT INTO departments (id,name) VALUES (10,'研发'),(20,'产品')");
        db.update("INSERT INTO teams (id,name,department_id) VALUES (11,'客户端',10),(21,'设计',20)");
        accounts = new AccountCreationService(db, encoder);
        users = new UserController();
        ReflectionTestUtils.setField(users, "jdbcTemplate", db);
        ReflectionTestUtils.setField(users, "accountCreationService", accounts);
        auth = new AuthController();
        ReflectionTestUtils.setField(auth, "jdbcTemplate", db);
        ReflectionTestUtils.setField(auth, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(auth, "sessionStore", new SessionStore());
        var jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "test-only-employee-account-secret-0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
        ReflectionTestUtils.setField(jwt, "refreshExpiration", 120000L);
        ReflectionTestUtils.setField(auth, "jwtUtil", jwt);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        http = MockMvcBuilders.standaloneSetup(users, auth).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private Map<String, Object> complete() {
        return new HashMap<>(Map.of("employeeNo", "000123", "password", " employee-password ", "role", "EMPLOYEE", "realName", "测试人员",
                "email", "employee@example.test", "phone", "13800000000", "idCard", "11010119900101001x", "birthday", "1990-01-01", "departmentId", 10, "teamId", 11));
    }
    private void rejects(Map<String, Object> request, ErrorCode expected) {
        int before = db.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(expected, assertThrows(BusinessException.class, () -> accounts.create(request)).getErrorCode());
        assertEquals(before, db.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
    }

    @SuppressWarnings("unchecked") @Test void creationPersistsAllRequiredFieldsAndEmployeeNumberLogsIntoApp() throws Exception {
        var request = complete();
        request.put("employeeNo", " 000123 "); request.put("username", "000123");
        http.perform(post("/user/create").contentType("application/json").content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.employeeNo").value("000123"));
        var row = db.queryForMap("SELECT * FROM users WHERE username='000123'");
        assertEquals("测试人员", row.get("real_name")); assertEquals("employee@example.test", row.get("email"));
        assertEquals("13800000000", row.get("phone")); assertEquals("11010119900101001X", row.get("id_card"));
        assertEquals(LocalDate.of(1990, 1, 1), ((java.sql.Date) row.get("birthday")).toLocalDate());
        assertEquals(10L, row.get("department_id")); assertEquals(11L, row.get("team_id"));
        assertEquals("APPROVED", row.get("approval_status"));
        assertTrue(encoder.matches(" employee-password ", (String) row.get("password_hash")));
        http.perform(post("/auth/login").contentType("application/json")
                        .content("{\"employeeNo\":\"000123\",\"password\":\" employee-password \",\"clientType\":\"APP\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.employeeNo").value("000123"))
                .andExpect(jsonPath("$.data.user.departmentId").value(10)).andExpect(jsonPath("$.data.user.teamId").value(11));
    }

    @ParameterizedTest @ValueSource(strings={"employeeNo","password","role","realName","email","phone","idCard","birthday","departmentId","teamId"})
    void missingRequiredFieldCannotCreateAPartialAccount(String field) {
        var request = complete(); request.remove(field);
        rejects(request, field.equals("password") ? ErrorCode.PASSWORD_TOO_SHORT : ErrorCode.MISSING_REQUIRED_FIELD);
        request.put(field, " ");
        rejects(request, field.equals("password") ? ErrorCode.PASSWORD_TOO_SHORT : ErrorCode.MISSING_REQUIRED_FIELD);
    }
    @Test void validatesContactIdentityDateAndEmployeeNumberInputs() {
        var cases = List.of(new Object[]{"email","invalid",ErrorCode.INVALID_EMAIL_FORMAT},
                new Object[]{"phone","123",ErrorCode.INVALID_PHONE_FORMAT}, new Object[]{"idCard","123",ErrorCode.INVALID_ID_CARD},
                new Object[]{"birthday","1990-02-30",ErrorCode.BAD_REQUEST},new Object[]{"birthday","2999-01-01",ErrorCode.BAD_REQUEST},
                new Object[]{"employeeNo","bad number",ErrorCode.BAD_REQUEST},new Object[]{"employeeNo",123,ErrorCode.BAD_REQUEST},
                new Object[]{"departmentId",10.5,ErrorCode.BAD_REQUEST},new Object[]{"teamId",-1,ErrorCode.BAD_REQUEST},
                new Object[]{"role","ROOT",ErrorCode.INVALID_ROLE});
        for (Object[] item : cases) { var request = complete(); request.put((String) item[0], item[1]); rejects(request, (ErrorCode) item[2]); }
    }
    @Test void validatesOrganizationExistsIsActiveAndTeamBelongsToDepartment() {
        var request = complete(); request.put("departmentId", 99); rejects(request, ErrorCode.BAD_REQUEST);
        request = complete(); request.put("teamId", 21); rejects(request, ErrorCode.BAD_REQUEST);
        request = complete(); db.update("UPDATE teams SET deleted=1 WHERE id=11"); rejects(request, ErrorCode.BAD_REQUEST);
        db.update("UPDATE teams SET deleted=0 WHERE id=11"); db.update("UPDATE departments SET deleted=1 WHERE id=10");
        rejects(complete(), ErrorCode.BAD_REQUEST);
    }
    @Test void rejectsDuplicateEmployeeNumberEmailAndIdentity() {
        accounts.create(complete());
        rejects(complete(), ErrorCode.USERNAME_ALREADY_EXISTS);
        var request = complete(); request.put("employeeNo", "000124"); request.put("email", "EMPLOYEE@example.test");
        rejects(request, ErrorCode.EMAIL_ALREADY_EXISTS);
        request.put("email", "other@example.test"); rejects(request, ErrorCode.ID_CARD_ALREADY_EXISTS);
        db.update("UPDATE users SET deleted=1 WHERE username='000123'");
        rejects(complete(), ErrorCode.USERNAME_ALREADY_EXISTS);
    }
    @Test void legacyUsernameFieldAndStringOrganizationIdsRemainCompatible() {
        var request = complete(); request.remove("employeeNo"); request.put("username", "000125");
        request.put("departmentId", "10"); request.put("teamId", "11");
        var result = accounts.create(request); assertEquals("000125", result.get("employeeNo"));
        assertEquals("000125", db.queryForObject("SELECT username FROM users WHERE id=?", String.class, result.get("id")));
    }
    @Test void rejectsConflictingEmployeeNumberAndUsername() {
        var request = complete(); request.put("username", "other"); rejects(request, ErrorCode.BAD_REQUEST);
    }
    @Test void nonAdminCannotCreateAccountEvenWithCompletePayload() {
        db.update("UPDATE users SET role='FOUNDER' WHERE id=1");
        var response = users.createUser(complete()); assertEquals(403, response.getStatusCode().value());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
    }
}
