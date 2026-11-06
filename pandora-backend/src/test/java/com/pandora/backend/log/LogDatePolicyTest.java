package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LogDatePolicyTest {
    private void window(String instant, String first, String yesterday, String today, String rejectedBefore, String tomorrow) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        for (String date : new String[]{first, yesterday, today}) assertEquals(date, LogDatePolicy.requireWritable(date, clock));
        BusinessException error = assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(rejectedBefore, clock));
        assertEquals("仅可填写或编辑前天、昨天、今天的日志", error.getMessage());
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(tomorrow, clock));
    }
    @Test void acceptsPreviousTwoDaysThroughToday() {
        window("2026-10-27T16:01:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    }
    @Test void beforeShanghaiMidnightUsesPreviousBusinessDay() {
        window("2026-10-27T15:59:59Z", "2026-10-25", "2026-10-26", "2026-10-27", "2026-10-24", "2026-10-28");
    }
    @Test void switchesAtShanghaiMidnight() {
        window("2026-10-27T16:00:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    }
    @Test void handlesMonthBoundary() {
        window("2026-10-31T16:01:00Z", "2026-10-30", "2026-10-31", "2026-11-01", "2026-10-29", "2026-11-02");
    }
    @Test void handlesYearBoundary() {
        window("2026-12-31T16:01:00Z", "2026-12-30", "2026-12-31", "2027-01-01", "2026-12-29", "2027-01-02");
    }
    @Test void rejectsMalformedDates() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T16:01:00Z"), ZoneOffset.UTC);
        for (String date : new String[]{null, "", "2026/10/28", "2026-1-1", "20261028", "2026-02-30"})
            assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(date, clock));
    }
}
