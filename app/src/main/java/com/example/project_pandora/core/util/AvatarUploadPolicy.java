package com.example.project_pandora.core.util;

import okhttp3.HttpUrl;

/** Prepared upload byte budget and URL handling; the original selected file has no size cap. */
public final class AvatarUploadPolicy {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_DIMENSION = 1600;
    private AvatarUploadPolicy() {}
    public static boolean accepts(long size) { return size > 0 && size <= MAX_BYTES; }
    public static int sampleSize(int width, int height) {
        int sample = 1;
        while (Math.max(width, height) / sample > MAX_DIMENSION) sample *= 2;
        return sample;
    }
    public static String resolveUrl(String baseUrl, String avatar) {
        if (avatar == null || avatar.trim().isEmpty()) return null;
        String path = avatar.trim();
        HttpUrl absolute = HttpUrl.parse(path);
        if (absolute != null) return absolute.toString();
        HttpUrl base = HttpUrl.parse(baseUrl);
        if (base == null) return null;
        // Stored paths are relative to the API context, unless that context is already included.
        HttpUrl resolved = path.startsWith(base.encodedPath()) ? base.resolve(path)
                : base.resolve(path.replaceFirst("^/+", ""));
        return resolved == null ? null : resolved.toString();
    }

    /** Only send the session credential to this server's avatar resource directory. */
    public static boolean requiresAuthentication(String baseUrl, String avatarUrl) {
        HttpUrl base = HttpUrl.parse(baseUrl);
        HttpUrl avatar = avatarUrl == null ? null : HttpUrl.parse(avatarUrl);
        if (base == null || avatar == null || !base.scheme().equals(avatar.scheme())
                || !base.host().equals(avatar.host()) || base.port() != avatar.port()) return false;
        return avatar.encodedPath().startsWith(base.encodedPath() + "uploads/avatars/");
    }
}
