package com.pandora.backend.theme;

import com.pandora.backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/theme")
public class ThemeController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ConcurrentHashMap<String, long[]> moonCacheExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> moonCacheValue = new ConcurrentHashMap<>();

    @GetMapping("/moon-phase")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMoonPhase() {
        long[] expiry = moonCacheExpiry.get("moon_phase:today");
        String cached = moonCacheValue.get("moon_phase:today");
        if (cached != null && expiry != null && System.currentTimeMillis() < expiry[0]) {
            Map<String, Object> result = new HashMap<>();
            result.put("phase", cached);
            result.put("themeName", "values-theme-" + cached.toLowerCase());
            return ResponseEntity.ok(ApiResponse.success(result));
        }

        Calendar cal = Calendar.getInstance();
        double phase = calculateMoonPhase(cal);
        String phaseName = mapToPhaseName(phase);

        moonCacheValue.put("moon_phase:today", phaseName);
        moonCacheExpiry.put("moon_phase:today", new long[]{System.currentTimeMillis() + 25 * 60 * 60 * 1000L});

        Map<String, Object> result = new HashMap<>();
        result.put("phase", phaseName);
        result.put("themeName", "values-theme-" + phaseName.toLowerCase());
        result.put("moonAge", phase);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/preference")
    public ResponseEntity<ApiResponse<Void>> setThemePreference(@RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        String mode = request.get("mode");
        String manualTheme = request.get("manualTheme");

        if ("AUTO".equals(mode) || "MANUAL".equals(mode)) {
            jdbcTemplate.update("UPDATE users SET theme_mode = ? WHERE id = ?", mode, userId);
            if (manualTheme != null) {
                jdbcTemplate.update("UPDATE users SET manual_theme = ? WHERE id = ?", manualTheme, userId);
            }
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    private double calculateMoonPhase(Calendar cal) {
        Calendar knownNewMoon = Calendar.getInstance();
        knownNewMoon.set(2000, Calendar.JANUARY, 6, 18, 14, 0);
        knownNewMoon.set(Calendar.MILLISECOND, 0);

        double synodicMonth = 29.53058867;
        long diffMillis = cal.getTimeInMillis() - knownNewMoon.getTimeInMillis();
        double diffDays = diffMillis / (1000.0 * 60 * 60 * 24);
        double phase = (diffDays % synodicMonth) / synodicMonth;
        if (phase < 0) phase += 1;
        return phase * synodicMonth;
    }

    private String mapToPhaseName(double moonAge) {
        if (moonAge < 1.84566) return "NEW_MOON";
        if (moonAge < 5.53699) return "WAXING_CRESCENT";
        if (moonAge < 9.22831) return "FIRST_QUARTER";
        if (moonAge < 12.91963) return "WAXING_GIBBOUS";
        if (moonAge < 16.61096) return "FULL_MOON";
        if (moonAge < 20.30228) return "WANING_GIBBOUS";
        if (moonAge < 23.99361) return "LAST_QUARTER";
        return "WANING_CRESCENT";
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}