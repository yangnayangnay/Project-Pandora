package com.pandora.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类——负责生成、解析和验证 JSON Web Token。
 * 本类是系统认证授权层的核心基础设施，被 AuthController 和各类鉴权拦截器调用。
 * 所有需要身份校验的接口均依赖此类完成令牌签发与校验。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * 获取 HMAC-SHA 签名密钥。
     * 将配置中的字符串密钥转换为 SecretKey 对象，供 JWT 签名和验签使用。
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌（Access Token）。
     * 令牌携带用户身份、角色及组织信息，用于接口鉴权和权限判断。
     *
     * @param userId 用户ID，主键
     * @param username 用户名，作为 JWT 的 subject
     * @param role 用户角色，用于权限校验
     * @param departmentId 部门ID，可为 null（如非业务角色）
     * @param teamId 团队ID，可为 null
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, String username, String role, Long departmentId, Long teamId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("departmentId", departmentId);
        claims.put("teamId", teamId);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌（Refresh Token）。
     * 刷新令牌仅包含用户ID，用于获取新的访问令牌，有效期通常更长。
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return 签名后的刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析并验证 JWT 令牌。
     * 若令牌签名无效或已过期，将抛出异常。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims 对象，包含所有声明信息
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证令牌是否有效。
     * 捕获所有异常并返回布尔值，避免调用方处理异常。
     *
     * @param token JWT 字符串
     * @return true 表示令牌有效且未过期；false 表示无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从令牌中提取用户ID。
     * 前置条件：令牌必须已验证有效。
     *
     * @param token JWT 字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从令牌中提取用户角色。
     * 用于权限校验，判断用户是否有权访问特定接口。
     *
     * @param token JWT 字符串
     * @return 角色字符串（如 ADMIN、EMPLOYEE）
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从令牌中提取用户名。
     * 用户名存储在 JWT 的 subject 字段中。
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
}