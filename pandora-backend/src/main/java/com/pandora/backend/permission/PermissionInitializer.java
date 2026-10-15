package com.pandora.backend.permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Component
public class PermissionInitializer {

    private static final Logger log = LoggerFactory.getLogger(PermissionInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${pandora.permission.auto-init:true}")
    private boolean autoInit;

    @PostConstruct
    public void init() {
        if (!autoInit) {
            log.info("权限自动初始化已关闭，跳过");
            return;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM function_permissions WHERE deleted = 0", Integer.class);
            if (count != null && count > 0) {
                log.info("权限配置已存在({}条)，跳过初始化", count);
                return;
            }
            List<Map<String, Object>> adminFounders = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE role IN ('ADMIN','FOUNDER') AND deleted = 0");
            if (adminFounders.isEmpty()) {
                log.warn("未找到ADMIN/FOUNDER用户，跳过权限初始化");
                return;
            }
            Long configuredBy = ((Number) adminFounders.get(0).get("ID")).longValue();
            for (FunctionCode fc : FunctionCode.values()) {
                for (Map<String, Object> user : adminFounders) {
                    Long userId = ((Number) user.get("ID")).longValue();
                    jdbcTemplate.update(
                            "INSERT INTO function_permissions (function_code, subject_user_id, configured_by) VALUES (?, ?, ?)",
                            fc.getCode(), userId, configuredBy);
                }
            }
            log.info("权限默认集初始化完成: {}个功能客体, {}个用户", FunctionCode.values().length, adminFounders.size());
        } catch (Exception e) {
            log.error("权限初始化失败: {}", e.getMessage(), e);
        }
    }
}