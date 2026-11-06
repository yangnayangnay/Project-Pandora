package com.pandora.backend.auth;

import jakarta.servlet.http.HttpServletRequest;

/** Align custom filters with the public avatar resources declared in SecurityConfig. */
final class AvatarResourceAccess {
    private AvatarResourceAccess() {}

    static boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) return false;
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }
        return path.startsWith("/uploads/avatars/");
    }
}
