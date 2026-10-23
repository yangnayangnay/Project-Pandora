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

    private static final String[] EXEMPT_PREFIXES = {
            "/api/auth/login", "/api/auth/refresh", "/api/auth/forgot-password",
            "/api/actuator/health"
    };

    @Value("${pandora.api-key.mode:off}")
    private String mode;

    @Value("${pandora.api-key.value:}")
    private String apiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

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
            String provided = request.getHeader(HEADER);
            if (provided == null || apiKey == null || apiKey.isEmpty() || !apiKey.equals(provided)) {
                log.warn("ApiKeyFilter log 观察: {} {} 未携带有效密钥头（放行）", request.getMethod(), path);
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isExempt(String path) {
        for (String prefix : EXEMPT_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}