package com.pandora.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.auth.AuthController;
import com.pandora.backend.auth.JwtUtil;
import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.*;

import javax.sql.DataSource;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.TestExecutionListeners({
        org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
        org.springframework.test.context.transaction.TransactionalTestExecutionListener.class
})
@SpringJUnitConfig(AccountProfileTest.Config.class)
@Transactional
class AccountProfileTest {
    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    static class Config {
        @Bean DataSource source() {
            var source = new DriverManagerDataSource("jdbc:h2:mem:admin-profile;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
            return source;
        }
        @Bean JdbcTemplate db(DataSource source) { return new JdbcTemplate(source); }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean AccountProfileService service(JdbcTemplate db) { return new AccountProfileService(db); }
        @Bean AccountProfileController controller(AccountProfileService service) { return new AccountProfileController(service); }
    }

    @Autowired JdbcTemplate db;
    @Autowired AccountProfileService service;
    @Autowired AccountProfileController controller;
    private MockMvc http;
    private final ObjectMapper json = new ObjectMapper();
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);

    @BeforeEach void setup() {
        db.update("DELETE FROM user_hierarchy"); db.update("DELETE FROM users");
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status) VALUES (1,'admin-existing',?,'ADMIN','APPROVED')", passwords.encode("admin-secret"));
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status,real_name,email,phone,id_card,birthday,avatar,department_id,team_id) "
                + "VALUES (2,'000123',?,'EMPLOYEE','APPROVED','姓名甲','first@example.test','13800000000','11010119900101001X','1990-01-01','/uploads/test.jpg',10,11)", passwords.encode("employee-secret"));
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status,email,id_card) "
                + "VALUES (3,'000456','hash','EMPLOYEE','APPROVED','other@example.test','11010119900101002X')");
        db.update("INSERT INTO user_hierarchy (superior_id,subordinate_id) VALUES (1,2)");
        signIn(1, "ADMIN");
        var auth = new AuthController();
        ReflectionTestUtils.setField(auth, "jdbcTemplate", db);
        ReflectionTestUtils.setField(auth, "passwordEncoder", passwords);
        ReflectionTestUtils.setField(auth, "sessionStore", new SessionStore());
        var jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "test-only-profile-edit-secret-0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
        ReflectionTestUtils.setField(jwt, "refreshExpiration", 120000L);
        ReflectionTestUtils.setField(auth, "jwtUtil", jwt);
        http = MockMvcBuilders.standaloneSetup(controller, auth).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private void signIn(long id, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    private Map<String, Object> account(long id) {
        Map<String, Object> account = new LinkedHashMap<>();
        db.queryForMap("SELECT * FROM users WHERE id=?", id).forEach((key, value) -> account.put(key.toLowerCase(Locale.ROOT), value));
        return account;
    }
    private Map<String, Object> edits() {
        return new LinkedHashMap<>(Map.of("employeeNo", " 000789 ", "realName", " 姓名乙 ", "email", " new@example.test ",
                "phone", "13900000000", "idCard", "11010119900202003x", "birthday", "1992-02-29"));
    }
    private org.springframework.test.web.servlet.ResultActions putProfile(long id, Map<String, Object> request) throws Exception {
        return http.perform(put("/user/" + id + "/profile").principal(SecurityContextHolder.getContext().getAuthentication())
                .contentType("application/json").content(json.writeValueAsString(request)));
    }

    @Test void administratorCanMaintainAllSixFieldsAndLoginUsesTheUpdatedEmployeeNumber() throws Exception {
        var original = account(2);
        putProfile(2, edits()).andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("000789"))
                .andExpect(jsonPath("$.data.birthday").value("1992-02-29"))
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
        var saved = account(2);
        assertEquals("姓名乙", saved.get("real_name")); assertEquals("new@example.test", saved.get("email"));
        assertEquals("13900000000", saved.get("phone")); assertEquals("11010119900202003X", saved.get("id_card"));
        assertEquals("1992-02-29", saved.get("birthday").toString());
        for (String key : List.of("id", "role", "password_hash", "approval_status", "avatar", "department_id", "team_id", "created_at"))
            assertEquals(original.get(key), saved.get(key), key);
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM user_hierarchy WHERE superior_id=1 AND subordinate_id=2", Integer.class));
        http.perform(post("/auth/login").contentType("application/json")
                        .content("{\"employeeNo\":\"000789\",\"password\":\"employee-secret\",\"clientType\":\"APP\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.employeeNo").value("000789"));
        http.perform(post("/auth/login").contentType("application/json")
                        .content("{\"employeeNo\":\"000123\",\"password\":\"employee-secret\",\"clientType\":\"APP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void unchangedOwnValuesAreAcceptedAndDoNotRewriteTimestamps() throws Exception {
        var before = account(2);
        putProfile(2, Map.of("employeeNo", "000123", "realName", "姓名甲", "email", "first@example.test", "phone", "13800000000",
                "idCard", "11010119900101001X", "birthday", "1990-01-01")).andExpect(status().isOk());
        assertEquals(before, account(2));
        putProfile(2, Map.of("email", "FIRST@example.test", "idCard", "11010119900101001x")).andExpect(status().isOk());
    }

    @Test void partialUpdatesPreserveMissingLegacyFieldsAndUnrelatedAccountData() throws Exception {
        db.update("UPDATE users SET email=NULL,phone=NULL,id_card=NULL,birthday=NULL WHERE id=2");
        var before = account(2);
        putProfile(2, Map.of("realName", "补充姓名")).andExpect(status().isOk());
        var expected = new HashMap<>(before); expected.put("real_name", "补充姓名"); expected.remove("updated_at");
        var actual = account(2); actual.remove("updated_at"); assertEquals(expected, actual);
    }

    @ParameterizedTest @ValueSource(strings={"employeeNo","realName","email","phone","idCard","birthday"})
    void cannotClearAnExistingRequiredField(String field) throws Exception {
        var before = account(2);
        for (Object value : new Object[]{null, "", " "}) {
            var request = new HashMap<String, Object>(); request.put(field, value); request.put("realName", "不应部分保存");
            if (field.equals("realName")) request.put(field, value);
            putProfile(2, request).andExpect(status().isBadRequest()); assertEquals(before, account(2));
        }
    }

    @Test void rejectsInvalidFormatsAndTypesWithoutPartialWrites() throws Exception {
        var invalid = List.of(new Object[]{"employeeNo", "bad number"}, new Object[]{"employeeNo", 123},
                new Object[]{"employeeNo", "x".repeat(65)}, new Object[]{"realName", "名".repeat(65)},
                new Object[]{"email", "invalid"}, new Object[]{"email", "x".repeat(120) + "@example.test"},
                new Object[]{"phone", "123"}, new Object[]{"idCard", "123"}, new Object[]{"birthday", "1990-02-30"},
                new Object[]{"birthday", "2999-01-01"}, new Object[]{"birthday", "0000-01-01"},
                new Object[]{"birthday", "1990-1-1"}, new Object[]{"email", Map.of("value", "a@example.test")});
        var before = account(2);
        for (Object[] entry : invalid) {
            var request = edits(); request.put((String) entry[0], entry[1]);
            putProfile(2, request).andExpect(status().isBadRequest()); assertEquals(before, account(2));
        }
    }

    @Test void duplicateIdentifiersAreRejectedAndDeletedUsernamesStayReserved() throws Exception {
        var before = account(2);
        for (var entry : Map.of("employeeNo", "000456", "email", "OTHER@example.test", "idCard", "11010119900101002x").entrySet()) {
            var request = edits(); request.put(entry.getKey(), entry.getValue());
            putProfile(2, request).andExpect(status().isBadRequest()); assertEquals(before, account(2));
        }
        db.update("UPDATE users SET deleted=1 WHERE id=3");
        putProfile(2, Map.of("employeeNo", "000456")).andExpect(status().isBadRequest());
        assertEquals(before, account(2));
    }

    @ParameterizedTest @ValueSource(strings={"FOUNDER","DEPT_HEAD","TEAM_LEADER","EMPLOYEE"})
    void nonAdministratorsCannotEditEvenTheirOwnProfile(String role) throws Exception {
        db.update("UPDATE users SET role=? WHERE id=2", role); signIn(2, role);
        var before = account(2);
        putProfile(2, edits()).andExpect(status().isForbidden()); assertEquals(before, account(2));
    }

    @Test void forgedAdminAuthorityCannotOverrideTheDatabaseRole() throws Exception {
        signIn(2, "ADMIN"); var before = account(2);
        putProfile(2, edits()).andExpect(status().isForbidden()); assertEquals(before, account(2));
    }

    @Test void inactiveAdministratorCannotWrite() throws Exception {
        var before = account(2);
        db.update("UPDATE users SET approval_status='REJECTED' WHERE id=1");
        putProfile(2, edits()).andExpect(status().isForbidden()); assertEquals(before, account(2));
        db.update("UPDATE users SET approval_status='APPROVED',deleted=1 WHERE id=1");
        putProfile(2, edits()).andExpect(status().isForbidden()); assertEquals(before, account(2));
    }

    @ParameterizedTest @ValueSource(strings={"password","password_hash","role","departmentId","teamId","approval_status","avatar","username","id"})
    void rejectsUnknownAndProtectedFieldsInsteadOfMassAssignment(String field) throws Exception {
        var before = account(2); var request = edits(); request.put(field, "injected");
        putProfile(2, request).andExpect(status().isBadRequest()); assertEquals(before, account(2));
    }

    @Test void missingOrDeletedTargetCannotBeEdited() throws Exception {
        putProfile(99999, edits()).andExpect(status().isNotFound());
        db.update("UPDATE users SET deleted=1 WHERE id=2"); var before = account(2);
        putProfile(2, edits()).andExpect(status().isNotFound()); assertEquals(before, account(2));
    }

    @Test void adminCanUpdateOwnContactsWithoutRequiringLegacyMissingFields() throws Exception {
        putProfile(1, Map.of("email", "admin-new@example.test", "phone", "13800000001")).andExpect(status().isOk());
        assertEquals("admin-existing", account(1).get("username")); assertNull(account(1).get("birthday"));
    }
}
