-- Additive upgrade; preserves notification history and works with SQL initialization disabled.
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS log_id BIGINT DEFAULT NULL;
