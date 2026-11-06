package com.pandora.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.common.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserSelfServicePolicyTest {
    private JdbcTemplate db;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:account-policy-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source); db.update("DELETE FROM users");
        var encoder = new BCryptPasswordEncoder(4);
        db.update("INSERT INTO users (id,username,password_hash,real_name,role,approval_status,email,phone,avatar) "
                        + "VALUES (1,'000123',?,'姓名甲','EMPLOYEE','APPROVED','original@example.com','13800138000','/uploads/original.jpg')",
                encoder.encode("old-test-value"));
        var controller = new UserController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "passwordEncoder", encoder);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        // Authority/client hints must not override the account's actual role in the database.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private Map<String, Object> account() {
        return db.queryForMap("SELECT username AS \"username\",real_name AS \"realName\",email AS \"email\","
                + "phone AS \"phone\",password_hash AS \"passwordHash\",avatar AS \"avatar\" FROM users WHERE id=1");
    }

    @ParameterizedTest @ValueSource(strings = {"FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"})
    void allMobileRolesCannotModifyProtectedFieldsOrSmuggleAnAvatarUpdate(String role) throws Exception {
        db.update("UPDATE users SET role=? WHERE id=1", role);
        Map<String, Object> before = account();
        for (String field : List.of("username", "employeeNo", "realName", "email", "phone", "password", "real_name", "employee_no", "password_hash")) {
            mvc.perform(put("/user/me").header("X-Client-Type", "WEB").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(field, "replacement", "avatar", "/uploads/changed.jpg"))))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
            assertEquals(before, account(), "Rejected field must leave the complete account unchanged: " + field);
        }
    }

    @Test void nullAndEmptyValuesCannotClearProtectedFields() throws Exception {
        Map<String, Object> before = account();
        for (String field : List.of("username", "realName", "email", "phone")) {
            for (Object value : new Object[]{null, ""}) {
                Map<String, Object> request = new HashMap<>(); request.put(field, value);
                mvc.perform(put("/user/me").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
                assertEquals(before, account());
            }
        }
    }

    @ParameterizedTest @ValueSource(strings = {"FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"})
    void correctOldPasswordDoesNotAllowMobileUserToChangePassword(String role) throws Exception {
        db.update("UPDATE users SET role=? WHERE id=1", role);
        Map<String, Object> before = account();
        mvc.perform(put("/user/me/password").header("X-Client-Type", "WEB").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("oldPassword", "old-test-value", "newPassword", "new-test-value"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        assertEquals(before, account());
    }

    @ParameterizedTest @ValueSource(strings = {"FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"})
    void avatarOnlyUpdateRemainsAvailableToEveryMobileRole(String role) throws Exception {
        db.update("UPDATE users SET role=? WHERE id=1", role);
        Map<String, Object> expected = new HashMap<>(account()); expected.put("avatar", "/uploads/new.jpg");
        mvc.perform(put("/user/me").contentType(MediaType.APPLICATION_JSON).content("{\"avatar\":\"/uploads/new.jpg\"}"))
                .andExpect(status().isOk());
        assertEquals(expected, account());
    }

    @Test void adminWebContactMaintenanceStillWorks() throws Exception {
        db.update("UPDATE users SET role='ADMIN' WHERE id=1");
        mvc.perform(put("/user/me").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "username", "000123", "realName", "姓名甲", "email", "new@example.com", "phone", "13900139000"))))
                .andExpect(status().isOk());
        assertEquals("new@example.com", account().get("email"));
        assertEquals("13900139000", account().get("phone"));
        assertEquals("000123", account().get("username"));
    }

    @Test void profileRemainsReadableWithoutExposingPassword() throws Exception {
        mvc.perform(get("/user/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("000123"))
                .andExpect(jsonPath("$.data.realName").value("姓名甲"))
                .andExpect(jsonPath("$.data.email").value("original@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }
}
