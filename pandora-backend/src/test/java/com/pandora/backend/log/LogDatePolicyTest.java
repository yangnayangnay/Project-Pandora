package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LogDatePolicyTest {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T16:01:00Z"), SHANGHAI);

    @Test void acceptsYesterdayTodayTomorrow() {
        assertEquals("2026-10-27", LogDatePolicy.requireWritable("2026-10-27", CLOCK));
        assertEquals("2026-10-28", LogDatePolicy.requireWritable("2026-10-28", CLOCK));
        assertEquals("2026-10-29", LogDatePolicy.requireWritable("2026-10-29", CLOCK));
    }

    @Test void rejectsDayBeforeAndDayAfter() {
        BusinessException before = assertThrows(BusinessException.class,
                () -> LogDatePolicy.requireWritable("2026-10-26", CLOCK));
        assertEquals("仅可填写或编辑昨天、今天、明天的日志", before.getMessage());
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-10-30", CLOCK));
    }

    @Test void rejectsInvalidEmptyAndNullDates() {
        for (String date : new String[]{"2026-02-30", "2026/10/28", "2026-13-01", "20261028", "", null})
            assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable(date, CLOCK));
    }

    @Test void switchesAtShanghaiMidnight() {
        Clock beforeMidnight = Clock.fixed(Instant.parse("2026-10-27T15:59:59Z"), ZoneOffset.UTC);
        assertEquals("2026-10-26", LogDatePolicy.requireWritable("2026-10-26", beforeMidnight));
        assertEquals("2026-10-27", LogDatePolicy.requireWritable("2026-10-27", beforeMidnight));
        assertEquals("2026-10-28", LogDatePolicy.requireWritable("2026-10-28", beforeMidnight));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-10-25", beforeMidnight));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-10-29", beforeMidnight));

        Clock afterMidnight = Clock.fixed(Instant.parse("2026-10-27T16:00:00Z"), ZoneOffset.UTC);
        assertEquals("2026-10-27", LogDatePolicy.requireWritable("2026-10-27", afterMidnight));
        assertEquals("2026-10-28", LogDatePolicy.requireWritable("2026-10-28", afterMidnight));
        assertEquals("2026-10-29", LogDatePolicy.requireWritable("2026-10-29", afterMidnight));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-10-26", afterMidnight));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-10-30", afterMidnight));
    }

    @Test void handlesMonthEndAcrossMonths() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-31T15:30:00Z"), SHANGHAI);
        assertEquals("2026-10-30", LogDatePolicy.requireWritable("2026-10-30", clock));
        assertEquals("2026-10-31", LogDatePolicy.requireWritable("2026-10-31", clock));
        assertEquals("2026-11-01", LogDatePolicy.requireWritable("2026-11-01", clock));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-10-29", clock));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-11-02", clock));
    }

    @Test void handlesYearEndAcrossYears() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T15:30:00Z"), SHANGHAI);
        assertEquals("2026-12-30", LogDatePolicy.requireWritable("2026-12-30", clock));
        assertEquals("2026-12-31", LogDatePolicy.requireWritable("2026-12-31", clock));
        assertEquals("2027-01-01", LogDatePolicy.requireWritable("2027-01-01", clock));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2026-12-29", clock));
        assertThrows(BusinessException.class, () -> LogDatePolicy.requireWritable("2027-01-02", clock));
    }
}
