package com.pandora.backend.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 头像资源访问控制工具类。
 * 判断请求是否为公开读取的头像资源，用于过滤器的白名单放行。
 * 与 SecurityConfig 中配置的公开资源路径保持一致。
 */
final class AvatarResourceAccess {
    private AvatarResourceAccess() {}

    /**
     * 判断请求是否为公开读取的头像资源。
     * 仅 GET/HEAD 方法且路径以 /uploads/avatars/ 开头的请求视为公开。
     *
     * @param request HTTP 请求
     * @return true 表示公开头像资源，false 表示需要认证
     */
    static boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) return false;
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }
        return path.startsWith("/uploads/avatars/");
    }
}