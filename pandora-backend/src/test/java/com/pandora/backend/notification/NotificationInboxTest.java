package com.pandora.backend.notification;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationInboxTest {
    JdbcTemplate db; NotificationInbox inbox;
    @BeforeEach void setup() {
        db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:inbox-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("CREATE TABLE notifications(id BIGINT PRIMARY KEY,target_user_id BIGINT,is_read INT DEFAULT 0,content VARCHAR(100))");
        inbox=new NotificationInbox(db);
    }
    @Test void unreadCountIncludesOldUnreadEvenWhenDeliveryCursorAlreadyAdvancedAndIsAccountScoped() {
        db.update("INSERT INTO notifications(id,target_user_id,is_read) VALUES(1,10,0),(2,10,1),(99,20,0)");
        var result=inbox.snapshot(10,2);
        assertEquals(1L,result.getUnreadCount()); assertEquals(2L,result.getMaxNotificationId()); assertTrue(result.getData().isEmpty());
    }
    @Test void cursorFromAnotherAccountOrDatabaseIsResetWithoutLosingUnread() {
        db.update("INSERT INTO notifications(id,target_user_id) VALUES(3,10),(99,20)");
        var result=inbox.snapshot(10,999);
        assertEquals(3L,((Number)result.getData().get(0).get("id")).longValue());
        assertEquals(3L,result.getMaxNotificationId());
    }
    @Test void batchesDrainInIdOrderWithoutSkippingOverflow() {
        for(int id=1;id<=130;id++) db.update("INSERT INTO notifications(id,target_user_id) VALUES(?,10)",id);
        var first=inbox.snapshot(10,0); assertEquals(100,first.getData().size());
        assertEquals(1L,((Number)first.getData().get(0).get("id")).longValue());
        var next=inbox.snapshot(10,100); assertEquals(30,next.getData().size()); assertEquals(130L,next.getUnreadCount());
    }
    @Test void aCommittedNewNotificationWakesOnlyTheCorrectAccountsWait() {
        var first=inbox.await(10,0,0,25); var other=inbox.await(20,0,0,25);
        assertFalse(first.hasResult()); assertFalse(other.hasResult());
        db.update("INSERT INTO notifications(id,target_user_id) VALUES(1,10)"); inbox.deliverChanges();
        assertTrue(first.hasResult()); assertFalse(other.hasResult());
    }
    @Test void readChangesWakeTheBadgeEvenWithoutAnyNewNotification() {
        db.update("INSERT INTO notifications(id,target_user_id) VALUES(1,10)");
        var waiting=inbox.await(10,1,1,25); assertFalse(waiting.hasResult());
        db.update("UPDATE notifications SET is_read=1 WHERE id=1"); inbox.deliverChanges();
        assertTrue(waiting.hasResult()); assertEquals(0L,inbox.snapshot(10,1).getUnreadCount());
    }
    @Test void notificationRollbacksDoNotWakeWaitingClients() {
        var waiting=inbox.await(10,0,0,25);
        var tx=new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(db.getDataSource()));
        tx.executeWithoutResult(s->{db.update("INSERT INTO notifications(id,target_user_id) VALUES(1,10)");s.setRollbackOnly();});
        inbox.deliverChanges(); assertFalse(waiting.hasResult());
    }
}
