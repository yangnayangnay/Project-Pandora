-- MySQL: run once against an existing database after taking the normal database backup.
-- Existing all-day entries remain NULL. No account or task data is rewritten.
ALTER TABLE daily_items ADD COLUMN start_clock VARCHAR(5) DEFAULT NULL,
    ADD COLUMN end_clock VARCHAR(5) DEFAULT NULL;
