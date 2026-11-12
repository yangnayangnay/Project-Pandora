package com.example.project_pandora.core.util;

/** WCAG text contrast, including translucent fills composed over their actual surface. */
public final class ReadableTextColors {
    private ReadableTextColors() {}
    public static int foreground(int fill, int surface, int preferred) {
        int background = composite(fill, surface);
        if (contrast(composite(preferred, background), background) >= 4.5) return preferred;
        return contrast(0xff000000, background) >= contrast(0xffffffff, background) ? 0xff000000 : 0xffffffff;
    }
    public static double contrast(int first, int second) {
        double a = luminance(first), b = luminance(second);
        return (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
    }
    private static double luminance(int color) {
        return .2126 * linear((color >> 16) & 255) + .7152 * linear((color >> 8) & 255) + .0722 * linear(color & 255);
    }
    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
    }
    private static int composite(int color, int background) {
        int alpha = color >>> 24, inverse = 255 - alpha;
        int red = (((color >> 16) & 255) * alpha + ((background >> 16) & 255) * inverse + 127) / 255;
        int green = (((color >> 8) & 255) * alpha + ((background >> 8) & 255) * inverse + 127) / 255;
        int blue = ((color & 255) * alpha + (background & 255) * inverse + 127) / 255;
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }
}
