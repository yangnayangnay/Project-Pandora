package com.pandora.backend.auth;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    public void put(Long userId, String token) {
        sessions.put("session:" + userId, token);
    }

    public String get(Long userId) {
        return sessions.get("session:" + userId);
    }

    public void remove(Long userId) {
        sessions.remove("session:" + userId);
    }
}