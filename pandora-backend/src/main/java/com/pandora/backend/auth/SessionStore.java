package com.pandora.backend.auth;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话存储——基于内存的令牌会话管理。
 * 用于实现单点登录控制：一个用户同一时间只能有一个有效令牌。
 * 被登录/登出/刷新令牌流程调用，存储结构为 "session:{userId}" -> "{token}"。
 * 学习备注: 当前使用内存存储，重启服务后会话丢失，生产环境建议替换为 Redis。
 */
@Component
public class SessionStore {

    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    /**
     * 存储用户会话令牌。
     * 同一用户重复登录会覆盖旧令牌，实现单点登录效果。
     *
     * @param userId 用户ID
     * @param token  JWT 令牌
     */
    public void put(Long userId, String token) {
        sessions.put("session:" + userId, token);
    }

    /**
     * 获取用户当前有效令牌。
     *
     * @param userId 用户ID
     * @return 令牌字符串，若无有效会话返回 null
     */
    public String get(Long userId) {
        return sessions.get("session:" + userId);
    }

    /**
     * 移除用户会话（登出时调用）。
     *
     * @param userId 用户ID
     */
    public void remove(Long userId) {
        sessions.remove("session:" + userId);
    }
}