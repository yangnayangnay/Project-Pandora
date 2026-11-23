package com.pandora.backend.daily;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Additive compatibility upgrade, including installations with spring.sql.init.mode=never. */
@Component
@Order(-100)
public class DailyItemSchemaUpgrade implements ApplicationRunner {
    private final JdbcTemplate db;
    public DailyItemSchemaUpgrade(JdbcTemplate db) { this.db = db; }

    @Override public void run(ApplicationArguments args) {
        Set<String> columns = db.execute((ConnectionCallback<Set<String>>) connection -> {
            var found = new HashSet<String>();
            var metadata = connection.getMetaData();
            String table = metadata.storesUpperCaseIdentifiers() ? "DAILY_ITEMS" : "daily_items";
            try (var rows = metadata.getColumns(connection.getCatalog(), null, table, null)) {
                while (rows.next()) found.add(rows.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
            return found;
        });
        if (columns == null || columns.isEmpty()) throw new IllegalStateException("daily_items table is missing");
        add(columns, "start_date", "DATE");
        add(columns, "end_date", "DATE");
        add(columns, "start_clock", "VARCHAR(5)");
        add(columns, "end_clock", "VARCHAR(5)");
        db.update("UPDATE daily_items SET start_date=item_date WHERE start_date IS NULL");
        db.update("UPDATE daily_items SET end_date=item_date WHERE end_date IS NULL");
    }

    private void add(Set<String> columns, String column, String type) {
        if (!columns.contains(column)) db.execute("ALTER TABLE daily_items ADD " + column + " " + type + " DEFAULT NULL");
    }
}
