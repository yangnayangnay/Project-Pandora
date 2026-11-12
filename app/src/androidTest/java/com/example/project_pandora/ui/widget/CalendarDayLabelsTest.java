package com.example.project_pandora.ui.widget;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

/** Requires Android ICU; source compilation is distinct from executing this on a device. */
@RunWith(AndroidJUnit4.class)
public class CalendarDayLabelsTest {
    private CalendarDayLabels labels(int year, int month, int day) {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        date.clear(); date.set(year, month - 1, day, 12, 0);
        return CalendarDayLabels.forDate(date);
    }
    @Test public void lunarNewYearAndShortEveAreDerivedFromTheActualLunarCalendar() {
        assertEquals("除夕", labels(2026, 2, 16).special);
        assertEquals("廿九", labels(2026, 2, 16).lunar);
        assertEquals("春节", labels(2026, 2, 17).special);
        assertEquals("正月", labels(2026, 2, 17).lunar);
        assertEquals("中秋节", labels(2026, 9, 25).special);
    }
    @Test public void decemberReferenceIncludesTermsAndMemorialDates() {
        assertEquals("大雪", labels(2026, 12, 7).special);
        assertEquals("国家公祭日", labels(2026, 12, 13).special);
        assertEquals("冬至", labels(2026, 12, 22).special);
    }
}
