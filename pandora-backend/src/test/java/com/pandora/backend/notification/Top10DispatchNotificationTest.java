package com.pandora.backend.notification;

import com.pandora.backend.top10.Top10Controller;
import com.pandora.backend.permission.FunctionPermissionService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class Top10DispatchNotificationTest {
    JdbcTemplate db; Top10Controller controller;
    @BeforeEach void setup() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:dispatch-notice-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/db/h2_schema.sql")).execute(source);
        db=new JdbcTemplate(source); db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES(1,'admin','hash','ADMIN','APPROVED'),(2,'founder','hash','FOUNDER','APPROVED'),(3,'employee','hash','EMPLOYEE','APPROVED')");
        db.update("INSERT INTO top10_works(id,user_id,work_type,rank_order,content) VALUES(10,1,'COMPANY_DISPATCH',1,'原内容')");
        controller=new Top10Controller();
        var permissions=mock(FunctionPermissionService.class); when(permissions.check(anyLong(),anyString())).thenReturn(true);
        ReflectionTestUtils.setField(controller,"jdbcTemplate",db);
        ReflectionTestUtils.setField(controller,"functionPermissionService",permissions);
        ReflectionTestUtils.setField(controller,"notificationService",new NotificationService(db));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L,null,List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void editingDispatchCreatesExactlyOneNotificationForTheOtherFounder() {
        controller.updateCompanyDispatch(10L,Map.of("content","修改内容"));
        assertEquals("修改内容",db.queryForObject("SELECT content FROM top10_works WHERE id=10",String.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM notifications WHERE type='TOP10_DISPATCH_CHANGED' AND target_user_id=2",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class));
    }
}
