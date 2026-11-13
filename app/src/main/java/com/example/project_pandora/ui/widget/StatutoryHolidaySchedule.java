package com.example.project_pandora.ui.widget;

import com.nlf.calendar.Holiday;
import com.nlf.calendar.util.HolidayUtil;
import java.util.Calendar;

/** Published mainland-China leave and makeup-work dates; never infer leave from weekends. */
public final class StatutoryHolidaySchedule {
    private StatutoryHolidaySchedule() {}

    public static String marker(Calendar date) {
        int year = date.get(Calendar.YEAR), md = (date.get(Calendar.MONTH) + 1) * 100 + date.get(Calendar.DAY_OF_MONTH);
        // State Council General Office, 2025-11-04:
        // https://www.gov.cn/zhengce/content/202511/content_7047090.htm
        if (year == 2026) {
            if (md == 104 || md == 214 || md == 228 || md == 509 || md == 920 || md == 1010) return "班";
            if (between(md, 101, 103) || between(md, 215, 223) || between(md, 404, 406)
                    || between(md, 501, 505) || between(md, 619, 621) || between(md, 925, 927)
                    || between(md, 1001, 1007)) return "休";
            return "";
        }
        // State Council General Office, 2026-11-05, 国办发明电〔2026〕10号:
        // https://www.gov.cn/zhengce/content/202611/content_7085101.htm
        if (year == 2027) {
            if (md == 131 || md == 214 || md == 508 || md == 926 || md == 1009) return "班";
            if (between(md, 101, 103) || between(md, 205, 213) || between(md, 403, 405)
                    || between(md, 501, 505) || md == 609 || md == 915
                    || between(md, 1001, 1007)) return "休";
            return "";
        }
        // The bundled library carries historical published schedules through 2025.
        // Unknown future years deliberately have no guessed leave/work markers.
        if (year > 2025) return "";
        Holiday holiday = HolidayUtil.getHoliday(year, date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
        return holiday == null ? "" : holiday.isWork() ? "班" : "休";
    }

    private static boolean between(int value, int first, int last) { return value >= first && value <= last; }
}
