package com.example.project_pandora.core.util;

/** Shared signature semantics; trailing blank lines count toward the four-line limit. */
public final class SignatureRules {
    public static final int MAX_LINES = 4;
    public static final int MAX_LENGTH = 20;
    private SignatureRules() {}
    public static String normalize(CharSequence value) {
        return value == null ? "" : value.toString().replace("\r\n", "\n").replace('\r', '\n')
                .replace('\u0085', '\n').replace('\u2028', '\n').replace('\u2029', '\n');
    }
    public static int explicitLines(CharSequence value) {
        String text = normalize(value); int lines = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') lines++;
        return lines;
    }
}
