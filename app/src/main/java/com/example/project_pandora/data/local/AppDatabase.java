package com.example.project_pandora.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * R44 REQ-D: version 1→2 正式 Migration（新增 source_server_id 列）。
 * **移除 fallbackToDestructiveMigration**（R1 风险防线：宁可抛异常不清库——
 * 离线队列数据是"最后断连者为准"语义的数据持有本体，清库=丢用户数据）。
 */
@Database(entities = {PendingData.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE pending_data ADD COLUMN source_server_id BIGINT DEFAULT NULL");
            android.util.Log.i(TAG, "Room v1->v2 migrated (source_server_id added)");
        }
    };

    private static volatile AppDatabase instance;

    public abstract PendingDataDao pendingDataDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "pandora_offline.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}