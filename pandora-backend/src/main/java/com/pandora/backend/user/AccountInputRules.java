package com.pandora.backend.user;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

/** Shared account creation constraints; identity dates use the company's China time zone. */
public final class AccountInputRules {
    private AccountInputRules() {}
    private static final int[] WEIGHTS = {7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2};
    private static final Set<String> REGIONS;
    static {
        try (var input = AccountInputRules.class.getResourceAsStream("/identity/region-codes.txt")) {
            if (input == null) throw new IllegalStateException("Missing identity region codes");
            REGIONS = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .lines().filter(line -> line.matches("[0-9]{6}")).collect(Collectors.toUnmodifiableSet());
        } catch (java.io.IOException error) { throw new ExceptionInInitializerError(error); }
    }

    public static boolean realName(String value) {
        return value != null && value.matches("\\p{IsHan}{1,15}");
    }

    public static boolean password(String value) {
        if (value == null) return false;
        int length = value.codePointCount(0, value.length());
        return length >= 6 && length <= 16;
    }

    public static boolean idCard(String value) {
        return idCard(value, LocalDate.now(ZoneId.of("Asia/Shanghai")));
    }

    static boolean idCard(String value, LocalDate today) {
        if (value == null || !value.matches("[0-9]{17}[0-9Xx]")
                || !REGIONS.contains(value.substring(0, 6)) || "000".equals(value.substring(14, 17))) return false;
        try {
            LocalDate birth = LocalDate.parse(value.substring(6, 14), DateTimeFormatter.BASIC_ISO_DATE);
            if (birth.isAfter(today) || !birth.plusYears(60).isAfter(today)) return false;
        } catch (java.time.DateTimeException error) { return false; }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) sum += (value.charAt(i) - '0') * WEIGHTS[i];
        return Character.toUpperCase(value.charAt(17)) == "10X98765432".charAt(sum % 11);
    }
}
