package com.pandora.backend.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类——定义认证授权策略和安全过滤链。
 * 采用无状态会话（JWT），禁用 CSRF（API 场景），配置接口访问规则。
 * 被 Spring 容器启动时加载，是系统安全架构的核心配置入口。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 密码编码器——BCrypt 算法，用于密码哈希和校验。
     * BCrypt 是 Spring Security 推荐的密码存储方案，自带盐值和强度可调。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链配置。
     * - 禁用 CSRF（API 无需跨站保护）
     * - 无状态会话（JWT，不创建 HttpSession）
     * - 公开接口白名单配置
     * - 自定义过滤器顺序：ApiKeyFilter -> RoleAuthFilter -> UsernamePasswordAuthenticationFilter
     * - 允许 H2-Console 框架嵌入（开发环境）
     *
     * @param http HttpSecurity 配置对象
     * @param roleAuthFilter 角色认证过滤器
     * @param apiKeyFilter API 密钥过滤器
     * @return SecurityFilterChain 安全过滤链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RoleAuthFilter roleAuthFilter,
                                           ApiKeyFilter apiKeyFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // The original REQUEST is authenticated before entering a DeferredResult wait.
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/forgot-password",
                                "/actuator/**", "/h2-console/**", "/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(roleAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // R41: ApiKeyFilter 置于 RoleAuthFilter 之前（密钥层先于 JWT 角色层）
                .addFilterBefore(apiKeyFilter, RoleAuthFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }
}