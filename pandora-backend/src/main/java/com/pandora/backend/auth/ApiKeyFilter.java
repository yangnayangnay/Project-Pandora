package com.pandora.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * R41 REQ-41-A: 固定密钥头校验（三态灰度）。
 * 用于在 JWT 认证之前增加一层 API 密钥校验，实现灰度发布和流量控制。
 * mode=off     全放行（现状兼容，默认）
 * mode=log     放行 + WARN 记录未带/错密钥请求（灰度观察）
 * mode=enforce 非豁免路径未带/错密钥 → 401（A6 红线：App 升级实测前禁止云端开启）
 * <p>
 * 豁免路径（enforce 态仍放行）：登录与刷新（匿名必需）、health（App 测试连接探测）。
 * nginx 反代统一注入密钥头（A10），Web 端零改动。
 */
@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String HEADER = "X-Api-Key";

    // 豁免路径前缀：enforce 模式下仍允许访问
    private static final String[] EXEMPT_PREFIXES = {
            "/api/auth/login", "/api/auth/refresh", "/api/auth/forgot-password",
            "/api/actuator/health"
    };

    @Value("${pandora.api-key.mode:off}")
    private String mode;

    @Value("${pandora.api-key.value:}")
    private String apiKey;

    /**
     * 执行 API 密钥校验逻辑。
     * 三态模式：off（关闭）、log（仅记录）、enforce（强制校验）。
     * 学习备注: 豁免路径和公开资源（头像）直接放行。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // 公开资源（头像）直接放行
        if (AvatarResourceAccess.isPublicRead(request)) {
            chain.doFilter(request, response);
            return;
        }

        // enforce 模式：强制校验密钥
        if ("enforce".equalsIgnoreCase(mode) && !isExempt(path)) {
            String provided = request.getHeader(HEADER);
            if (apiKey == null || apiKey.isEmpty() || provided == null || !apiKey.equals(provided)) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"缺少或错误的 API 密钥\"}");
                log.warn("ApiKeyFilter enforce 拒绝: {} {} (无密钥头: {})", request.getMethod(), path, provided == null);
                return;
            }
        } else if ("log".equalsIgnoreCase(mode) && !isExempt(path)) {
            // log 模式：仅记录警告，不拦截
            String provided = request.getHeader(HEADER);
            if (provided == null || apiKey == null || apiKey.isEmpty() || !apiKey.equals(provided)) {
                log.warn("ApiKeyFilter log 观察: {} {} 未携带有效密钥头（放行）", request.getMethod(), path);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 判断路径是否为豁免路径（enforce 模式下仍放行）。
     *
     * @param path 请求路径
     * @return true 表示豁免，false 表示需要校验
     */
    private boolean isExempt(String path) {
        for (String prefix : EXEMPT_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}