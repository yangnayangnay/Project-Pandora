package com.pandora.backend.notification;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Preserve notification history when production SQL initialization is disabled. */
@Component
@Order(-90)
public class NotificationSchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate db;
    public NotificationSchemaUpgrade(JdbcTemplate db) { this.db = db; }
    @Override public void run(ApplicationArguments args) {
        Boolean present = db.execute((ConnectionCallback<Boolean>) connection -> {
            var metadata = connection.getMetaData();
            String table = metadata.storesUpperCaseIdentifiers() ? "NOTIFICATIONS" : "notifications";
            try (var columns = metadata.getColumns(connection.getCatalog(), null, table, null)) {
                while (columns.next()) if ("log_id".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
            return false;
        });
        if (!Boolean.TRUE.equals(present)) db.execute("ALTER TABLE notifications ADD log_id BIGINT DEFAULT NULL");
    }
}
