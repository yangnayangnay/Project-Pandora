package com.pandora.backend.notification;

import com.pandora.backend.hierarchy.HierarchyService;
import com.pandora.backend.log.LogCommentService;
import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LogCommentNotificationTest {
    JdbcTemplate db; LogCommentService comments; NotificationController notifications; TransactionTemplate tx;
    @BeforeEach void setup() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:log-comment-notice-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/db/h2_schema.sql")).execute(source);
        db=new JdbcTemplate(source); db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,real_name,password_hash,role,approval_status) VALUES(1,'leader','上级','hash','TEAM_LEADER','APPROVED'),(2,'employee','下属','hash','EMPLOYEE','APPROVED'),(3,'peer','同事','hash','EMPLOYEE','APPROVED'),(4,'admin','管理员','hash','ADMIN','APPROVED')");
        db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(1,2)");
        db.update("INSERT INTO work_logs(id,user_id,work_item,log_date) VALUES(90,2,'11月23日日志','2026-11-23')");
        var hierarchy=new HierarchyService(); ReflectionTestUtils.setField(hierarchy,"jdbcTemplate",db); ReflectionTestUtils.setField(hierarchy,"maxDepth",10);
        comments=new LogCommentService(db,hierarchy,new NotificationService(db));
        tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        notifications=new NotificationController(); ReflectionTestUtils.setField(notifications,"jdbcTemplate",db);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(2L,null,List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void superiorCommentCreatesOneOwnerOnlyNotificationAndCarriesLogLink() {
        tx.execute(s -> comments.add(90,1,"请补充复测结论"));
        var rows=notifications.listNotifications(false).getBody().getData();
        assertEquals(1,rows.size()); var row=rows.get(0);
        assertEquals("LOG_COMMENTED",row.get("type")); assertEquals(2L,row.get("target_user_id"));
        assertEquals(90L,row.get("log_id")); assertNull(row.get("task_id"));
        assertTrue(row.get("content").toString().contains("上级")); assertTrue(row.get("content").toString().contains("请补充复测结论"));
        assertEquals(1,notifications.pollNotifications(0L).getBody().getData().size());
        notifications.markAsRead(((Number)row.get("id")).longValue());
        assertTrue(notifications.listNotifications(true).getBody().getData().isEmpty());
    }
    @Test void disabledPreferencePersistsWhileCommentStillAppearsInTheInbox() {
        assertEquals(true,notifications.getPreferences().getBody().getData().get("LOG_COMMENTED"));
        notifications.updatePreferences(Map.of("LOG_COMMENTED",false));
        assertEquals(false,notifications.getPreferences().getBody().getData().get("LOG_COMMENTED"));
        tx.execute(s -> comments.add(90,1,"关闭弹窗后仍可查看评论通知"));
        assertEquals(1,notifications.listNotifications(false).getBody().getData().size());
    }
    @Test void rejectedSelfPeerAndEmptyCommentsNeverGenerateNotifications() {
        for(long author:List.of(2L,3L)) assertThrows(BusinessException.class,()->tx.execute(s->comments.add(90,author,"越权评论")));
        assertThrows(BusinessException.class,()->tx.execute(s->comments.add(90,1," ")));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class));
    }
    @Test void commentAndNotificationRollbackTogetherAndLongUnicodePreviewFits() {
        tx.execute(s->{ comments.add(90,1,"尚未提交"); s.setRollbackOnly(); return null; });
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM log_comments",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class));
        tx.execute(s->comments.add(90,1,"核查😀".repeat(300)));
        assertTrue(db.queryForObject("SELECT content FROM notifications",String.class).length()<256);
    }
    @Test void hiddenAdministratorCommentsDoNotGenerateMobileNotifications() {
        tx.execute(s->comments.add(90,4,"后台协助记录"));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM log_comments",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class));
    }
    @Test void oldNotificationSchemaUpgradesTwiceWithoutLosingHistory() {
        db.execute("ALTER TABLE notifications DROP COLUMN log_id");
        db.update("INSERT INTO notifications(target_user_id,type,content) VALUES(2,'NEW_TASK','原通知')");
        var upgrade=new NotificationSchemaUpgrade(db); upgrade.run(null); upgrade.run(null);
        tx.execute(s->comments.add(90,1,"升级后的评论"));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class));
        assertEquals("原通知",db.queryForObject("SELECT content FROM notifications WHERE type='NEW_TASK'",String.class));
    }
}
