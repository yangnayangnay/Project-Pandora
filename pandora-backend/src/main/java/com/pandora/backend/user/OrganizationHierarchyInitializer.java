package com.pandora.backend.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Repair legacy reporting lines once at startup, after database schema initialization. */
@Component
public class OrganizationHierarchyInitializer implements ApplicationRunner {
    private final JdbcTemplate db;
    public OrganizationHierarchyInitializer(JdbcTemplate db) { this.db = db; }
    @Override @Transactional public void run(ApplicationArguments args) {
        new OrganizationHierarchy(db).synchronize(java.util.List.of(), -1);
    }
}
