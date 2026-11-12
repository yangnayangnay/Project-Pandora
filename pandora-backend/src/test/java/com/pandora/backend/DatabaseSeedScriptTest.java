package com.pandora.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSeedScriptTest {
    @Test
    void schemaAndSeedDataLoadTogetherWithUpgradedTaskModel() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:seed-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/h2_schema.sql"),
                new ClassPathResource("db/h2_data.sql")).execute(source);

        var db = new JdbcTemplate(source);
        assertTrue(db.queryForObject("SELECT COUNT(*) FROM tasks", Integer.class) > 0);
        assertEquals(0, db.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE status NOT IN ('IN_PROGRESS','COMPLETED')", Integer.class));
        assertEquals(0, db.queryForObject(
                "SELECT COUNT(*) FROM tasks t WHERE t.assignee_id IS NOT NULL " +
                        "AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id=t.id AND ta.user_id=t.assignee_id)",
                Integer.class));
    }

    @Test
    void schemaUpgradesAnExistingH2FileModelWithoutDeletingLocalData() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:legacy-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var db = new JdbcTemplate(source);
        db.execute("CREATE TABLE users(id BIGINT AUTO_INCREMENT PRIMARY KEY,username VARCHAR(64) NOT NULL," +
                "password_hash VARCHAR(128) NOT NULL,role VARCHAR(20) NOT NULL,department_id BIGINT,team_id BIGINT," +
                "avatar VARCHAR(256),theme_mode VARCHAR(10) DEFAULT 'AUTO',manual_theme VARCHAR(32)," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted TINYINT DEFAULT 0)");
        db.execute("CREATE TABLE tasks(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(256) NOT NULL," +
                "priority VARCHAR(10) DEFAULT 'MEDIUM',status VARCHAR(20) DEFAULT 'PENDING',start_time TIMESTAMP,end_time TIMESTAMP," +
                "progress_note CLOB,assignee_id BIGINT NOT NULL,dispatcher_id BIGINT,is_important TINYINT DEFAULT 0," +
                "is_urgent TINYINT DEFAULT 0,version INT DEFAULT 0,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted TINYINT DEFAULT 0)");
        db.execute("CREATE INDEX idx_task_quadrant ON tasks(is_important,is_urgent)");
        db.execute("CREATE TABLE daily_items(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL," +
                "content VARCHAR(500) NOT NULL,item_date DATE NOT NULL,completed TINYINT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted TINYINT DEFAULT 0)");
        db.update("INSERT INTO users(id,username,password_hash,role) VALUES(2,'legacy-user','hash','EMPLOYEE')");
        db.update("INSERT INTO tasks(id,name,status,assignee_id) VALUES(8,'旧任务','PENDING',2)");
        db.update("INSERT INTO daily_items(id,user_id,content,item_date) VALUES(9,2,'旧事项','2026-11-06')");

        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);

        assertDoesNotThrow(() -> db.queryForObject("SELECT signature FROM users WHERE id=2", String.class));
        assertEquals("IN_PROGRESS", db.queryForObject("SELECT status FROM tasks WHERE id=8", String.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM task_assignees WHERE task_id=8 AND user_id=2", Integer.class));
        assertEquals(java.sql.Date.valueOf("2026-11-06"), db.queryForObject("SELECT start_date FROM daily_items WHERE id=9", java.sql.Date.class));
        assertEquals(java.sql.Date.valueOf("2026-11-06"), db.queryForObject("SELECT end_date FROM daily_items WHERE id=9", java.sql.Date.class));
    }

    @Test
    void productionMigrationPreservesAccountsAndCanRunTwice() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:prod-migration-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var db = new JdbcTemplate(source);
        db.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,username VARCHAR(64) NOT NULL,password_hash VARCHAR(128) NOT NULL,role VARCHAR(20) NOT NULL)");
        db.execute("CREATE TABLE tasks(id BIGINT PRIMARY KEY,status VARCHAR(20),assignee_id BIGINT,priority VARCHAR(10),is_important TINYINT)");
        db.execute("CREATE INDEX idx_task_quadrant ON tasks(is_important)");
        db.execute("CREATE TABLE daily_items(id BIGINT PRIMARY KEY,user_id BIGINT,content VARCHAR(500),item_date DATE,deleted TINYINT DEFAULT 0)");
        db.update("INSERT INTO users VALUES(2,'000001','original-hash','FOUNDER')");
        db.update("INSERT INTO tasks VALUES(8,'PENDING',2,'HIGH',1)");
        db.update("INSERT INTO daily_items(id,user_id,content,item_date) VALUES(9,2,'原事项','2026-11-06')");
        var migration = new ClassPathResource("db/migrations/20261106_task_daily_directory_h2.sql");

        new ResourceDatabasePopulator(migration).execute(source);
        new ResourceDatabasePopulator(migration).execute(source);

        assertEquals("000001", db.queryForObject("SELECT username FROM users WHERE id=2", String.class));
        assertEquals("original-hash", db.queryForObject("SELECT password_hash FROM users WHERE id=2", String.class));
        assertEquals("FOUNDER", db.queryForObject("SELECT role FROM users WHERE id=2", String.class));
        assertEquals("IN_PROGRESS", db.queryForObject("SELECT status FROM tasks WHERE id=8", String.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM task_assignees WHERE task_id=8 AND user_id=2", Integer.class));
        assertEquals(java.sql.Date.valueOf("2026-11-06"), db.queryForObject("SELECT start_date FROM daily_items WHERE id=9", java.sql.Date.class));
    }
}
