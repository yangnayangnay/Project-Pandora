package com.example.project_pandora.notification;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationDeliveryTest {
    @Test public void accountsNeverShareDeliveryOrPreferenceStorage() {
        assertNotEquals(NotificationDelivery.preferencesName(10),NotificationDelivery.preferencesName(20));
    }
    @Test public void failedAlertRemainsPendingUntilSuccessfulRetry() {
        long cursor=10;
        cursor=NotificationDelivery.acknowledge(cursor,11,false); assertEquals(10,cursor);
        cursor=NotificationDelivery.acknowledge(cursor,11,true); assertEquals(11,cursor);
        assertEquals(11,NotificationDelivery.acknowledge(cursor,10,true));
    }
    @Test public void restoredDatabaseResetsAnAheadCursorToFetchExistingUnread() {
        assertEquals(0,NotificationDelivery.baseline(1000,10));
        assertEquals(8,NotificationDelivery.baseline(8,10));
        assertEquals(0,NotificationDelivery.baseline(-1,10));
    }
    @Test public void readAndDisabledRecordsNeverAlertButCanBeAcknowledged() {
        assertFalse(NotificationDelivery.shouldAlert(true,true));
        assertFalse(NotificationDelivery.shouldAlert(false,false));
        assertTrue(NotificationDelivery.shouldAlert(false,true));
    }
    @Test public void bulkReadSuppressesOldSnapshotsWithoutConsumingNewerNotifications() {
        assertTrue(NotificationDelivery.confirmedRead(99,100));
        assertTrue(NotificationDelivery.confirmedRead(100,100));
        assertFalse(NotificationDelivery.confirmedRead(101,100));
        assertFalse(NotificationDelivery.confirmedRead(0,100));
        assertFalse(NotificationDelivery.shouldAlert(NotificationDelivery.confirmedRead(99,100),true));
        assertTrue(NotificationDelivery.shouldAlert(NotificationDelivery.confirmedRead(101,100),true));
    }
}
