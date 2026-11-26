package com.pandora.backend.auth;

import com.pandora.backend.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 角色认证过滤器——基于 JWT 的接口级权限拦截器。
 * 该过滤器在每个请求到达 Controller 前执行，验证令牌有效性并设置安全上下文。
 * 被 Spring Security 框架集成，是系统认证授权层的核心组件。
 */
@Component
public class RoleAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 执行过滤逻辑：验证 JWT 令牌、查询用户角色、校验会话有效性、设置安全上下文。
     * 对公开接口（登录、注册、刷新令牌等）直接放行。
     *
     * @param request  HTTP 请求
     * @param response  HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        // 公开接口白名单：无需认证即可访问
        if (AvatarResourceAccess.isPublicRead(request) || path.contains("/auth/login") || path.contains("/auth/register")
                || path.contains("/auth/refresh") || path.contains("/auth/forgot-password")
                || path.contains("/actuator")
                || path.contains("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 拦截缺少 Authorization 头的请求
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(ErrorCode.UNAUTHORIZED.getCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未认证\"}");
            return;
        }

        String token = authHeader.substring(7);
        // 拦截无效或过期的令牌
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(ErrorCode.UNAUTHORIZED.getCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效\"}");
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        // 查询用户当前角色，同时校验账号状态（已删除、未审批）
        java.util.List<String> roles = jdbcTemplate.queryForList(
                "SELECT role FROM users WHERE id = ? AND deleted = 0 AND approval_status = 'APPROVED'", String.class, userId);
        if (roles.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"账号已停用或未批准\"}");
            return;
        }
        String role = roles.get(0);
        // 将角色写入响应头，供下游服务或日志使用
        response.setHeader("X-User-Role", role);

        // 会话校验：防止单点登录被踢下线后仍使用旧令牌
        String sessionToken = sessionStore.get(userId);
        if (sessionToken == null || !sessionToken.equals(token)) {
            response.setStatus(ErrorCode.UNAUTHORIZED.getCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"会话已过期\"}");
            return;
        }

        // 设置 Spring Security 安全上下文，供 @PreAuthorize 等注解使用
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}