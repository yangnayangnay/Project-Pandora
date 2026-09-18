package com.example.project_pandora.core.moon;

import java.util.Calendar;
import java.util.Date;

public class MoonPhaseCalculator {

    public enum MoonPhase {
        NEW_MOON("新月", "values-theme-moon_new"),
        WAXING_CRESCENT("蛾眉月", "values-theme-waxing_crescent"),
        FIRST_QUARTER("上弦月", "values-theme-first_quarter"),
        WAXING_GIBBOUS("盈凸月", "values-theme-waxing_gibbous"),
        FULL_MOON("满月", "values-theme-full_moon"),
        WANING_GIBBOUS("亏凸月", "values-theme-waning_gibbous"),
        LAST_QUARTER("下弦月", "values-theme-last_quarter"),
        WANING_CRESCENT("残月", "values-theme-waning_crescent");

        private final String displayName;
        private final String themeName;

        MoonPhase(String displayName, String themeName) {
            this.displayName = displayName;
            this.themeName = themeName;
        }

        public String getDisplayName() { return displayName; }
        public String getThemeName() { return themeName; }
    }

    private static final double SYNODIC_MONTH = 29.53058867;
    private static final long KNOWN_NEW_MOON_MILLIS;

    static {
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.JANUARY, 6, 18, 14, 0);
        cal.set(Calendar.MILLISECOND, 0);
        KNOWN_NEW_MOON_MILLIS = cal.getTimeInMillis();
    }

    public MoonPhase calculateMoonPhase(Date date) {
        double diffDays = (date.getTime() - KNOWN_NEW_MOON_MILLIS) / (1000.0 * 60 * 60 * 24);
        double moonAge = diffDays % SYNODIC_MONTH;
        if (moonAge < 0) moonAge += SYNODIC_MONTH;

        return mapAgeToPhase(moonAge);
    }

    public MoonPhase calculateCurrentMoonPhase() {
        return calculateMoonPhase(new Date());
    }

    public double getMoonAge(Date date) {
        double diffDays = (date.getTime() - KNOWN_NEW_MOON_MILLIS) / (1000.0 * 60 * 60 * 24);
        double moonAge = diffDays % SYNODIC_MONTH;
        if (moonAge < 0) moonAge += SYNODIC_MONTH;
        return moonAge;
    }

    private MoonPhase mapAgeToPhase(double moonAge) {
        double phaseFraction = moonAge / SYNODIC_MONTH;
        double phaseAngle = phaseFraction * 8;

        if (phaseAngle < 0.5 || phaseAngle >= 7.5) return MoonPhase.NEW_MOON;
        else if (phaseAngle < 1.5) return MoonPhase.WAXING_CRESCENT;
        else if (phaseAngle < 2.5) return MoonPhase.FIRST_QUARTER;
        else if (phaseAngle < 3.5) return MoonPhase.WAXING_GIBBOUS;
        else if (phaseAngle < 4.5) return MoonPhase.FULL_MOON;
        else if (phaseAngle < 5.5) return MoonPhase.WANING_GIBBOUS;
        else if (phaseAngle < 6.5) return MoonPhase.LAST_QUARTER;
        else return MoonPhase.WANING_CRESCENT;
    }
}