package com.pandora.backend.top10;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.permission.FunctionPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDispatchAssigneeTest {
    private JdbcTemplate db;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:company-dispatch-assignee;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(dataSource);
        db = new JdbcTemplate(dataSource);
        db.update("DELETE FROM top10_works");
        db.update("DELETE FROM tasks");
        db.update("DELETE FROM users");
        db.update("INSERT INTO users (id, username, password_hash, role, approval_status) " +
                "VALUES (1, 'admin1', 'hash', 'ADMIN', 'APPROVED')");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                2L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAndMarkCompanyDispatchRejectsAdminAssignee() {
        Top10Controller controller = new Top10Controller();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "functionPermissionService",
                new FunctionPermissionService() {
                    @Override
                    public boolean check(Long userId, String functionCode) {
                        return true;
                    }
                });

        Map<String, Object> request = new HashMap<>();
        request.put("name", "管理员派发");
        request.put("assigneeId", 1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> controller.createAndMarkCompanyDispatch(request));

        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertTrue(error.getMessage().contains("管理员"));
        assertEquals(0L, db.queryForObject("SELECT COUNT(*) FROM tasks", Long.class));
    }
}
