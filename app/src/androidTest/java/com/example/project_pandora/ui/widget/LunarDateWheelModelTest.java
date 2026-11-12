package com.example.project_pandora.ui.widget;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Calendar;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LunarDateWheelModelTest {
    private Calendar date(int year, int month, int day) { Calendar result = Calendar.getInstance(); result.clear(); result.set(year, month - 1, day, 12, 0); return result; }
    @Test public void newYearAndLeapMonthHaveRealCivilDates() {
        LunarDateWheelModel spring = LunarDateWheelModel.from(date(2024, 2, 10));
        assertEquals(2024, spring.year); assertEquals(1, spring.month); assertEquals(1, spring.day); assertFalse(spring.leap);
        List<LunarDateWheelModel.Month> months = LunarDateWheelModel.months(2023);
        assertEquals(13, months.size());
        LunarDateWheelModel.Month leap = months.stream().filter(month -> month.leap).findFirst().get();
        assertEquals(2, leap.number); assertEquals("闰二月", leap.label());
        Calendar civil = leap.date(1, date(2023, 1, 1)); assertEquals(3, civil.get(Calendar.MONTH) + 1); assertEquals(22, civil.get(Calendar.DAY_OF_MONTH));
    }
    @Test public void switchingWheelsPreservesRealDateIncludingLeapMonthAndMonthEnd() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Calendar selected : new Calendar[]{date(2023, 3, 22), date(2024, 2, 29), date(2025, 8, 1), date(2026, 1, 31), date(2026, 11, 10), date(1900, 1, 1)}) {
                YearMonthDayPicker picker = new YearMonthDayPicker(ApplicationProvider.getApplicationContext(), selected, 1900);
                for (int repeat = 0; repeat < 5; repeat++) {
                    picker.setShowLunar(true); assertSameDay(selected, picker.getSelectedDate());
                    picker.setShowLunar(false); assertSameDay(selected, picker.getSelectedDate());
                }
            }
        });
    }
    private void assertSameDay(Calendar a, Calendar b) { assertEquals(a.get(Calendar.YEAR), b.get(Calendar.YEAR)); assertEquals(a.get(Calendar.DAY_OF_YEAR), b.get(Calendar.DAY_OF_YEAR)); }
}
