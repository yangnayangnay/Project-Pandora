package com.pandora.backend.notification;

import com.pandora.backend.common.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationAccessTest {
    JdbcTemplate db;
    MockMvc http;
    @BeforeEach void setup() {
        db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:notice-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        db.execute("CREATE TABLE notifications(id BIGINT PRIMARY KEY,target_user_id BIGINT,is_read INT,content VARCHAR(100),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        db.update("INSERT INTO notifications(id,target_user_id,is_read,content) VALUES(1,10,0,'我的未读'),(2,10,1,'我的已读'),(3,20,0,'他人的通知')");
        var controller = new NotificationController(); ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        http = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(10L, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void allNotificationsIncludeReadAndUnreadButNeverAnotherUsersRecords() throws Exception {
        http.perform(get("/notification/list").param("unreadOnly", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        http.perform(get("/notification/list").param("unreadOnly", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }
    @Test void markingReadCannotModifyAnotherUsersNotice() throws Exception {
        http.perform(put("/notification/3/read")).andExpect(status().isNotFound());
        assertEquals(0, db.queryForObject("SELECT is_read FROM notifications WHERE id=3", Integer.class));
        http.perform(put("/notification/1/read")).andExpect(status().isOk());
        assertEquals(1, db.queryForObject("SELECT is_read FROM notifications WHERE id=1", Integer.class));
    }
}
