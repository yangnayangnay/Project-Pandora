package com.example.project_pandora.notification;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class NotificationContentTest {
    @Test public void commentNotificationLinksToTheLogInsteadOfATask() {
        assertEquals(Long.valueOf(23), NotificationContent.logId(Map.of("type","LOG_COMMENTED","log_id",23.0,"task_id",99)));
        assertEquals(Long.valueOf(24), NotificationContent.logId(Map.of("TYPE","LOG_COMMENTED","LOG_ID",24)));
        assertNull(NotificationContent.logId(Map.of("type","NEW_TASK","task_id",99,"log_id",23)));
        assertNull(NotificationContent.logId(Map.of("type","LOG_COMMENTED","log_id",-1)));
    }
    @Test public void commentTypeHasDistinctLabelsAndParticipatesInPreferenceFiltering() {
        assertTrue(Arrays.asList(NotificationContent.TYPES).contains("LOG_COMMENTED"));
        assertEquals("上级评论日志", NotificationContent.listTitle("LOG_COMMENTED"));
        assertEquals("上级评论了你的日志", NotificationContent.systemTitle("LOG_COMMENTED"));
        assertEquals("任务通知", NotificationContent.systemTitle("NEW_TASK"));
    }
}
